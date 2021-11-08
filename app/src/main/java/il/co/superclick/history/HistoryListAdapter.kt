package il.co.superclick.history

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.getColor
import com.dm6801.framework.ui.onClick
import il.co.superclick.R
import il.co.superclick.data.*
import il.co.superclick.data.Cart.updateLiveData
import il.co.superclick.data.Database.cart
import il.co.superclick.data.Database.shop
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.RecyclerAdapter
import il.co.superclick.product_list.ProductListFragment
import il.co.superclick.product_list.ProductViewHolder
import il.co.superclick.utilities.currencyFormatter
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import il.co.superclick.utilities.setThemeColor
import il.co.superclick.widgets.PriceView
import kotlinx.android.synthetic.main.item_history.view.*
import java.text.SimpleDateFormat
import java.util.*

class HistoryListAdapter :
    RecyclerAdapter<HistoryListAdapter.ViewHolder, HistoryListAdapter.AdapterItem>() {

    companion object {
        private val dateFormatter = SimpleDateFormat("dd.MM.yy", Locale.ROOT)

        fun addHistoryOrderToCard(order: HistoryOrder) {
            var tempProducts = order.products?.toMutableList()
            val filteredProductsPairs = tempProducts?.map { product ->
                val filteredProducts = tempProducts?.filter { it.productId == product.productId }
                tempProducts = tempProducts?.minus(elements = filteredProducts ?: emptyList())?.toMutableList()
                product.productId to filteredProducts
            }?.filter { it.second?.isNotEmpty() == true  }

            filteredProductsPairs?.forEach { productPair ->
                cart.set(
                    id =  productPair.second?.first()?.productId ?: return,
                    category =  productPair.second?.first()?.category ?: return,
                    unitType =  productPair.second?.first()?.unitTypeName ?: return,
                    amount =  productPair.second?.first()?.amount ?: return,
                    comment =  productPair.second?.first()?.comment ?: return,
                    isChecked = true,
                    toppings = productPair.second?.map { it.associatedToppings.toMutableList() }?.flatten()?.toMutableList()?.takeIf { it.first().size > 0 },
                    options = productPair.second?.map { historyProduct -> mutableListOf((historyProduct.productOption?.id ?: -1) to listOf(-1)) }?.toMutableList()?.takeIf { it.first().first().first != -1 }
                )
                cart.updateLiveData()
                cart.save()
            }
        }
    }

    override val layout = R.layout.item_history
    override val viewHolderClass = ViewHolder::class.java

    init {
        setHasStableIds(true)
    }

    fun submitList(list: List<HistoryOrder>) {
        submitList(list.map { AdapterItem(it) })
    }

    data class AdapterItem(
        val order: HistoryOrder
    ) : Identity<Int> {
        override val id = order.id
        override fun compareTo(other: Int) = id.compareTo(other)
    }

    inner class ViewHolder(itemView: View) : RecyclerAdapter.ViewHolder<AdapterItem>(itemView) {
        private val id: TextView? get() = itemView.item_history_id
        private val date: TextView? get() = itemView.item_history_date
        private val sum: TextView? get() = itemView.item_history_sum
        private val type: TextView? get() = itemView.item_history_order_type
        private val time: TextView? get() = itemView.item_history_order_time
        private val status: TextView? get() = itemView.item_history_order_status
        private val orderReadyIcon: ImageView? get() = itemView.item_history_icon
        private val copyOrder: TextView? get() = itemView.item_history_copy_order

        @SuppressLint("SetTextI18n")
        override fun bind(item: AdapterItem, position: Int, payloads: MutableList<Any>?) {
            super.bind(item, position, payloads)
            id?.setThemeColor()
            date?.setThemeColor()
            type?.setThemeColor()
            time?.setThemeColor()
            status?.setThemeColor()
            copyOrder?.setThemeColor()
            copyOrder?.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setStroke(1.dpToPx, mainColor)
                cornerRadius = 16.dpToPx.toFloat()
            }
            copyOrder?.onClick {
                addHistoryOrderToCard(item.order)
                foregroundActivity?.popBackStack()
            }
            orderReadyIcon?.imageTintList = when (item.order.status) {
                "collected", "paid" -> mainColor
                else -> ColorStateList.valueOf(getColor(R.color.black) ?: return)
            }
            itemView.onClick {
                HistoryOrderFragment.open(item.order)
            }
            id?.text = item.order.id.toString()
            date?.text = item.order.created?.let(dateFormatter::format)
            item.order.sum?.let { sum?.setText("₪${currencyFormatter.format(it)}") }
            sum?.setThemeColor()
            item.order.type?.let { OrderType.displayName(it) }?.let { type?.text = it }
            item.order.status?.let { HistoryOrder.statusDisplayName(it) }?.let { status?.text = it }
            val supplyDate = item.order.time?.date?.let(dateFormatter::format)
            item.order.time?.let { time?.text = "${it.from} - ${it.to}\n$supplyDate" }
        }
    }


}