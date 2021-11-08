package il.co.superclick.product_list

import il.co.superclick.data.ShopProduct
import il.co.superclick.infrastructure.RecyclerAdapter

data class ProductAdapterItem(
    val product: ShopProduct
) : RecyclerAdapter.Identity<Int> {
    override val id = product.id
    override fun compareTo(other: Int) = id.compareTo(other)
}