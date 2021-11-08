package il.co.superclick.toppings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundApplication
import il.co.superclick.R
import il.co.superclick.data.ShopTopping
import il.co.superclick.utilities.formatPrice
import il.co.superclick.utilities.getString
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.item_pizza_type.view.*

class TypePizzaSelectionAdapter(val areOptionsForFree: Boolean = true, val onChange: (topping: ShopTopping) -> Unit) :
    ListAdapter<TypePizzaSelectionAdapter.Item, TypePizzaSelectionAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem == newItem
            }
        }
    ) {

    @Suppress("UNUSED_PARAMETER")
    fun submitList(toppings: List<ShopTopping>, a: Unit = Unit) {
        submitList(toppings.mapIndexed { index, shopTopping ->
            Item(
                index.toLong(),
                shopTopping,
                false
            )
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_pizza_type, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pizzaType: CheckBox? get() = itemView.pizza_type
        private val priceText: TextView? get() = itemView.type_price

        fun bind(item: Item) {
            pizzaType?.text = item.topping.topping.name
            pizzaType?.buttonTintList = mainColor
            priceText?.text = when{
                item.topping.price == 0.0 || areOptionsForFree -> getString(R.string.for_free)
                else -> item.topping.price.formatPrice()
            }
            pizzaType?.onClick {
                pizzaType?.isChecked = true
                onChange(item.topping)
            }
            itemView.onClick {
                pizzaType?.isChecked = true
                onChange(item.topping)
            }
        }
    }

    data class Item(
        val id: Long,
        val topping: ShopTopping,
        var isSelected: Boolean
    )
}
