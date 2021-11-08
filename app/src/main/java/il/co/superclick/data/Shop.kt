@file:SuppressLint("DefaultLocale")

package il.co.superclick.data

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import il.co.superclick.R
import il.co.superclick.infrastructure.Locator
import il.co.superclick.utilities.DistanceUtil
import il.co.superclick.utilities.getString

data class Shop(
    val id: Int,
    val code: String,
    val name: String,
    val image: String?,
    val description: String?,
    val about: String?,
    val address: String?,
    val phone: String?,
    val extraPhone: String?,
    val paymentEndpoint: String,
    val paymentKey: String,
    val categories: List<Category>,
    val paymentTypes: List<PaymentType>,
    val orderTypes: List<OrderType>,
    var mainColor: String,
    val layout: Int,
    val withSound: Int,
    val backgroundName: String,
    val minimumOrder: Double,
    val deliveryRadius: Float,
    val deliveryCost: Double,
    val deliveryTimes: List<Time>,
    val pickupTimes: List<Time>,
    val workingTimes: List<Time>,
    val info: Map<String, String?>,
    val paymentDescription: String?,
    val withCoupons: Int,
    val withoutFutureDelivery: Boolean,
    val withoutFuturePickup: Boolean,
    val deliveryZones: List<DeliveryZone>,
    var directPayment: Int,
    val branches: List<ShopBranch>,
    val subCategories: List<Category>,
    val maxPayments: Int,
    val isAreaDelivery: Boolean = false,
    val shopLat: Double? = null,
    val shopLon: Double? = null,
//    val linkFacebook: String? = null,
//    val phoneWhatsapp: String? = null,
) {
    companion object {
        val isShopWithSound: Boolean get() = Locator.database.shop?.withSound == 1
        val isShopWithCoupons: Boolean get() = Locator.database.shop?.withCoupons == 1
        val isDirectPayment: Boolean get() = Locator.database.shop?.directPayment == 1

        val listType: ListType
            get() {
                return when (Locator.database.shop?.layout) {
                    1 -> ListType.Linear
                    2 -> ListType.Grid
                    3 -> ListType.Horizontal
                    else -> ListType.LinearBig
                }
            }

        fun getCompleteDeliveryCost(address: String? = Locator.database.user?.getFullAddress()): Double? {
            val distanceFromShop = DistanceUtil.getDistanceFromShop(address ?: return 0.0)
            distanceFromShop ?: return null
            val shop = Locator.database.shop
            return when {
                shop?.deliveryZones?.isEmpty() == true -> shop.deliveryCost
                else -> shop?.deliveryZones?.firstOrNull { (it.from * 1000) <= distanceFromShop && (it.to * 1000) > distanceFromShop }?.deliveryCost
                    ?: 0.0
            }
        }

        fun getCompleteDeliveryCost(distance: Float): Double {
            val shop = Locator.database.shop
            return when {
                shop?.deliveryZones?.isEmpty() == true -> shop.deliveryCost
                else -> shop?.deliveryZones?.firstOrNull { (it.from * 1000) <= distance && (it.to * 1000) > distance }?.deliveryCost
                    ?: 0.0
            }
        }

        fun getShopColor(): Int {
            return Locator.database.shop?.color
                ?: ContextCompat.getColor(
                    foregroundActivity ?: foregroundApplication,
                    R.color.colorAccent
                )
        }

        fun getBackground(): Int? {

            return try {
                (foregroundActivity ?: foregroundApplication).run {
                    resources.getIdentifier(
                        Locator.database.shop?.backgroundName,
                        "drawable",
                        packageName
                    )
                }
            }catch (_:Throwable){null}
        }
    }

    private val color: Int? get() {
        return try {
            Color.parseColor("#${mainColor}")
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}

enum class ListType {
    Linear, Grid, Horizontal,LinearBig
}

enum class PaymentType {
    Cash, Credit, Cibus, Tenbis, Goodi;
}

enum class OrderType {
    Delivery, Pickup;

    companion object {
        fun displayName(type: OrderType): String {
            return when (type) {
                Delivery -> getString(R.string.delivery)
                Pickup -> getString(R.string.take_away)
            }
        }
    }
}

@Suppress("MemberVisibilityCanBePrivate")
data class Category(
    val id: Int?,
    val name: String,
    val parentId: Int?,
    val displayName: String,
    val iconUrl: String
)

data class DeliveryZone(
    val from: Double,
    val to: Double,
    val deliveryCost: Double
)

data class ShopBranch(
    val id: Int,
    val name: String
) {
    companion object {
        const val BRANCH_KEY = "BRANCH_KEY"
    }
}