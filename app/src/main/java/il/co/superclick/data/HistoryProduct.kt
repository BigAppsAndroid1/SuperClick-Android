package il.co.superclick.data

data class HistoryProduct(
    override val product: ShopProduct?,
    override val category: String,
    override val unitTypeName: String,
    override val amount: Float,
    override var comment: String?,
    val price: Double?,
    val toppings: List<HistoryTopping>,
    var link: String?,
    val productOption: ProductOption?,
    val total: Double?
) : OrderProduct {

    override val sum: Double get() = amount * ((price ?: product?.unitPrice) ?: 0.0)

    val associatedToppings: MutableList<MutableList<Pair<Int, List<Int>>>>
        get() {
            val toppingsList = mutableListOf<Pair<Int, MutableList<Int>>>()
            this.toppings.map { topping ->
                val slices = Pair(topping.topping.id, mutableListOf<Int>())
                topping.positions.mapIndexed { index, c ->
                    //ASCII evaluation
                    if (c.toInt() - 48 == 1)
                        slices.second.add(index + 1)
                }
                if (slices.second.isEmpty()) slices.second.add(-1)
                toppingsList.add(slices)
            }
            return mutableListOf(toppingsList.toMutableList())
        }
}


