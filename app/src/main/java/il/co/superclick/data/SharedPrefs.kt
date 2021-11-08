package il.co.superclick.data

import android.content.Context
import androidx.core.content.edit
import il.co.superclick.BuildConfig
import il.co.superclick.infrastructure.Locator
import com.dm6801.framework.infrastructure.foregroundApplication
import com.google.gson.reflect.TypeToken

object SharedPrefs {

    val json get() = Locator.json
    val prefs by lazy {
        foregroundApplication.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
    }

    inline operator fun <reified T : Any?> set(key: String, value: T?): Boolean {
        if (value == null) return false
        try {
            prefs.edit(commit = true) {
                when (value) {
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                    else -> putString(key, json.toJson(value))
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return true
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline operator fun <reified T : Any?> get(key: String): T? {
        return try {
            val clazz = T::class.java
            val value =  when {
                clazz.isAssignableFrom(java.lang.Integer::class.java) -> prefs.getInt(key, -1)
                clazz.isAssignableFrom(java.lang.Long::class.java) -> prefs.getLong(key, -1L).takeIf { it == -1L }?.let { null }
                clazz.isAssignableFrom(java.lang.Float::class.java) -> prefs.getFloat(key, -1f).takeIf { it == -1f }?.let { null }
                clazz.isAssignableFrom(java.lang.Boolean::class.java) -> prefs.getBoolean(key, false)
                clazz.isAssignableFrom(java.lang.String::class.java) -> prefs.getString(key, null)
                else -> prefs.getString(key, null)
                    ?.let { json -> SharedPrefs.json.fromJson<T>(json, object : TypeToken<T>() {}.type) }
            }
            return if (value == null || value == -1 || value == -1L || value == -1f) null
            else value as? T
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun delete(key: String) {
        prefs.edit { remove(key) }
    }

}