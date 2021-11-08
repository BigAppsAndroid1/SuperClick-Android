package il.co.superclick.dialogs

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.onClick
import il.co.superclick.R
import il.co.superclick.data.HistoryProduct
import il.co.superclick.data.HistoryTopping
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.utilities.formatPrice
import il.co.superclick.utilities.getString
import il.co.superclick.utilities.mainColor
import kotlinx.android.synthetic.main.dialog_toppings.*
import kotlinx.android.synthetic.main.item_dialog_topping.view.*
import kotlinx.android.synthetic.main.item_topping.view.item_topping_price

class ToppingsDialog : BaseDialog() {

    companion object : Comp<ToppingsDialog>() {
        private const val KEY_PRODUCT = "KEY_PRODUCT"
        fun open(product: HistoryProduct) {
            ToppingsDialog.open(
                KEY_PRODUCT to product,
            )
        }
    }

    override val layout: Int get() = R.layout.dialog_toppings
    override val gravity: Int = android.view.Gravity.CENTER
    override val widthFactor = 0.9f
    override val heightFactor = 0.7f
    override val isCancelable: Boolean get() = true
    private val dialogClose: ImageView? get() = dialog_toppings_close
    private val recyclerView: RecyclerView? get() = dialog_toppings_list
    private val title: TextView? get() = dialog_toppings_title
    private var product: HistoryProduct? = null

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_PRODUCT] as? HistoryProduct)?.let { this.product = it }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        title?.setTextColor(mainColor)
        title?.text = "${product?.product?.product?.name}\n"+ getString(R.string.toppings)
        dialogClose?.onClick { dismiss() }
        recyclerView?.adapter = DialogToppingAdapter().apply { submitList(product?.toppings ?: emptyList()) }
    }
}

class DialogToppingAdapter :
    ListAdapter<DialogToppingAdapter.Item, DialogToppingAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem == newItem
        }
    ) {

    @Suppress("UNUSED_PARAMETER")
    fun submitList(toppings: List<HistoryTopping>, a: Unit = Unit) {
        submitList(toppings.mapIndexed { index, shopTopping ->
            Item(index.toLong(), shopTopping)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_dialog_topping, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val toppingName: TextView? get() = itemView.item_topping_name
        private val priceText: TextView? get() = itemView.item_topping_price

        fun bind(item: Item) {
            toppingName?.text = item.topping.topping.topping.name
            priceText?.text = when (item.topping.price) {
                0.0 -> getString(R.string.for_free)
                else -> item.topping.price.formatPrice()
            }
        }
    }

    data class Item(
        val id: Long,
        val topping: HistoryTopping
    )

}