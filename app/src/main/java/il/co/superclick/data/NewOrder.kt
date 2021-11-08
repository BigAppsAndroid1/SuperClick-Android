package il.co.superclick.data

import com.dm6801.framework.utilities.catch
import il.co.superclick.infrastructure.Locator

data class NewOrder(
    override val userId: Int?,
    override val shopId: Int,
    override val products: List<Cart.Item>? = null,
    override val type: OrderType? = null,
    override val time: Time? = null,
    override val comment: String? = null,
    override val deliveryComment: String? = null,
    override val paymentType: PaymentType? = null,
    val creditCard: CreditCard? = null,
    val cvv: String? = null,
    var distance: Int? = null,
    var deliveryCost: Double? = null
) : Order {

    val sum: Double?
        get() {
                var sum = products?.sumByDouble { it.sum }
                val shop = Locator.database.shop
                sum = sum?.minus(Database.coupon?.getInPercent?.times(sum) ?: 0.0)
            return if (type == OrderType.Delivery)
                (sum ?: 0.0) + if (shop?.deliveryZones?.isEmpty() == true || shop?.isAreaDelivery == true)
                    shop.deliveryCost else catch { Shop.getCompleteDeliveryCost(distance?.toFloat() ?: 0f) } ?: 0.0
            else sum
        }
}
