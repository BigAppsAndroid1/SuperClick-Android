package il.co.superclick.utilities

import android.location.Location
import com.google.android.gms.maps.model.LatLng

@Suppress("unused")
fun LatLng.toLocation(): Location {
    return Location("").apply {
        latitude = this@toLocation.latitude
        longitude = this@toLocation.longitude
    }
}

fun Location.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

fun Location.toCoordinates(): CoOrds {
    return arrayOf(latitude, longitude)
}

fun LatLng.distanceTo(other: LatLng): Float {
    return try {
        val results = floatArrayOf()
        Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, results)
        results.getOrNull(0) ?: -1f
    } catch (_: Exception) {
        -1f
    }
}

fun CoOrds?.distanceTo(other: CoOrds?): Float {
    if (this?.size != 2 || other?.size != 2) return -1f
    return try {
        val results = floatArrayOf()
        Location.distanceBetween(get(0), get(1), other[0], other[1], results)
        results.getOrNull(0) ?: -1f
    } catch (_: Exception) {
        -1f
    }
}