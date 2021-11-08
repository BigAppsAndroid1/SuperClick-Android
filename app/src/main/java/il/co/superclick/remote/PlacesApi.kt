package il.co.superclick.remote

import com.dm6801.framework.remote.Http
import com.dm6801.framework.remote.iterator
import com.dm6801.framework.remote.onResult
import com.dm6801.framework.ui.getString
import com.dm6801.framework.utilities.catch
import com.dm6801.framework.utilities.weakRef
import com.google.android.gms.maps.model.LatLng
import il.co.superclick.R
import il.co.superclick.utilities.CoOrds
import il.co.superclick.utilities.toLatLng
import kotlinx.coroutines.Job
import org.json.JSONObject
import java.io.Serializable

object PlacesApi {

    private val API_KEY = getString(R.string.google_maps_key)
    private val client = Http.instance

    /*private var lastSessionId: Long = generateUUID()
    private fun generateUUID(): Long {
        return UUID.randomUUID().leastSignificantBits and 0x0000FFFFFFFFFFFFL
    }*/

    var autoCompleteCache: List<Place>? by weakRef(null); private set
    var detailsCache: Place? by weakRef(null); private set
    var reverseGeocodeCache: Place? by weakRef(null); private set

    fun autocomplete(
        text: String,
        language: String = "iw",
        coordinates: CoOrds? = null,
        radius: Int? = null,
        onFailure: ((Throwable?) -> Unit)? = null,
        onSuccess: (String) -> Unit
    ): Job {
        var arguments = mapOf(
            "key" to API_KEY,
            "input" to text,
            "language" to language
        )
        if (coordinates != null && radius != null)
            arguments = arguments + mapOf(
                "location" to "${coordinates[0]},${coordinates[1]}",
                "radius" to radius.toString()
            )

        return client.get(
            url = "https://maps.googleapis.com/maps/api/place/autocomplete/json",
            arguments = arguments
        ).onResult(onFailure, onSuccess)
    }

    fun parseAutocompleteResult(json: String): List<Place> = catch<List<Place>?> {
        val jsonObject = JSONObject(json)
        val status = jsonObject.optString("status")
        if (status != "OK") return@catch null

        val predictions = jsonObject.optJSONArray("predictions") ?: return@catch emptyList()
        if (predictions.length() <= 0) return@catch emptyList()

        predictions.iterator().asSequence().mapNotNull { prediction ->
            val placeId = prediction.optString("place_id")
            if (placeId.isNullOrBlank()) return@mapNotNull null
            val text = prediction.optJSONObject("structured_formatting")
            val name = text?.optString("main_text")
            val address = text?.optString("secondary_text")
            Place(placeId, name, address, null)
        }.toList()
            .also { autoCompleteCache = it }
    } ?: emptyList()

    fun getDetails(
        placeId: String,
        language: String = "iw",
        onFailure: ((Throwable?) -> Unit)? = null,
        onSuccess: (String) -> Unit
    ): Job {
        return client.get(
            url = "https://maps.googleapis.com/maps/api/place/details/json",
            arguments = mapOf(
                "key" to API_KEY,
                "place_id" to placeId,
                "language" to language
            ),
            progressBar = false
        ).onResult(onFailure, onSuccess)
    }

    fun parseDetailsResult(json: String): Place? = catch {
        val jsonObject = JSONObject(json)
        val status = jsonObject.optString("status")
        if (status != "OK") return@catch null
        jsonObject.optJSONObject("result")?.let(::parsePlaceJson)
            .also { detailsCache = it }
    }

    private fun parsePlaceJson(json: JSONObject): Place? = catch {
        val placeId = json.optString("place_id")
        if (placeId.isNullOrBlank()) return@catch null
        val name = json.optString("name")
        val address = json.optString("formatted_address")
        val geometry = json.optJSONObject("geometry")
        val location = geometry?.optJSONObject("location")
        val latitude = location?.optString("lat")?.toDoubleOrNull()
        val longitude = location?.optString("lng")?.toDoubleOrNull()
        val latLng =
            if (latitude != null && longitude != null) arrayOf(latitude, longitude)
            else null
        Place(placeId, name, address, latLng)
    }

    fun geocodeReverse(
        latitude: Double,
        longitude: Double,
        language: String = "iw",
        onFailure: ((Throwable?) -> Unit)? = null,
        onSuccess: (String) -> Unit
    ): Job {
        return client.get(
            "https://maps.googleapis.com/maps/api/geocode/json",
            arguments = mapOf(
                "key" to API_KEY,
                "language" to language,
                "latlng" to "$latitude,$longitude"
            ),
            progressBar = false
        ).onResult(onFailure, onSuccess)
    }

    fun parseGeocodeReverse(json: String): Place? = catch {
        val jsonObject = JSONObject(json)
        val status = jsonObject.optString("status")
        if (status != "OK") return@catch null
        val results = jsonObject.optJSONArray("results")
        if (results == null || results.length() == 0) return@catch null
        parsePlaceJson(results.iterator().next())
            .also { reverseGeocodeCache = it }
    }

    suspend fun geocode(
        address: String?,
        language: String = "iw",
    ): LatLng? {
        address ?: return null
        client.get(
            "https://maps.googleapis.com/maps/api/geocode/json",
            arguments = mapOf(
                "key" to API_KEY,
                "language" to language,
                "address" to "$address"
            ),
            progressBar = false
        ).await().let { response ->
            if (response.isFailure) return null
            return response.getOrNull()?.let { parseGeocodeReverse(it)?.coordinates?.toLatLng() }
        }
    }

}

data class Place(
    val placeId: String,
    val name: String?,
    val address: String?,
    val coordinates: CoOrds?
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (other !is Place) return false
        if (!coordinates.isNullOrEmpty() && !other.coordinates.isNullOrEmpty())
            if (!coordinates.contentDeepEquals(other.coordinates)) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    val components: List<String> by lazy {
        address?.split(",")?.map { it.trim() } ?: emptyList()
    }

    val processedName: String by lazy {
        when {
            name?.isNotBlank() == true -> name
            components.isEmpty() -> address
            components.size <= 1 -> components.joinToString()
            components.size == 2 -> components.dropLast(1).joinToString()
            components.size >= 3 -> components.dropLast(2).joinToString()
            else -> address
        } ?: ""
    }

    val processedMunicipality: String by lazy {
        when {
            components.isEmpty() -> ""
            components.size <= 1 -> ""
            components.size == 2 -> components.last()
            components.size >= 3 -> components.dropLast(1).last()
            else -> ""
        }
    }
}