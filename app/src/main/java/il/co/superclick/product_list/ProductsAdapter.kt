package il.co.superclick.product_list

import androidx.recyclerview.widget.RecyclerView
import il.co.superclick.data.ShopProduct
import il.co.superclick.infrastructure.LayoutManager

interface ProductsAdapter {
    var recyclerView: RecyclerView?
    val layoutManager: LayoutManager?
    var focusedProductId: Int?
    var focusedPosition: Int?
    var canMealProductButtonAction: Boolean?
    fun submitList(list: List<ShopProduct>)
}