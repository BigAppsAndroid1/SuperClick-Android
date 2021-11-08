package il.co.superclick.utilities

import android.location.Location
import com.dm6801.framework.utilities.catch
import com.google.android.gms.maps.model.LatLng

internal typealias CoOrds = Array<Double>

internal fun CoOrds.asString(): String {
    val lat = getOrNull(0) ?: return ""
    val lng = getOrNull(1) ?: return ""
    return "$lat,$lng"
}

fun CoOrds.toLatLng(): LatLng {
    return LatLng(get(0), get(1))
}

fun CoOrds.destruct(): Pair<Double, Double>? {
    return if (size == 2) get(0) to get(1)
    else null
}

fun CoOrds.toLocation(): Location? = catch {
    if (size == 2) Location("").apply {
        latitude = get(0)
        longitude = get(1)
    }
    else null
}

fun CoOrds.toPair(): Pair<Double, Double>? = catch {
    if (size == 2) get(0) to get(1)
    else null
}

fun LatLng.toCoordinates(): CoOrds {
    return arrayOf(latitude, longitude)
}

data class LocationData(
    var latitude: Double,
    var longitude: Double,
    var address: String?
) {
    override fun toString(): String = "$address" //DEBUG
    val coOrds: CoOrds get() = arrayOf(latitude, longitude)
}

data class RelationData(
    var origin: LocationData,
    var destination: LocationData,
    var distance: Int,
    var duration: Int
) {
    override fun toString(): String {
        return """
                origin=$origin
                dest=$destination
                distance=$distance
                duration=$duration
            """.trimIndent()
    }
}