package il.co.superclick.data

import com.dm6801.framework.infrastructure.foregroundApplication
import il.co.superclick.R
import il.co.superclick.utilities.getString

data class HistoryOrder(
    override val userId: Int?,
    override val shopId: Int,
    override var products: List<HistoryProduct>? = null,
    override val type: OrderType? = null,
    override val paymentType: PaymentType? = null,
    override val time: Time? = null,
    override val comment: String? = null,
    override val deliveryComment: String? = null,
    val id: Int,
    val created: Long? = null,
    val status: String? = null,
    val discount: Int,
    val deliveryCost: Double,
    val totalNew: Double?,
    val totalPay: Double?,
    val link: String
) : Order {
    val sum: Double? get(){
        totalNew?.let {
            return totalNew
        }
        return totalPay ?: return 0.0
    }

    companion object {
        fun statusDisplayName(status: String): String? {
            return when (status) {
                "new" -> getString(R.string.new_order)
                "in_process" -> getString(R.string.prepare_order)
                "collected" -> getString(R.string.order_ready)
                "paid" -> getString(R.string.order_payed)
                else -> null
            }
        }
    }
}