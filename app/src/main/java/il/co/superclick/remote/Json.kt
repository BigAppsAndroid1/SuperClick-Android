package il.co.superclick.remote

import com.dm6801.framework.remote.toList
import org.json.JSONArray
import org.json.JSONObject

@Suppress("UNCHECKED_CAST")
fun <IN, OUT> JSONArray.iterator(transform: (IN) -> OUT): Iterator<OUT> =
    (0 until length()).asSequence().map { transform(get(it) as IN) }.iterator()

fun JSONObject.getList(name: String): List<JSONObject> {
    return optJSONArray(name)?.toJsonList() ?: emptyList()
}

fun<T> JSONObject.getTypedList(name: String): List<Any?> {
    return optJSONArray(name)?.toList() ?: emptyList<T>()
}

@Suppress("UNCHECKED_CAST")
fun JSONArray.toJsonList(): List<JSONObject> = asSequence().toList()

fun JSONArray.asSequence(): Sequence<JSONObject> =
    (0 until length()).asSequence().map { get(it) as JSONObject }

fun JSONObject.toByteArray(): ByteArray = toString().toByteArray()

fun JSONObject.double(key: String): Double? {
    return if (isNull(key)) null else optDouble(key)
}

fun JSONObject.long(key: String): Long? {
    return if (isNull(key)) null else optLong(key)
}

fun JSONObject.boolean(key: String): Boolean? {
    return if (isNull(key)) null else optBoolean(key)
}

fun JSONObject.string(key: String): String? {
    return if (isNull(key)) null else optString(key)
}