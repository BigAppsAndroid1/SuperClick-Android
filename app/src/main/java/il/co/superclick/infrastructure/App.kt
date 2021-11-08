package il.co.superclick.infrastructure

import android.content.Context
import android.content.Intent
import android.os.Looper
import il.co.superclick.notifications.OneSignalExtender
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.dm6801.framework.infrastructure.AbstractApplication
import com.dm6801.framework.infrastructure.AbstractFragment
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.utilities.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import il.co.superclick.Dev
import il.co.superclick.MainActivity
import il.co.superclick.data.Cart
import il.co.superclick.data.Database
import il.co.superclick.data.ProductsRepository
import il.co.superclick.data.SharedPrefs
import il.co.superclick.language.LanguageManager
import kotlin.system.exitProcess

val foregroundFragment: AbstractFragment?
    get() = foregroundActivity?.supportFragmentManager?.fragments
        ?.filterNot { it is SupportRequestManagerFragment }
        ?.lastOrNull() as? AbstractFragment

class App : AbstractApplication() {

    private val Thread.isMainThread: Boolean get() = this == Looper.getMainLooper().thread
    var accessibilityLink: String? = null

    override fun onCreate() {
        super.onCreate()
        OneSignalExtender.init()
        registerGlobalExceptionHandler()
        Dev.init()
    }

    private fun registerGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            if (!t.isMainThread) return@setDefaultUncaughtExceptionHandler
            e.printStackTrace()
            Cart.save()
            exitProcess(1)
        }
    }

    private fun restartApp() {
        Log("BLACK MAGIC restarting...")
        startActivity(Intent(instance, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageManager.attachBaseContext(base))
    }

}

object Locator {
    val sharedPreferences = SharedPrefs
    val json: Gson = GsonBuilder().setLenient().serializeNulls().setPrettyPrinting().create()
    val database = Database
    val repository = ProductsRepository
    val notifications = OneSignalExtender
}