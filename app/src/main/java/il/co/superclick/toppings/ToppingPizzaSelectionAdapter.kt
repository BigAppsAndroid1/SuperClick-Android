package il.co.superclick.toppings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import il.co.superclick.R
import il.co.superclick.data.ProductType
import il.co.superclick.data.ShopTopping
import il.co.superclick.utilities.*
import kotlinx.android.synthetic.main.item_pizza_toping.view.*
import kotlin.math.max

class ToppingPizzaSelectionAdapter(
    val freeToppingsAmount: Int?,
    val canAddToppings: Boolean? = null,
    val productType: ProductType?,
    val maxToppings: Int?,
    indexPizza: Int,
    val onChange: (Pair<Int, List<Int>>, MutableList<ShopTopping>) -> Unit
) : ListAdapter<ToppingPizzaSelectionAdapter.Item, ToppingPizzaSelectionAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
) {
    var indexPizza: Int = 0
    var selectedIds = mutableListOf<Int>()

    fun ifNeedToShowPrice(position: Int): Boolean {
        return if (productType == ProductType.PIZZA) {
            val selectedToppings = selectedIds.map { id -> currentList.firstOrNull { item -> item.topping.id == id } }
            val addedSlicesAmount = selectedToppings.map { it?.slices ?: emptyList() }.takeIf { it.isNotEmpty() }?.flatten()?.size ?: 0
            val freeToppings = mutableListOf<Int>()
            var toppingsCounter = 0
            selectedToppings.forEach{ item ->
                toppingsCounter += item?.slices?.size ?: 0
                if (toppingsCounter / 4 <= freeToppingsAmount ?: 0)
                    if (toppingsCounter / 4 == freeToppingsAmount && toppingsCounter % 4 > 0)
                        freeToppings.add(-1)
                    else
                        freeToppings.add(item?.topping?.id ?: 0)
            }
            addedSlicesAmount / 4 >= (freeToppingsAmount ?: 0) && canAddToppings == true && currentList[position].topping.id !in freeToppings
        }else {
            val freeToppings = selectedIds.mapIndexed { index, item ->  if (index < (freeToppingsAmount ?: 0)) item else -1 }.filter { it != -1 }
            currentList.filter { it.isSelected }.size >= (freeToppingsAmount ?: 0) && canAddToppings == true && currentList[position].topping.id !in freeToppings
        }
    }

    init {
        this.indexPizza = indexPizza
    }

    @Suppress("UNUSED_PARAMETER")
    fun submitList(toppings: List<ShopTopping>, a: Unit = Unit) {
        submitList(toppings.mapIndexed { index, shopTopping ->

            Item(
                index.toLong(),
                shopTopping,
                false,
                mutableListOf()
            )
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_pizza_toping, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val topping: CheckBox? get() = itemView.item_topping
        val priceText: TextView? get() = itemView.topping_price

        fun bind(item: Item) {
            topping?.text = item.topping.topping.name
            topping?.buttonTintList = mainColor
            topping?.isChecked = item.isSelected
//            if(item.isSelected && !selectedIds.contains(item.id))
//                selectedIds.add(item.id.toInt())
            priceText?.text = when (item.topping.price) {
                0.0 -> getString(R.string.for_free)
                else -> if (freeToppingsAmount != null)
                    if (ifNeedToShowPrice(adapterPosition) && currentList.size > freeToppingsAmount) item.topping.price.formatPrice()
                    else getString(R.string.for_free)
                else item.topping.price.formatPrice()

            }
            itemView.onClick {
                freeToppingsAmount?.let {
                    if (it > 0 || canAddToppings == true) showDialogChoosePizzaSlices(item)
                    else toppingsLimitToast(freeToppingsAmount)
                } ?: run {
                    maxToppings?.let {
                        showDialogChoosePizzaSlices(item)
                    } ?: run {
                        showDialogChoosePizzaSlices(item)
                    }
                }
            }
        }

        private fun showDialogChoosePizzaSlices(item: Item) {
            if (productType == ProductType.PIZZA) {
                val selectedToppingsList = currentList.filter { it.isSelected }
                val numberSelectedPieces = selectedToppingsList.map { it.slices }.flatten().size
                val maxToppingsAmount =
                    if (canAddToppings != null) if (canAddToppings == false) freeToppingsAmount
                        ?: 0 else 0 else maxToppings
                DialogChoosePizzaSlices.open(
                    maxToppingsAmount ?: 0,
                    numberSelectedPieces,
                    item.topping.topping.codename ?: "",
                    indexPizza,
                    item.topping.id,
                    item.slices.toTypedArray()
                ) { slices ->
                    if (slices.isNotEmpty()) {
                        topping?.isChecked = true
                        item.isSelected = true
                        if(!selectedIds.contains(item.topping.id))
                            selectedIds.add(item.topping.id)
                        item.slices = slices.toMutableList()
                    } else {
                        item.isSelected = false
                        selectedIds.remove(element = item.topping.id)
                        item.slices = slices.toMutableList()
                    }
                    item.slices = slices.toMutableList()
                    if (slices.isNotEmpty())
                        playToppingsSound()
                    onChange(item, slices.toList())
                }
            } else {
                val selectedToppingsList = currentList.filter { it.isSelected }
                var canAddTopping = true
                freeToppingsAmount?.let {
                    if (selectedToppingsList.size == it && canAddToppings == false && topping?.isChecked == false) {
                        toppingsLimitToast(freeToppingsAmount)
                        canAddTopping = false
                    }
                } ?: maxToppings?.let {
                    if (selectedToppingsList.size == it && topping?.isChecked == false) {
                        toppingsLimitToast(maxToppings)
                        canAddTopping = false
                    }
                }

                if (topping?.isChecked == false && canAddTopping) {
                    if(!selectedIds.contains(item.topping.id))
                        selectedIds.add(item.topping.id)
                    topping?.isChecked = topping?.isChecked == false
                    item.isSelected = !item.isSelected
                    if (item.isSelected)
                        playToppingsSound()
                    item.slices = mutableListOf()
                    onChange(item)
                } else if (topping?.isChecked == true) {
                    selectedIds.remove(element = item.topping.id)
                    topping?.isChecked = false
                    item.isSelected = false
                    item.slices = mutableListOf()
                    onChange(item)
                }
            }
            notifyDataSetChanged()
        }
    }

    private fun onChange(item: Item, slices: List<Int>? = null){
        var list = selectedIds.map { id ->  currentList.firstOrNull { it.topping.id == id && it.isSelected }?.topping ?: return }.toMutableList()
        onChange(
            item.topping.id to (slices ?: listOf()),
            selectedIds.map { id ->  currentList.firstOrNull { it.topping.id == id && it.isSelected }?.topping ?: return }.toMutableList()
        )
    }

    data class Item(
        val id: Long,
        val topping: ShopTopping,
        var isSelected: Boolean,
        var slices: MutableList<Int>
    )
}
