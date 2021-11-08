package il.co.superclick.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import il.co.superclick.utilities.set
import com.dm6801.framework.utilities.background
import il.co.superclick.infrastructure.Locator.database

typealias CartProducts = Map<Int, Cart.Item>

object Cart {

    const val KEY_CART = "cart"

    val products: CartProducts = mutableMapOf()

    private val pendingProducts: CartProducts get() = products.filterValues { it.amount > 0 && if (it.meals == null) it.isChecked else it.meals?.firstOrNull { meal -> meal.first } != null }
    val liveProducts: LiveData<Map<String, List<Item>>> = MutableLiveData(emptyMap())
    val liveSum: LiveData<Double> = MutableLiveData(0.0)
    val livePendingItemsSize: LiveData<Int> = MutableLiveData(0)

    operator fun invoke(shopId: Int): Item? = get(shopId)

    operator fun get(shopId: Int): Item? = products[shopId]

    fun set(
        id: Int,
        category: String,
        unitType: String,
        amount: Float,
        comment: String?,
        isChecked: Boolean,
        meals: MutableList<Pair<Boolean,MutableList<MealProduct>>>? = null,
        toppings: MutableList<MutableList<Pair<Int, List<Int>>>>?,
        options: MutableList<MutableList<Pair<Int, List<Int>>>>?
    ): Item {
        return Item(
            productId = id,
            category,
            unitType,
            amount,
            comment,
            isChecked,
            meals,
            toppings,
            options
        ).put()
    }

    fun updateItemComment(id: Int, comment: String?): Item? {
        return products[id]?.apply { this.comment = comment }?.put()
    }

    fun remove(productId: Int): Item? {
        return if (has(productId))
            (products as MutableMap).remove(productId)?.also {
                updateLiveData()
                save()
            }
        else null
    }

    private fun Item.put() = apply {
        (products as MutableMap)[productId] = this
        updateLiveData()
        save()
    }

    private fun putAll(items: List<Item>): Map<Int, Item> =
        putAll(items.map { it.productId to it }
            .toMap())

    private fun putAll(items: CartProducts): Map<Int, Item> {
        return (products as MutableMap)
            .apply { clear(); putAll(items) }
            .updateLiveData()
            .also { save() }
    }

    fun has(shopId: Int): Boolean = products.containsKey(shopId)

    fun save() = background {
        if (products.isNotEmpty())
            Database.save(
                products.values,
                KEY_CART
            )
        else
            Database.delete(KEY_CART)
    }

    fun load() {
        putAll(Database.load<List<Item>>(KEY_CART) ?: return)
    }

    fun clear() {
        Database.delete(KEY_CART)
        (products as MutableMap).clear()
        updateLiveData()
    }

    fun <T> T.updateLiveData(): T {
        liveProducts.set(products.map { it.value }.groupBy { it.category })
        val sum = products.filterValues { if (it.meals == null) it.isChecked else it.meals?.firstOrNull { meal -> meal.first } != null}.map { it.value.sum }.sum()
        (database.coupon?.getInPercent)?.let { liveSum.set(sum - sum * it) } ?: liveSum.set(sum)
        livePendingItemsSize.set(pendingProducts.values.sumBy { it.meals?.size ?: 1 })
        return this
    }

    fun uncheckAll() {
        putAll(products.mapValues {
            it.value.copy(isChecked = false)
        })
    }

    data class Item(
        override var productId: Int,
        override val category: String,
        override val unitTypeName: String,
        override val amount: Float,
        override var comment: String?,
        val isChecked: Boolean,
        var meals: MutableList<Pair<Boolean, MutableList<MealProduct>>>?,
        val toppings: MutableList<MutableList<Pair<Int, List<Int>>>>?,
        val options: MutableList<MutableList<Pair<Int, List<Int>>>>?,
        var id: Int? = null
    ) : OrderProduct {
        @Transient
        override var product: ShopProduct? = null
            get() {
                if (field != null) return field
                field = ProductsRepository.find(productId)
                return field
            }
            private set

        val shopToppings: MutableList<MutableList<ShopTopping>>?
            get() =
                toppings?.map { toppings ->
                    product?.toppings?.filter { topping -> topping.id in toppings.map { it.first } }?.toMutableList() ?: mutableListOf()
                }?.toMutableList()


        val shopOptions: MutableList<MutableList<ShopTopping>>?
            get() =
                options?.map { options ->
                    (product?.shopProductOptions?.filter { option -> option.id in options.map { it.first } }?.toMutableList() ?: mutableListOf())
                }?.toMutableList()

        val mealProductSum: Double
            get() = meals?.sumByDouble { (product?.unitPrice ?: 0.0) + it.second.sumByDouble { mealProduct -> mealProduct.sum} } ?: 0.0


        override val sum: Double
            get() {
                when {
                    product?.product?.productType == ProductType.PACK -> {
                        return meals?.sumByDouble { if (it.first) (product?.unitPrice ?: 0.0) + it.second.sumByDouble { mealProduct -> mealProduct.sum} else 0.0} ?: 0.0
                    }
                    product?.product?.productType != ProductType.PIZZA -> return shopToppings?.sumByDouble { topping -> unitPrice + topping.sumByDouble { it.price } }?.plus(shopOptions?.sumByDouble { option -> option.sumByDouble { it.price } } ?: 0.0) ?: unitPrice * amount
                    else -> {
                        shopToppings ?: return unitPrice * amount
                        var innerSum = unitPrice * amount
                        shopToppings?.forEachIndexed { index, shopTopping ->
                            innerSum += shopTopping.sumByDouble { sp ->
                                val slices = toppings?.get(index)?.firstOrNull { it.first == sp.id }
                                when {
                                    slices?.second?.isEmpty() == true -> 0.0
                                    slices?.second?.first() == -1 -> sp.price
                                    else -> sp.price / 4 * (slices?.second?.size ?: 0)
                                }

                            }
                        }
                        innerSum += (shopOptions?.sumByDouble { option -> option.sumByDouble { it.price } } ?: 0.0)
                        return innerSum
                    }
                }
            }
    }

}
