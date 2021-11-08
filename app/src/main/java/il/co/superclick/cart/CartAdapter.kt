package il.co.superclick.cart

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.text.bold
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.*
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.filterEmoticons
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.ui.showKeyboard
import com.dm6801.framework.utilities.Log
import com.dm6801.framework.utilities.delay
import com.dm6801.framework.utilities.main
import com.dm6801.framework.utilities.withMain
import il.co.superclick.R
import il.co.superclick.data.*
import il.co.superclick.data.Cart.save
import il.co.superclick.data.Cart.updateLiveData
import il.co.superclick.dialogs.ConfirmDialog
import il.co.superclick.dialogs.ProductAlertDialog
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.RecyclerAdapter
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.meal.EditMealFragment
import il.co.superclick.toppings.ToppingsFragment
import il.co.superclick.utilities.*
import il.co.superclick.widgets.AmountWidget
import il.co.superclick.widgets.PriceView
import il.co.superclick.widgets.UnitTypeWidget
import kotlinx.android.synthetic.main.item_cart_checkout.view.*
import kotlinx.android.synthetic.main.item_cart.view.*
import kotlinx.android.synthetic.main.item_cart.view.item_checkout_comment_button
import kotlinx.android.synthetic.main.item_cart_edit.view.*

class CartAdapter(
    products: List<Cart.Item>,
    val onToggle: ((productId: Int, isChecked: Boolean) -> Unit)? = null
) : RecyclerAdapter<CartAdapter.ViewHolder, CartAdapter.AdapterItem>(
    asyncDifferConfig = AsyncDifferConfig.Builder(
        object : DiffUtil.ItemCallback<AdapterItem>() {
            override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem) =
                oldItem.id == newItem.id

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem) =
                oldItem.cartItem == newItem.cartItem
        }
    ).build()
) {

    companion object {
        private val cart get() = Locator.database.cart
    }

    override val layout = R.layout.item_cart
    override val viewHolderClass = ViewHolder::class.java
    private var isEditing: Boolean = false

    override val asyncListDiffer = object : AsyncListDiffer<AdapterItem>(
        object : ListUpdateCallback {
            override fun onChanged(position: Int, count: Int, payload: Any?) {
                notifyItemRangeChanged(position, count, position until position + count)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition, toPosition)
            }

            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position, count)
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position, count)
                delay(600) { withMain { notifyDataSetChanged() } }
            }
        },
        asyncDifferConfig
    ) {}

    init {
        setHasStableIds(true)
        submitList(products)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        object : SwipeHelper(recyclerView) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton>?
            ) {
                underlayButtons?.add(UnderlayButton(
                    clickListener = object : UnderlayButtonClickListener {
                        override fun onClick(pos: Int) {
                            removeItem(this@CartAdapter.getItem(pos))
                        }
                    }
                ))
            }
        }
    }

    private fun removeItem(item: AdapterItem?) {
        item?.id?.let {
            if (it >= 0) {
                ConfirmDialog.deleteProduct(item.cartItem.productId)
                notifyDataSetChanged()
            } else {
                ConfirmDialog.deleteProduct(onConfirm = {
                    cart[item.cartItem.productId]?.meals?.removeAt((item.cartItem.productId + (item.id)).unaryMinus())
                    cart[item.cartItem.productId]?.comment = cart[item.cartItem.productId]?.comment?.split("_")?.toMutableList()?.also { com -> com.removeAt((item.cartItem.productId + (item.id)).unaryMinus()) }?.joinToString(separator = "_")
                    if(cart[item.cartItem.productId]?.meals?.size == 0)
                        cart.remove(item.cartItem.productId)
                    updateLiveData()
                    save()
                    recyclerView?.adapter?.notifyDataSetChanged()
                })
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun submitList(items: List<Cart.Item>, a: Boolean = false, callback: (() -> Unit)? = null) {
        val regularProducts = items.filter { it.meals == null }.map { AdapterItem(it) }
        val mealProducts = items.filter { it.meals != null }.map { item ->
            var counter = 0
            item.meals?.map {
                val copyItem = item.copy()
                copyItem.id = (item.productId + counter).unaryMinus()
                copyItem.meals = mutableListOf(it)
                counter += 1
                AdapterItem(copyItem)
            }?.toMutableList() ?: mutableListOf()
        }.flatten()
        submitList(regularProducts + mealProducts) {
            notifyDataSetChanged()
            callback?.invoke()
        }
    }

    data class AdapterItem(
        val cartItem: Cart.Item
    ) : Identity<Int> {
        override val id = cartItem.id ?: 0
        override fun compareTo(other: Int) = id.compareTo(other)
    }

    enum class ItemMode {
        Default, Edit
    }

    inner class ViewHolder(itemView: View) :
        RecyclerAdapter.ViewHolder<AdapterItem>(itemView) {

        val checkbox: CheckBox? get() = itemView.item_cart_checkbox
        private val checkboxBackground: View? get() = itemView.item_cart_checkbox_background
        private val image: ImageView? get() = itemView.item_cart_image
        private val name: TextView? get() = itemView.item_cart_name
        private val itemCartTag: TextView? get() = itemView.item_cart_tag_new
        private val amount: TextView? get() = itemView.item_cart_amount
        private val unitType: TextView? get() = itemView.item_cart_unit_type
        private val price: PriceView? get() = itemView.item_cart_price
        private val startEditButton: TextView? get() = itemView.item_cart_edit_button
        private val editAmount: AmountWidget? get() = itemView.item_cart_edit_amount
        private val editUnitType: UnitTypeWidget? get() = itemView.item_cart_unit_type_widget
        private val editDeleteButton: TextView? get() = itemView.item_cart_delete_button
        private val editCloseButton: ImageView? get() = itemView.item_cart_close_button
        private val commentPlaceholder: TextView? get() = itemView.item_checkout_comment_button
        private val info: TextView? get() = itemView.cart_item_product_info
        private var mode: ItemMode = ItemMode.Default
        private var item: AdapterItem? = null
        private val productId: Int get() = item?.cartItem?.productId ?: -1

        @Suppress("UNCHECKED_CAST")
        override fun bind(item: AdapterItem, position: Int, payloads: MutableList<Any>?) {
            super.bind(item, position, payloads)
            val range = payloads as? List<IntRange>
            if (range?.isNotEmpty() == true && range.none { it.contains(position) }) return
            setItemDetails(item)
            setTag()
            setMode(mode)
            checkbox?.buttonTintList = mainColor
            showComment(item = item.cartItem)
        }

        private fun setTag() {
            when {
                item?.cartItem?.product?.isNew == true -> {
                    itemCartTag?.isVisible = true
                    itemCartTag?.backgroundTintList = newColor
                    itemCartTag?.text = getString(R.string.new_item)
                }
                item?.cartItem?.product?.isSale == true -> {
                    itemCartTag?.isVisible = true
                    itemCartTag?.backgroundTintList = saleColor
                    itemCartTag?.text = getString(R.string.sale)
                }
                else -> {
                    itemCartTag?.isInvisible = true
                }
            }
        }

        private fun setItemDetails(item: AdapterItem) {
            this.item = item
            displayDetails(item.cartItem)
        }

        private fun displayDetails(item: Cart.Item) {
            itemView.tag = item.product?.id
            name?.text = item.product?.product?.name
            image?.glide(item.product?.product?.image)
            editDeleteButton?.backgroundTintList = mainColor
            setInfoButton()
            image?.onClick(1_000) {
                showMagnifiedImage(
                    image ?: return@onClick,
                    item.product?.product?.imageBig ?: return@onClick
                )
            }

            amount?.text = if (item.unitType?.type == UnitType.UNIT) {
                if (item.toppings?.isEmpty() == false)
                    item.toppings.size.toString()
                else
                    item.amount.toInt().toString()
            } else
                String.format("%.2f", item.amount)
            unitType?.text = item.unitTypeDisplay
            if (item.meals == null)
                price?.set(item.sum)
            else
                price?.set(item.mealProductSum)
            editAmount?.setProduct(item.product, item.unitType)
            main { editAmount?.setAmount(item.amount) }
            editUnitType?.set(item.product?.unitTypes, item.unitTypeName)
            editUnitType?.setCallback {
                if (editAmount?.unitTypeName != it.type) {
                    editAmount?.setProduct(item.product, it)
                    updateCart()
                }
                editAmount?.setProduct(item.product, it)
                editAmount?.setAmount(item.amount)
            }
        }

        private fun setInfoButton() {
            item?.cartItem?.product?.product?.description?.takeIf { it.isNotBlank() }?.let { description ->
                info?.onClick {
                    ProductAlertDialog.open(
                        item?.cartItem?.product ?: return@onClick,
                        description,
                        textAlignment = Gravity.START or Gravity.CENTER_VERTICAL,
                        closeButton = true,
                        autoClose = null
                    )
                }
                info?.isVisible = true
                info?.text = info?.text.toString().underlineText()
                info?.setThemeColor()
            } ?: run { info?.isVisible = false }
        }

        private fun showComment(item: OrderProduct) {
            commentPlaceholder?.setThemeColor()
            if (item.comment.isNullOrBlank() || item.comment == "null") {
                commentPlaceholder?.isVisible = true
                commentPlaceholder?.text = getString(R.string.add_comment).underline()
                commentPlaceholder?.onClick {
                    showCommentDialog(item.comment) {
                        if (item.product?.product?.productType == ProductType.PACK) {
                            val mealItemPosition = (item.productId - ((item as? Cart.Item)?.id?.unaryMinus() ?: 0)).unaryMinus()
                            val commentsList = cart[item.productId]?.meals?.map {""}?.toMutableList()
                            commentsList?.set(mealItemPosition, it.toString())
                            item.comment = commentsList?.joinToString(separator = "_")
                        }else
                            item.comment = it?.trim()
                        saveComment(item)
                    }
                }
            } else {
                commentPlaceholder?.isVisible = true
                if (item.product?.product?.productType == ProductType.PACK) {
                    val mealItemPosition = (item.productId - ((item as? Cart.Item)?.id?.unaryMinus() ?: 0)).unaryMinus()
                    val comments = item.comment.toString().split("_").toMutableList()
                    if (comments[mealItemPosition].isNullOrBlank())
                        commentPlaceholder?.text = getString(R.string.add_comment).underline()
                    else
                        commentPlaceholder?.text = formatComment(comments[mealItemPosition])
                    commentPlaceholder?.onClick {
                        showCommentDialog(comments[mealItemPosition]){
                            item.comment = comments.also { com -> com[mealItemPosition] = it.toString()}.joinToString(separator = "_")
                            saveComment(item)
                        }
                    }
                }else {
                    commentPlaceholder?.text = formatComment(item.comment)
                    commentPlaceholder?.onClick {
                        showCommentDialog(item.comment) {
                            item.comment = it?.trim()
                            saveComment(item)
                        }
                    }
                }
            }
        }

        private fun saveComment(item: OrderProduct){
            cart.updateItemComment(item.productId, item.comment)
            showComment(item)
        }

        private fun showCommentDialog(previousComment: String?, action: (String?) -> Unit) {
            AlertDialog.Builder(itemView.context)
                .setView(R.layout.dialog_item_comment)
                .create()
                .apply {
                    window?.setBackgroundDrawableResource(R.color.transparent)
                    setOnShowListener {
                        findViewById<EditText?>(R.id.dialog_item_comment_text)
                            ?.run {
                                filterEmoticons()
                                setText(previousComment)
                                setSelection(text?.length ?: 0)
                                requestFocus()
                                delay(200) { showKeyboard(this@run) }
                            }
                        findViewById<TextView?>(R.id.dialog_item_comment_button)?.run {
                            setTextColor(mainColor)
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                setStroke(1.dpToPx, mainColor)
                                cornerRadius = 16.dpToPx.toFloat()
                            }
                            onClick {
                                action(this@apply.findViewById<EditText?>(R.id.dialog_item_comment_text)?.text?.toString())
                                dismiss()
                            }
                        }
                        findViewById<TextView?>(R.id.dialog_item_comment_cancel)?.run {
                            setTextColor(mainColor)
                            onClick { cancel() }
                        }
                    }
                }
                .show()
        }

        private fun formatComment(comment: String?): CharSequence? {
            return comment?.let {
                SpannableStringBuilder().bold { append(getString(R.string.item_checkout_comment)) }
                    .append(" ")
                    .append(it)
            }
        }

        private fun setMode(mode: ItemMode) {
            this.mode = mode
            when (mode) {
                ItemMode.Default -> default()
                ItemMode.Edit -> edit()
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun default() {
            editFinish()
            editAmount?.isInvisible = true
            editUnitType?.isInvisible = true
            editCloseButton?.isInvisible = true
            checkbox?.setOnCheckedChangeListener { _, isChecked ->
                if(cart[productId]?.isChecked == isChecked) return@setOnCheckedChangeListener
                checkboxBackground?.isVisible = isChecked
                updateCart()?.let {
                    onToggle?.invoke(productId, isChecked)
                }
            }
            checkbox?.isChecked = if(item?.cartItem?.meals == null) item?.cartItem?.isChecked ?: false  else  item?.cartItem?.meals?.first()?.first == true
            onToggle?.invoke(productId, item?.cartItem?.isChecked ?: false)
            checkbox?.isVisible = true
            image?.isVisible = true
            name?.isVisible = true
            name?.setThemeColor()
            unitType?.isVisible = true
            price?.isVisible = true
            startEditButton?.isVisible = true
            editDeleteButton?.onClick { removeItem(item) }
            startEditButton?.backgroundTintList = mainColor
            startEditButton?.onClick {
                when {
                    item?.cartItem?.product?.product?.productType == ProductType.PACK -> {
                        openCartMeal()
                    }
                    item?.cartItem?.product?.isToppings == true -> {
                        ToppingsFragment.open(
                            item?.cartItem?.product?.product?.productType == ProductType.PIZZA,
                            item?.cartItem?.product ?: return@onClick,
                            item?.cartItem
                        ) { toppings, options ->
                            toppings?.let {
                                updateCart(null, it, options)
                                setMode(ItemMode.Default)
                            } ?: cart.remove(productId)
                        }
                    }
                    else -> setMode(ItemMode.Edit)
                }
            }
        }

        private fun openCartMeal() {
            item?.let {
                EditMealFragment.open(it, adapterPosition)
            }
        }

        private fun edit() {
            editStart()
            checkbox?.isInvisible = true
            startEditButton?.isInvisible = true
            editAmount?.isVisible = true
            editAmount?.callback = {
                if (cart.products[productId]?.amount ?: 0f < editAmount?.amount ?: 0f) playCartSound()
                updateCart()
                (foregroundFragment as BaseFragment).showSnackBar(
                    getString(
                        R.string.dialog_product_alert_update_success
                    )
                )
            }
            unitType?.isInvisible = true
            amount?.isInvisible = true
            unitType?.isInvisible = true
            amount?.isInvisible = true
            editUnitType?.isVisible = true
            editDeleteButton?.onClick { removeItem(item) }
            editCloseButton?.isVisible = true
            editCloseButton?.onClick {
                setMode(ItemMode.Default)
                item?.cartItem?.let {
                    editAmount?.setAmount(it.amount)
                    editUnitType?.set(it.unitTypeName)
                }
            }
        }
        private fun updateCart(
            levels: MutableList<Pair<Boolean, MutableList<MealProduct>>>? = null,
            toppings: MutableList<MutableList<Pair<Int, List<Int>>>>? = null,
            options: MutableList<MutableList<Pair<Int, List<Int>>>>? = null
        ): Cart.Item? {
            if (item?.cartItem?.meals?.isNotEmpty() == true) {
                item?.cartItem?.productId?.let { cart[it]?.meals?.set(item?.cartItem?.productId?.plus(item?.id ?: 0)?.unaryMinus() ?: 0, item?.cartItem?.meals?.first()?.copy(first = checkbox?.isChecked == true) ?: return@let ) }
            }
            return cart.set(
                productId,
                item?.cartItem?.category ?: return null,
                editUnitType?.type?.type ?: return null,
                (item?.cartItem?.toppings?.size?.toFloat() ?: editAmount?.amount) ?: return null,
                comment = item?.cartItem?.comment,
                isChecked = checkbox?.isChecked ?: false,
                meals = levels ?: item?.cartItem?.productId?.let { cart[it]?.meals },
                toppings = toppings ?: item?.cartItem?.toppings,
                options = options ?: item?.cartItem?.options
            ).also {
                item?.copy(cartItem = it)?.let(::setItemDetails)
            }
        }
    }

    private fun editStart() {
        isEditing = true
    }

    private fun editFinish() {
        isEditing = false
    }

    private fun getViewHolders(): List<CartAdapter.ViewHolder> {
        return (0 until itemCount).mapNotNull { recyclerView?.findViewHolderForAdapterPosition(it) as? CartAdapter.ViewHolder }
    }

    fun checkAll() {
        getViewHolders().forEach { it.checkbox?.isChecked = true }
    }

    fun uncheckAll() {
        getViewHolders().forEach { it.checkbox?.isChecked = false }
    }

}