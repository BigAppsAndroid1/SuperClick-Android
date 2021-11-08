package il.co.superclick.data

interface OrderProduct {
    val product: ShopProduct?
    val productId: Int get() = product?.id ?: -1
    val category: String
    val amount: Float
    val sum: Double?
    var comment: String?
    val unitTypeName: String
    val unitTypeDisplay: String
        get() = UnitType.display(unitTypeName)
    val unitType: UnitType?
        get() = product?.unitTypes?.find { it.type == unitTypeName }
            ?: product?.unitTypes?.firstOrNull { it.type == product?.defaultUnitType }
    val unitPrice: Double
        get() = unitType?.price
            ?: product?.unitPrice
            ?: 0.0
}