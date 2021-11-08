@file:Suppress("SpellCheckingInspection")

package il.co.superclick.utilities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.utilities.catch
import com.dm6801.framework.utilities.runOnUiThread
import il.co.superclick.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val weekdayParser = SimpleDateFormat("EEEE", Locale.ROOT)

val hebrewWeekdayParser = SimpleDateFormat("EEEE", Locale("iw"))

val hoursFormatter = SimpleDateFormat("HH:MM", Locale.ROOT)


fun getWeekDay(time: Long) = catch { weekdayParser.format(time) }

fun Long.getHebrewWeekDay() = catch { hebrewWeekdayParser.format(this) }

fun toDayHoursDate(time: String): Date{
    val cal = Calendar.getInstance()
    cal.time = Date()
    cal.set(Calendar.HOUR_OF_DAY, time.substringBefore(":").toInt())
    cal.set(Calendar.MINUTE, time.substringAfter(":").toInt())
    cal.set(Calendar.SECOND, 0)
    return cal.time
}

fun List<Int>.toByteString():String{
    val tempList = mutableListOf(0,0,0,0)
    forEach {
        tempList[it - 1] = 1
    }
    return tempList.joinToString("")
}

@SuppressLint("MissingPermission")
fun call(phone: String) = catch {
    foregroundActivity?.apply {
        ensurePermissions(mapOf(
            Manifest.permission.CALL_PHONE to {
                startActivity(
                    Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
                        .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
        ))
    }
}

@SuppressLint("MissingPermission")
fun dial(phone: String) = catch {
    foregroundActivity?.startActivity(
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
}

fun View.onClick(ms: Long = 1000, action: ((View) -> Unit)? = null) {
    action?.let {
        setOnClickListener {
            it.isClickable = false
            CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(ms)
                it.isClickable = true
            }
            action(this)
        }
    } ?: setOnClickListener(null)
}