package il.co.superclick.data

data class HistoryTopping(
    val price: Double,
    val topping: ShopTopping,
    val total: Double,
    val positions: String
)

data class ShopTopping(
    val id: Int,
    val price: Double,
    val topping: BaseTopping,


)

data class BaseTopping(
    val name: String,
    val codename: String?,
)