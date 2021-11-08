package il.co.superclick.data

import android.location.Geocoder
import android.location.Location
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.utilities.background
import com.dm6801.framework.utilities.suspendCatch
import com.google.android.gms.maps.model.LatLng
import il.co.superclick.infrastructure.Locator.database
import il.co.superclick.remote.PlacesApi

data class Network(
    val welcomeMessage: String?,
    var shops: List<ShortShop>?,
    val tags: List<Tag>?
) {
    fun filterShopsByLocation(onComplete: () -> Unit) {
        background {
            try {
                val locationShops = shops?.map {
                    if (it.shopLat != null && it.shopLon != null){
                        LatLng(it.shopLat, it.shopLon) to it
                    }else {
                        Geocoder(foregroundApplication).getFromLocationName(it.address, 1)
                            .firstOrNull()
                            ?.run { LatLng(latitude, longitude) } to it
                    }
                }
                locationShops?.sortedBy {
                    Location("my").apply {
                        latitude = database.lastKnownLocation?.latitude ?: return@apply
                        longitude = database.lastKnownLocation?.longitude ?: return@apply
                    }.distanceTo(
                        Location("shop").apply {
                            latitude = it.first?.latitude ?: return@apply
                            longitude = it.first?.longitude ?: return@apply
                        }
                    )
                }.apply {
                    shops = this?.map { it.second }
                    onComplete.invoke()
                }
            }catch (t: Throwable){
                t.printStackTrace()
                onComplete.invoke()
            }
        }
    }
}

data class ShortShop(
    val id: Int,
    val name: String,
    val address: String?,
    val image: String,
    val tags: List<Int>,
    val deliveryTimeMinutesFrom: Int?,
    val deliveryTimeMinutesTo: Int?,
    val isMakingDelivery: Boolean = false,
    val shopLat: Double? = null,
    val shopLon: Double? = null
)

data class Tag(
    val id: Int,
    val name: String,
) {
    var isChecked: Boolean = false
}