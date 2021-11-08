package il.co.superclick.product_list

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.showProgressBar
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.updateMargins
import com.dm6801.framework.utilities.*
import il.co.superclick.R
import il.co.superclick.data.*
import il.co.superclick.dialogs.ConfirmDialog
import il.co.superclick.dialogs.ProductAlertDialog
import il.co.superclick.infrastructure.*
import il.co.superclick.meal.AddOrEditMealDialog
import il.co.superclick.meal.MealFragment
import il.co.superclick.remote.Remote
import il.co.superclick.toppings.ToppingsFragment
import il.co.superclick.utilities.*
import il.co.superclick.widgets.AmountWidget
import il.co.superclick.widgets.UnitTypeWidget
import il.co.superclick.widgets.UnitTypeWidget2
import il.co.superclick.widgets.UnitTypeWidget3
import kotlinx.android.synthetic.main.item_product.view.*
import kotlinx.android.synthetic.main.item_product2.view.*
import kotlinx.android.synthetic.main.item_product2.view.item_product_amount
import kotlinx.android.synthetic.main.item_product2.view.item_product_disable
import kotlinx.android.synthetic.main.item_product2.view.item_product_done_tag
import kotlinx.android.synthetic.main.item_product2.view.item_product_edit_button
import kotlinx.android.synthetic.main.item_product2.view.item_product_image
import kotlinx.android.synthetic.main.item_product2.view.item_product_info
import kotlinx.android.synthetic.main.item_product2.view.item_product_name
import kotlinx.android.synthetic.main.item_product2.view.item_product_out_of_stock
import kotlinx.android.synthetic.main.item_product2.view.item_product_submit_button
import kotlinx.android.synthetic.main.item_product2.view.item_product_tag
import kotlinx.android.synthetic.main.item_product2.view.item_product_unit_price
import kotlinx.android.synthetic.main.item_product2.view.item_product_unit_type


