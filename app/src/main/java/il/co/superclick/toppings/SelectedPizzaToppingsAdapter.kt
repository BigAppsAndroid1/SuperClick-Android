package il.co.superclick.toppings

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.ui.getString
import com.dm6801.framework.utilities.catch
import il.co.superclick.R
import il.co.superclick.data.ShopTopping
import il.co.superclick.utilities.formatPrice
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.item_selected_pizza_topping.view.*

class SelectedPizzaToppingsAdapter(
    val toppingsFree: Int?,
    val areOptionsFree: Boolean,
    val isFirstToppingOption: Boolean,
    val state: ToppingsState,
    val index: Int,
    val onPizzaType: () -> Unit,
    val onChange: (topping: ShopTopping) -> Unit,
) :
    ListAdapter<SelectedPizzaToppingsAdapter.Item, SelectedPizzaToppingsAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(
                oldItem: Item,
                newItem: Item
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: Item,
                newItem: Item
            ): Boolean {
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
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_selected_pizza_topping, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val topping: TextView? get() = itemView.name_topping
        private val deleteTopping: ImageView? get() = itemView.iv_delete

        @SuppressLint("SetTextI18n")
        fun bind(item: Item) {
            itemView.backgroundTintList = mainColor
            catch {
                if (adapterPosition == 0 && isFirstToppingOption) {
                    deleteTopping?.isVisible = false
                    itemView.onClick { onPizzaType.invoke() }
                    topping?.text = item.topping.topping.name + "\n" + when {
                        item.topping.price == 0.0 || areOptionsFree -> getString(R.string.for_free)
                        else -> {
                            if (state.associatedOptions.isEmpty()) ""
                            else if (state.associatedOptions.first().isEmpty()) ""
                            else if (state.associatedOptions[index].first().second.first() == -1 && state.associatedOptions[index].first().first == item.topping.id)
                                item.topping.price.formatPrice()
                            else
                                ""
                        }
                    }
                } else {
                    deleteTopping?.isVisible = true
                    itemView.onClick { onChange(item.topping) }
                    topping?.text = item.topping.topping.name + "\n" + when {
                        item.topping.price == 0.0 || (toppingsFree != null && getSlicesAmount() <= toppingsFree.times(4)) -> getString(R.string.for_free)
                        else -> {
                            when {
                                state.associatedToppings.isEmpty() -> ""
                                state.associatedToppings[index].isEmpty() -> ""
                                state.associatedToppings[index].first().second.first() == -1 -> item.topping.price.formatPrice()
                                else -> {
                                    var totalSlices = getSlicesAmount()
                                    if (totalSlices / 4 == toppingsFree && totalSlices % 4 > 0) {
                                        val slices = state.associatedToppings[index][adapterPosition - if (isFirstToppingOption) 1 else 0]
                                        totalSlices -= slices.second.size
                                        var amountToBePaid = 0
                                        slices.second.forEach { _ ->
                                            if (totalSlices / 4 == toppingsFree)
                                                amountToBePaid += 1
                                            totalSlices += 1
                                        }
                                        (item.topping.price / 4 * amountToBePaid).formatPrice()
                                    } else
                                        if (state.toppingsSlices[index].isNullOrEmpty())
                                            item.topping.price.formatPrice()
                                        else
                                            (item.topping.price / 4 * (state.toppingsSlices[index].firstOrNull { it.first == item.topping.id }?.second?.size ?: 1)).formatPrice()
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun getSlicesAmount(): Int {
            val suffix = if (isFirstToppingOption) 0 else 1
            return try {
                state.toppingsSlices.first().subList(0, adapterPosition + suffix).map { it.second }.flatten().size
            } catch (_: Throwable) {
                try {
                    state.associatedToppings.first().subList(0, adapterPosition + suffix).size * 4
                } catch (_: Throwable) {
                    0
                }
            }
        }
    }

    data class Item(
        val id: Long,
        val topping: ShopTopping,
        var isSelected: Boolean
    )
}
