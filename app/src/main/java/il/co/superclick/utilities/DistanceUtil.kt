package il.co.superclick.utilities

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.location.Geocoder
import android.location.Location
import com.dm6801.framework.infrastructure.ensurePermissions
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.utilities.Network
import com.dm6801.framework.utilities.catch
import com.dm6801.framework.utilities.main
import com.google.android.gms.maps.model.LatLng
import il.co.superclick.data.Shop
import il.co.superclick.infrastructure.Locator
import il.co.superclick.remote.PlacesApi
import java.io.IOException
import java.util.*


object DistanceUtil {

    private val shop: Shop? get() = Locator.database.shop
    private val geocoder: Geocoder?
        get() = Geocoder(
            foregroundApplication.applicationContext,
            Locale("he")
        )

    fun getDistanceFromShop(address: String): Float? {
        try {
            ensurePermissions(ACCESS_COARSE_LOCATION to {})
            if (address.isEmpty()) return null
            val userLocation = Location("User")
            geocoder?.getFromLocationName(address, 1)?.firstOrNull {
                it.locality != null && it.thoroughfare != null &&
                it.locality.trim()
                    .contains(address.split(",").last().trim()) && it.thoroughfare.split(" ")
                    .firstOrNull { streetPart -> address.split(",").first().split(" ").firstOrNull { streetPart.contains(it) } != null
                    } != null
            }?.apply {
                userLocation.latitude = latitude
                userLocation.longitude = longitude
            } ?: return null
            val shopLocation = Location("Shop")
            geocoder?.getFromLocationName(shop?.address, 1)?.firstOrNull()?.apply {
                shopLocation.latitude = latitude
                shopLocation.longitude = longitude
            } ?: return null
            return userLocation.distanceTo(shopLocation)
        } catch (t: Throwable) {
            main { hideProgressBar() }
            throw IOException()
        }
    }

    suspend fun getDistanceFromShopWithApi(address: String): Float? {
        try {
            if (address.isEmpty()) return null
            val userLocation = Location("User")
            PlacesApi.geocode(address)?.apply {
                userLocation.latitude = latitude
                userLocation.longitude = longitude
            }
            val shopLocation = Location("Shop")
            PlacesApi.geocode(shop?.address)?.apply {
                shopLocation.latitude = latitude
                shopLocation.longitude = longitude
            }
            if (userLocation.latitude == 0.0 || userLocation.longitude == 0.0 || shopLocation.latitude == 0.0 || shopLocation.longitude == 0.0 ) return null
            return userLocation.distanceTo(shopLocation)
        } catch (t: Throwable) {
            main { hideProgressBar() }
            throw IOException()
        }
    }

    fun getDistanceFromShopLatLng(latLng: LatLng?): Float? {
        try {
            latLng ?: return null
            return if (Network.isConnected) {
                val userLocation = Location("User").apply {
                    latitude = latLng.latitude
                    longitude = latLng.longitude
                }
                val shopLocation = Location("Shop")
                if (shop?.shopLat != null && shop?.shopLon != null){
                    shopLocation.latitude = shop?.shopLat!!
                    shopLocation.longitude = shop?.shopLon!!
                }else {
                    geocoder?.getFromLocationName(shop?.address, 1)?.firstOrNull()?.apply {
                        shopLocation.latitude = latitude
                        shopLocation.longitude = longitude
                    }
                }
                userLocation.distanceTo(shopLocation)
            }else null
        }catch (t:Throwable){
            return null
        }
    }

    fun getLatLngFromAddress(address: String): LatLng? {
        try {
            val latLng = geocoder?.getFromLocationName(address, 1)?.firstOrNull() ?: return null
            return LatLng(latLng.latitude, latLng.longitude)
        }catch (_:Throwable){
            return null
        }
    }
}

