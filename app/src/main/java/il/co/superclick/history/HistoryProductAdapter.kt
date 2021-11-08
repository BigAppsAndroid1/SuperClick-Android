package il.co.superclick.history

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.*
import androidx.core.text.bold
import androidx.core.view.isGone
import androidx.core.view.isVisible
import il.co.superclick.R
import il.co.superclick.data.HistoryProduct
import il.co.superclick.infrastructure.RecyclerAdapter
import il.co.superclick.widgets.PriceView
import il.co.superclick.data.OrderProduct
import il.co.superclick.data.ProductType
import il.co.superclick.data.UnitType
import il.co.superclick.dialogs.ProductAlertDialog
import il.co.superclick.utilities.*
import kotlinx.android.synthetic.main.item_history_product_parent.view.*

class HistoryProductAdapter(
    products: List<HistoryProduct>,
) : RecyclerAdapter<HistoryProductAdapter.ViewHolder, HistoryProductAdapter.AdapterItem>() {

    override val layout = R.layout.item_history_product_parent
    override val viewHolderClass = ViewHolder::class.java

    init {
        setHasStableIds(true)
        submitList(products)
    }

    @Suppress("UNUSED_PARAMETER")
    fun submitList(
        products: List<HistoryProduct>,
        a: Boolean = false,
        callback: (() -> Unit)? = null
    ) {
        submitList(products.map { AdapterItem(it) }, callback)
    }

    data class AdapterItem(
        val orderProduct: HistoryProduct
    ) : Identity<Int> {
        override val id = orderProduct.productId
        override fun compareTo(other: Int) = id.compareTo(other)
    }

    inner class ViewHolder(itemView: View) :
        RecyclerAdapter.ViewHolder<AdapterItem>(itemView) {
        private val image: ImageView? get() = itemView.item_history_image
        private val name: TextView? get() = itemView.item_history_name
        private val amount: TextView? get() = itemView.item_history_amount
        private val unitType: TextView? get() = itemView.item_history_unit_type
        private val price: TextView? get() = itemView.item_history_price
        private val editToppings: TextView? get() = itemView.item_history_toppings_button
        private val commentPlaceholder: TextView? get() = itemView.item_history_comment_button


        override fun bind(item: AdapterItem, position: Int, payloads: MutableList<Any>?) {
            super.bind(item, position, payloads)
            val product = item.orderProduct.product
            itemView.tag = product?.id
            editToppings?.backgroundTintList = mainColor
            commentPlaceholder?.setTextColor(Color.WHITE)
            commentPlaceholder?.backgroundTintList = mainColor
            name?.text = product?.product?.name
            name?.setThemeColor()
            image?.glide(product?.product?.image)
            amount?.text = if (item.orderProduct.unitType?.type == UnitType.UNIT) item.orderProduct.amount.toInt().toString() else String.format("%.2f", item.orderProduct.amount)
            unitType?.text = item.orderProduct.unitTypeDisplay
            price?.setText("₪${currencyFormatter.format(item.orderProduct.sum + item.orderProduct.toppings.sumByDouble { it.total } + (item.orderProduct.productOption?.price ?: 0.0))}")
            showHistoryComment(item.orderProduct)
            setToppingsButton(item.orderProduct)
        }

        private fun setToppingsButton(product: HistoryProduct?) {
            editToppings?.isVisible = product?.toppings?.isNotEmpty() == true || product?.product?.product?.productType == ProductType.PACK
            editToppings?.onClick {
                ReceiptWebView.open(product?.link ?: return@onClick)
            }
        }

        private fun showHistoryComment(item: OrderProduct) {
            if (item.comment.isNullOrBlank() || item.comment == "null") {
                commentPlaceholder?.isGone = true
            } else {
                commentPlaceholder?.text = formatComment(item.comment)
                commentPlaceholder?.isVisible = true
                commentPlaceholder?.onClick {
                    item.product?.let {
                        ProductAlertDialog.open(
                            it,
                            item.comment ?: "",
                            closeButton = true,
                            autoClose = null
                        )
                    }
                }
            }
        }

        private fun formatComment(comment: String?): CharSequence? {
            return comment?.let {
                SpannableStringBuilder().bold { append(getString(R.string.item_checkout_comment)) }
                    .append(" ")
                    .append(it)
            }
        }
    }

}