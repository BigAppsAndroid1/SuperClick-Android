package il.co.superclick.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.utilities.background
import il.co.superclick.remote.Remote
import il.co.superclick.utilities.preloadImage
import il.co.superclick.utilities.set

private typealias ProductsMap = Map<String, Map<Int, ShopProduct>>

object ProductsRepository {

    private const val CATEGORY_CART = Cart.KEY_CART

    val products: Map<String, Map<Int, ShopProduct>> = mutableMapOf()
    private val liveCategoryProducts: LiveData<ProductsMap> = MutableLiveData(emptyMap())

    private fun ProductsMap.flatten(): Map<Int, ShopProduct> =
        values.fold(mutableMapOf()) { acc, map ->
            acc.putAll(map)
            acc
        }

    suspend fun fetch(category: String, page: Int? = null, pageSize: Int? = null): ProductPage? {
        return if (page != null) {
            if (pageSize != null)
                Remote.getProducts(category, page, pageSize)
            else
                Remote.getProducts(category, page)
        } else {
            Remote.getProducts(category)
        }?.also {
            if(it.products.isNotEmpty())
                cacheProducts(category, it.products)
        }
    }

    suspend fun fetchCategory(categoryId: String, onFinishFetch: () -> Unit) {
        Remote.search(text = "", categoryId = categoryId)?.also { response ->
            cacheProducts("category_$categoryId", response)
        } ?: emptyList()
        onFinishFetch.invoke()
    }

    suspend fun search(name: String): List<ShopProduct> =
        Remote.search(name)?.also { response ->
            cacheProducts(CATEGORY_CART, response)
        } ?: emptyList()

    suspend fun fetch(ids: List<Int>) =
        Remote.getCartProducts(ids)
            .also { cacheProducts(CATEGORY_CART, it) }

    suspend fun fetchAll(ids: List<Int>) =
        Remote.getAllProducts(ids)
            .also { cacheProducts(it) }

    fun clear() {
        (products as MutableMap).clear()
        updateLiveData()
    }

    fun find(id: Int): ShopProduct? = products.flatten()[id]

    fun getLiveData(category: String): LiveData<List<ShopProduct>> =
        Transformations.map(liveCategoryProducts) { it[category]?.values?.toList() }

    private fun cacheProducts(map: Map<String, List<ShopProduct>>) {
        map.filter { it.value.isNotEmpty() }.apply {
            forEach { (category, products) ->
                cacheProducts(category, products)
            }
            background {
                values.first().forEach {
                    foregroundApplication.preloadImage(it.product.image)
                }
            }
        }
    }

    private fun cacheProducts(category: String, products: List<ShopProduct>) {
        val old = ProductsRepository.products[category] ?: emptyMap()
        val new = products.filter { ProductsRepository.products[category]?.values?.any { it1 -> it1.id == it.id } == false || old.isEmpty() }.map { it.id to it }.toMap()
        if (new.isEmpty()) return
        (ProductsRepository.products as MutableMap)[category] = old + new
        updateLiveData()
    }

    private fun updateLiveData() {
        liveCategoryProducts.set(products)
    }


}