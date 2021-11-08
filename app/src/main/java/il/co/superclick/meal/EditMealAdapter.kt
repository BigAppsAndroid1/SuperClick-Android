package il.co.superclick.meal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import il.co.superclick.R
import il.co.superclick.data.MealProduct
import il.co.superclick.data.UnitType
import il.co.superclick.utilities.glide
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import il.co.superclick.utilities.setThemeColor
import kotlinx.android.synthetic.main.item_edit_meal_product.view.*

class EditMealAdapter(val onItemEditTapped: (Int) -> Unit) : ListAdapter<MealProduct, EditMealAdapter.ViewHolder>(object :
    DiffUtil.ItemCallback<MealProduct>() {
    override fun areItemsTheSame(oldItem: MealProduct, newItem: MealProduct): Boolean = false
    override fun areContentsTheSame(oldItem: MealProduct, newItem: MealProduct): Boolean = false
}) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder.from(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemEditTapped = onItemEditTapped)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val amount: TextView? get() = itemView.item_meal_product_amount
        private val unitType: TextView? get() = itemView.item_meal_product_unit_type
        private val editButton: TextView? get() = itemView.item_meal_product_edit_button
        private val image: ImageView? get() = itemView.item_meal_product_image
        private val name: TextView? get() = itemView.item_meal_product_name


        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.item_edit_meal_product, parent, false)
                return ViewHolder(view)
            }
        }

        fun bind(item: MealProduct, onItemEditTapped:(Int)->Unit){
            name?.setThemeColor()
            name?.text = item.shopProduct.product.name
            amount?.text = (item.amount ?: 1).toInt().toString()
            unitType?.text = UnitType.display(item.shopProduct.unitType.type)
            image?.glide(item.shopProduct.product.image)
            editButton?.backgroundTintList = mainColor
            editButton?.onClick {
                onItemEditTapped.invoke(adapterPosition)
            }
        }

    }
}