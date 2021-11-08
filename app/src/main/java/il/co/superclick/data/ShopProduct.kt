package il.co.superclick.data

import il.co.superclick.R
import il.co.superclick.utilities.formatPrice
import il.co.superclick.utilities.getString

data class ShopProduct(
    val id: Int,
    val unitTypes: List<UnitType>,
    val defaultUnitType: String,
    val isNew: Boolean,
    val isOutOfStock: Boolean,
    val isSeason: Boolean,
    val isSale: Boolean,
    val product: BaseProduct,
    val toppings: List<ShopTopping>?,
    val shopProductOptions: List<ShopTopping>?
) {
    val unitType: UnitType
        get() = unitTypes.find { it.type == defaultUnitType } ?: unitTypes.first()

    val unitPrice: Double get() = unitType.price
    val isToppings: Boolean get() = toppings?.isNotEmpty() == true
}

data class UnitType(
    val type: String,
    val price: Double,
    val multiplier: Float
) {
    companion object {
        const val UNIT_KG = "kg"
        const val UNIT = "unit"
        const val ALL = "all"

        fun display(name: String): String {
            return when (name) {
                UNIT_KG -> getString(R.string.unit_type_kg)
                UNIT -> getString(R.string.unit_type_unit)
                else -> ""
            }
        }
    }
}

data class BaseProduct(
    val id: Int,
    val name: String,
    val image: String?,
    val maxToppings: Int?,
    val imageBig: String?,
    val category: String,
    val productType: ProductType?,
    val description: String?,
    val toppingsDescription: String?,
    val optionsDescription: String?,
    val toppings: List<BaseTopping>,
    val levels: List<Level>?,
    val subCategory: String?,
    )

data class ProductPage(
    val page: Int,
    val pageSize: Int,
    val pages: Int,
    val products: List<ShopProduct>
)

data class ProductOption(
    val price: Double,
    val name: String,
    val id: Int
)

data class Level(
    val id:Int,
    val description: String,
    val productsAmount: Int,
    val toppingsFree: Int,
    val optionsPaid:Int,
    val toppingsAddPaid:Int,
    val products: List<ShopProduct>
)

data class Meal(
    var shopProduct: ShopProduct,
    var meals: MutableList<Pair<Boolean, MutableList<MealProduct>>> = mutableListOf()
)

data class MealProduct(
    val shopProduct: ShopProduct,
    var amount: Float?,
    var level: Int,
    val freeToppings: Int,
    val canAddToppings:Boolean,
    val areOptionsFree: Boolean,
    val toppings: MutableList<MutableList<Pair<Int, List<Int>>>>?,
    val options: MutableList<MutableList<Pair<Int, List<Int>>>>?
){

    private val shopToppings: MutableList<MutableList<ShopTopping>>?
        get() =
            toppings?.map { toppings ->
                shopProduct.toppings?.filter { topping -> topping.id in toppings.map { it.first } }?.toMutableList() ?: mutableListOf()
            }?.toMutableList()


    private val shopOptions: MutableList<MutableList<ShopTopping>>?
        get() =
            options?.map { options ->
                (shopProduct.shopProductOptions?.filter { option -> option.id in options.map { it.first } }?.toMutableList() ?: mutableListOf())
            }?.toMutableList()

    val sum: Double
        get() {
            when {
                shopProduct.product.productType != ProductType.PIZZA -> return shopToppings?.sumByDouble { topping ->
                    if(canAddToppings)
                        if((toppings?.firstOrNull()?.size ?: 0) > freeToppings)
                            toppings?.first()?.subList(if(freeToppings > 0) freeToppings else 0, toppings.firstOrNull()?.size ?: 0)?.map { id -> topping.firstOrNull { it.id == id.first } }?.sumByDouble { it?.price ?: 0.0 } ?: 0.0
                        else
                            0.0
                    else
                        0.0
                }?.plus(if (areOptionsFree) 0.0 else shopOptions?.sumByDouble { option -> option.sumByDouble { it.price } } ?: 0.0) ?: 0.0
                else -> {
                    shopToppings ?: return 0.0
                    var innerSum = 0.0
                    var totalSlices = 0
                    innerSum += toppings?.first()?.sumByDouble { slices ->
                        val sp = shopToppings?.first()?.firstOrNull { slices.first == it.id }
                        totalSlices += slices.second.size
                        when {
                            totalSlices / 4 == freeToppings && totalSlices % 4 > 0 -> {
                                totalSlices -= slices.second.size
                                var amountToBePaid = 0
                                slices.second.forEach { _ ->
                                    if (totalSlices / 4 == freeToppings)
                                        amountToBePaid += 1
                                    totalSlices += 1
                                }
                                sp?.price?.div(4)?.times(amountToBePaid) ?: 0.0
                            }
                            slices.second.isEmpty() || totalSlices / 4 <= freeToppings -> 0.0
                            else -> sp?.price?.div(4)?.times(slices.second.size) ?: 0.0
                        }
                    } ?: 0.0

                    innerSum += if (areOptionsFree) 0.0 else shopOptions?.sumByDouble { option -> option.sumByDouble { it.price } } ?: 0.0
                    return innerSum
                }
            }
        }
}

enum class ProductType(type:String){
    PIZZA("pizza"), PACK("pack")
}