class ProductViewHolder(
    itemView: View,
    private val adapter: ProductsAdapter
) : RecyclerAdapter.ViewHolder<ProductAdapterItem>(itemView) {

    companion object {
        private const val DEFAULT_SUBMIT_TEXT = R.string.item_product_default_submit
        private const val DONE_SUBMIT_TEXT = R.string.item_product_done_submit
        private val DEFAULT_BG_TINT = ColorStateList.valueOf(Color.TRANSPARENT)
        private val cart get() = Locator.database.cart
    }

    private val image: ImageView? get() = itemView.item_product_image
    private val unitPrice: TextView? get() = itemView.item_product_unit_price
    private val name: TextView? get() = itemView.item_product_name
    private val defaultButton: TextView? get() = itemView.item_product_submit_button
    private val doneButton: TextView? get() = itemView.item_product_edit_button
    private val editDeleteButton: TextView? get() = itemView.item_product_delete_button
    private val defaultImageTag: TextView? get() = itemView.item_product_tag
    private val defaultInfoButton: TextView? get() = itemView.item_product_info
    private val unitType: UnitTypeWidget2? get() = itemView.item_product_unit_type
    private val unitTypeFocused: UnitTypeWidget? get() = itemView.item_cart_unit_type_widget
    private val unitTypeWidgetGrid: UnitTypeWidget3? get() = itemView.item_cart_unit_type_widget_grid
    private val typeSpinner: AppCompatSpinner? get() = itemView.item_product_unit_type_spinner
    private val spinnerArrow: ImageView? get() = itemView.spinner_arrow
    private val focusedAmount: AmountWidget? get() = itemView.item_product_amount
    private val doneImageTag: ImageView? get() = itemView.item_product_done_tag
    private val disableOverlay: View? get() = itemView.item_product_disable
    private val outOfStockText: TextView? get() = itemView.item_product_out_of_stock
    private val recyclerView: RecyclerView? get() = adapter.recyclerView
    private val layoutManager: LayoutManager? get() = recyclerView?.layoutManager as? LayoutManager
    private var focusedProductId: Int?
        get() = adapter.focusedProductId
        set(value) {
            adapter.focusedProductId = value
        }
    private var focusedPosition: Int?
        get() = adapter.focusedPosition
        set(value) {
            adapter.focusedPosition = value
        }
    private var mode: ItemMode = ItemMode.Default
    var product: ShopProduct? = null; private set
    var mealCallback: ((
        product: ShopProduct,
        amount: Float?,
        toppings: MutableList<MutableList<Pair<Int, List<Int>>>>?,
        options: MutableList<MutableList<Pair<Int, List<Int>>>>?
    ) -> Unit)? = null
    var level: Level? = null
        set(value) {
            field = value
            (value == null).let {
                if (!it) setMode(ItemMode.Default)
                unitPrice?.isVisible = it
            }
        }
    val productId: Int get() = product?.id ?: -1

    enum class ItemMode {
        Default, Focused, Done;
    }

    override fun bind(
        item: ProductAdapterItem,
        position: Int,
        payloads: MutableList<Any>?
    ) {
        super.bind(item, position, payloads)
        if (Shop.listType == ListType.Horizontal) itemView.updateMargins(
            left = 4.dpToPx,
            right = 4.dpToPx
        )
        if (payloads == null && product?.id == item.product.id) return
        this.product = item.product
        setMode(if (cart.has(productId)) ItemMode.Done else ItemMode.Default)
        displayDetails(item.product)
    }

    @SuppressLint("SetTextI18n")
    private fun displayDetails(product: ShopProduct) {
        product.product.image?.let { image?.glide(it) }
        image?.onClick(1_000) {
            showProgressBar()
            main {
                kotlinx.coroutines.delay(200)
                showMagnifiedImage(image ?: return@main, product.product.image ?: return@main)
            }
        }
        unitPrice?.text = product.unitPrice.formatPrice()
        unitPrice?.isVisible = level == null
        name?.text = product.product.name
        if (Shop.listType != ListType.LinearBig)
            name?.setTextColor(mainColor)
        doneImageTag?.imageTintList = mainColor
        editDeleteButton?.backgroundTintList = mainColor
        editDeleteButton?.onClick {
            ConfirmDialog.deleteProduct(
                productId,
                onConfirm = ({ recyclerView?.adapter?.notifyItemChanged(adapterPosition) })
            )
        }
        if (cart.has(product.id)) {
            displayCartDetails()
        } else {
            focusedAmount?.setProduct(product)
            focusedAmount?.setAmount(product.unitType.multiplier)
            unitType?.set(
                listOf(product.unitTypes.first { it.type == product.defaultUnitType }),
                product.defaultUnitType
            )
            unitType?.setCallback { focusedAmount?.setUnitType(it) }
        }
        when {
            !product.isOutOfStock -> {
                defaultImageTag?.isGone = true
                disable(1f, isOutOfStock = true)
            }
            product.isNew -> {
                defaultImageTag?.isVisible = level == null
                defaultImageTag?.backgroundTintList = newColor
                defaultImageTag?.text = getString(R.string.new_item)
                enable()
            }
            product.isSale -> {
                defaultImageTag?.isVisible = level == null
                defaultImageTag?.backgroundTintList = saleColor
                defaultImageTag?.text = getString(R.string.sale)
                enable()
            }
            else -> {
                defaultImageTag?.isInvisible = true
                enable()
            }
        }
        toggleInfoButton()
    }

    private fun toggleInfoButton() {
        product?.product?.description?.takeIf { it.isNotBlank() }?.let { description ->
            defaultInfoButton?.onClick {
                ProductAlertDialog.open(
                    product ?: return@onClick,
                    description,
                    textAlignment = Gravity.START or Gravity.CENTER_VERTICAL,
                    closeButton = true,
                    autoClose = null
                )
            }
            defaultInfoButton?.isVisible = true
            defaultInfoButton?.text = defaultInfoButton?.text.toString().underlineText()
            defaultInfoButton?.setThemeColor()
        } ?: run {
            if (Shop.listType != ListType.Linear)
                defaultInfoButton?.isVisible = false
            else
                defaultInfoButton?.visibility = View.INVISIBLE
        }

    }

    private fun displayCartDetails() {
        val cartProduct = cart[productId] ?: return
        focusedAmount?.setProduct(cartProduct.product, cartProduct.unitType)
        focusedAmount?.setAmount(cartProduct.amount)
        unitType?.setCallback { unitType ->
            if (focusedAmount?.unitTypeName == unitType.type) return@setCallback
            focusedAmount?.setUnitType(unitType)
            focusedAmount?.setAmount(unitType.multiplier)
        }
    }

    fun setMode(mode: ItemMode) {
        this.mode = mode
        when (mode) {
            ItemMode.Default -> default()
            ItemMode.Focused -> focused()
            ItemMode.Done -> done()
        }
    }

    private fun default() {
        editFinish()
        toggleInfoButton()
        if (adapter !is ProductListAdapter) {
            setTypeWidgetGrid(false)
        } else {
            setTypeWidget()
        }
        unitType?.isInvisible = true
        focusedAmount?.isInvisible = true
        defaultButton?.isInvisible = true
        image?.isVisible = true
        unitPrice?.isVisible = mealCallback == null
        editDeleteButton?.isVisible = false
        (foregroundActivity?.getFragments()
            ?.firstOrNull { it is MealFragment } as? MealFragment)?.let {
            doneImageTag?.isVisible = if (it.mealIndex >= it.meal?.meals?.size ?: 0) false
            else it.meal?.meals?.get(it.mealIndex)?.second?.firstOrNull { mealProduct ->
                mealProduct.shopProduct.product.id == product?.product?.id && mealProduct.level == it.meal?.shopProduct?.product?.levels?.indexOf(
                    level
                )
            } != null
        } ?: run { doneImageTag?.isVisible = false }

        if (adapter is ProductListAdapter && Shop.listType == ListType.Linear)
            name?.updateMargins(left = 6.dpToPx)

        defaultImageTag?.isVisible =
            (product?.isNew == true || product?.isSale == true) && level == null
        doneButton?.apply {
            isVisible = true
            setText(DEFAULT_SUBMIT_TEXT)
            backgroundTintList = mainColor
            scaleX = 1f
            scaleY = 1f
            onClick(1000) {
                if (adapter.canMealProductButtonAction == true) {
                    if (mealCallback != null)
                        adapter.canMealProductButtonAction = false
                    when {
                        product?.product?.productType == ProductType.PACK -> {
                            openMeal()
                            adapter.canMealProductButtonAction = true
                        }
                        product?.isToppings == true -> {
                            openToppings()
                            adapter.canMealProductButtonAction = true
                        }
                        else ->
                            if (mealCallback == null) setMode(ItemMode.Focused)
                            else {
                                product?.let {
                                    (adapter as? ProductListAdapter)?.canMealProductButtonAction
                                    mealCallback?.invoke(
                                        it,
                                        1f,
                                        null,
                                        null
                                    )
                                    (adapter as? ProductListAdapter
                                        ?: adapter as? ProductGridAdapter
                                        ?: adapter as? ProductHorizontalAdapter)?.notifyDataSetChanged()
                                }
                            }
                    }
                }
            }
        }
    }

    private fun focused() {
        editStart(product?.id)
        toggleInfoButton()
        if (adapter !is ProductListAdapter) {
            setTypeSpinner()
        }
        doneImageTag?.isVisible = level == null
        if (mealCallback == null)
            editDeleteButton?.isVisible = true
        doneButton?.isInvisible = level == null
        if (cart.has(productId))
            displayCartDetails()
        focusedAmount?.isVisible = level == null
        focusedAmount?.setAmount(
            cart[productId]?.amount ?: product?.unitType?.multiplier ?: return
        )
        focusedAmount?.callback = {
            if (mealCallback == null)
                addProductToCart()
        }
        defaultButton?.apply {
            isInvisible = true
        }
        if (!cart.has(productId)) addProductToCart()
    }

    private fun done() {
        editFinish()
        toggleInfoButton()
        if (adapter !is ProductListAdapter) {
            setTypeWidgetGrid(true)
            unitType?.isInvisible = true
        } else {
            unitType?.isInvisible = true
            setTypeWidget()
        }
        focusedAmount?.isInvisible = true
        defaultImageTag?.isInvisible =
            (product?.isNew == false && product?.isSale == false || level != null) && product?.isOutOfStock == true
        doneButton?.isInvisible = true
        image?.isVisible = true
        unitPrice?.isVisible = mealCallback == null
        doneImageTag?.isVisible = level == null
        if (mealCallback == null)
            editDeleteButton?.isVisible = true
        defaultButton?.apply {
            isVisible = true
            text = getString(DONE_SUBMIT_TEXT)
            backgroundTintList = mainColor
            onClick {
                when {
                    product?.product?.productType == ProductType.PACK -> {
                        AddOrEditMealDialog.open { openMeal() }
                    }
                    product?.isToppings == true -> openToppings()
                    else -> setMode(ItemMode.Focused)
                }
            }
        }
    }

    private fun setTypeWidget() {
        unitTypeFocused?.apply {
            isVisible = true
            set(
                types = product?.unitTypes ?: return,
                default = cart[productId]?.unitTypeName ?: cart[productId]?.unitType?.type
                ?: product?.defaultUnitType ?: return
            )
            setCallback {
                if (focusedAmount?.unitTypeName != it.type) {
                    focusedAmount?.setProduct(product, it)
                    focusedAmount?.setAmount(it.multiplier)
                } else {
                    focusedAmount?.setProduct(product, it)
                    focusedAmount?.setAmount(cart[productId]?.amount ?: 0f)
                }
                unitType?.set(
                    types = listOf(it),
                    default = it.type
                )
                if (mode == ItemMode.Focused || cart[productId] != null) addProductToCart()
            }
        }
    }

    private fun setTypeWidgetGrid(withInitValue: Boolean) {
        typeSpinner?.isInvisible = true
        spinnerArrow?.isInvisible = true
        unitTypeWidgetGrid?.apply {
            isVisible = true
            set(
                types = product?.unitTypes ?: return,
                default = if (withInitValue) cart[productId]?.unitTypeName
                    ?: cart[productId]?.unitType?.type ?: product?.defaultUnitType
                    ?: return else ""
            )
            setCallback {
                if (mode == ItemMode.Done && focusedAmount?.unitTypeName == it.type) return@setCallback
                if (focusedAmount?.unitTypeName != it.type) {
                    focusedAmount?.setProduct(product, it)
                    focusedAmount?.setAmount(it.multiplier)
                } else {
                    focusedAmount?.setProduct(product, it)
                    focusedAmount?.setAmount(cart[productId]?.amount ?: 0f)
                }
                unitType?.set(
                    types = listOf(it),
                    default = it.type
                )
                addProductToCart()
                setMode(ItemMode.Focused)
            }
        }
    }

    private fun setTypeSpinner() {
        toggleSpinner(true)
        unitTypeWidgetGrid?.isInvisible = true
        typeSpinner?.adapter = ArrayAdapter(
            foregroundFragment?.context ?: foregroundActivity?.baseContext ?: return,
            android.R.layout.simple_list_item_1,
            product?.unitTypes?.map { UnitType.display(it.type) }?.toList()
                ?: listOf(product?.defaultUnitType)
        )
        if (product?.unitTypes?.size?.equals(1) == true) {
            typeSpinner?.isEnabled = false
            spinnerArrow?.isVisible = false
        }
        typeSpinner?.setSelection(product?.unitTypes?.indexOf(cart[productId]?.unitType) ?: 0)
        typeSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(view: AdapterView<*>?, item: View?, pos: Int, l: Long) {
                unitType?.set(
                    types = listOf(product?.unitTypes?.get(pos) ?: return),
                    default = product?.unitTypes?.get(pos)?.type ?: return
                )
                focusedAmount?.setProduct(product, unit = product?.unitTypes?.get(pos))
                if (unitType?.type != cart[productId]?.unitType) {
                    focusedAmount?.setAmount(unitType?.type?.multiplier ?: 0f)
                    if (mode == ItemMode.Focused || cart[productId] != null) addProductToCart()
                } else {
                    focusedAmount?.setAmount(cart[productId]?.amount ?: 0f)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }


    private fun addProductToCart(
        toppings: MutableList<MutableList<Pair<Int, List<Int>>>>? = null,
        options: MutableList<MutableList<Pair<Int, List<Int>>>>? = null,
        dialog: Boolean = true
    ) {
        val isInCart = cart.has(productId)
        val isAdded = cart.products[productId]?.amount ?: 0f < focusedAmount?.amount ?: 0f
        if (toppings == null && isAdded) playCartSound()
        addToCart(toppings, options)?.let {
            if (dialog) {
                if (isInCart)
                    (foregroundFragment as BaseFragment).showSnackBar(
                        getString(
                            R.string.dialog_product_alert_update_success
                        )
                    )
                else
                    (foregroundFragment as BaseFragment).showSnackBar(
                        getString(
                            R.string.dialog_product_alert_add_success
                        )
                    )
            }
        }
    }

    private fun openMeal() {
        product?.let {
            val meal = if (cart.has(productId))
                Meal(it, cart[productId]?.meals ?: mutableListOf())
            else
                Meal(it)
            MealFragment.open(meal)
        }
    }

    private fun openToppings() {
        editStart(product?.id)
        ToppingsFragment.open(
            product?.product?.productType == ProductType.PIZZA,
            product ?: return,
            cart[productId],
            level = level
        ) { toppings, options ->
            toppings?.let {
                if (mealCallback != null) {
                    product?.let { shopProduct ->
                        mealCallback?.invoke(
                            shopProduct,
                            null,
                            toppings,
                            options
                        )
                    }
                    setMode(ItemMode.Default)
                } else {
                    addProductToCart(it, options, dialog = true)
                    setMode(ItemMode.Done)
                }
            } ?: run {
                cart.remove(productId)
                setMode(ItemMode.Default)
            }
        }
    }

    private fun addToCart(
        toppings: MutableList<MutableList<Pair<Int, List<Int>>>>? = null,
        options: MutableList<MutableList<Pair<Int, List<Int>>>>? = null
    ): Cart.Item? {
        if (cart.livePendingItemsSize.value == 0) background { suspendCatch { Remote.remindOrder() } }
        return cart.set(
            productId,
            product?.product?.category ?: return null,
            unitType?.type?.type ?: return null,
            (toppings?.size?.toFloat() ?: focusedAmount?.amount) ?: return null,
            comment = null,
            isChecked = true,
            toppings = toppings,
            options = options
        )
    }

    fun disable(alpha: Float = 1f, isOutOfStock: Boolean = false) {
        disableOverlay?.alpha = alpha
        disableOverlay?.isVisible = true
        outOfStockText?.isVisible = isOutOfStock
    }

    fun enable(alpha: Float = 0f) {
        disableOverlay?.isInvisible = true
        disableOverlay?.alpha = alpha
        outOfStockText?.isGone = true
    }

    private fun editStart(productId: Int?) {
        if (productId == null) return
        main {
            if (focusedPosition != adapterPosition)
                recyclerView?.adapter?.notifyItemChanged(focusedPosition ?: return@main)
        }
        findVisibleViewHolders().firstOrNull { it.product?.id == focusedProductId }
            ?.run { if (cart.has(product?.id ?: 0)) done() else default() }
        focusedProductId = productId
        focusedPosition = adapterPosition
    }

    private fun editFinish() {
        focusedProductId = null
        recyclerView?.backgroundTintList = DEFAULT_BG_TINT
    }

    private fun findVisibleViewHolders(): List<ProductViewHolder> {
        return layoutManager?.visibleItemPositions?.mapNotNull {
            recyclerView?.findViewHolderForLayoutPosition(it) as? ProductViewHolder
        } ?: emptyList()
    }

    private fun toggleSpinner(isVisible: Boolean = false) {
        spinnerArrow?.isVisible = isVisible
        typeSpinner?.isVisible = isVisible
    }

}