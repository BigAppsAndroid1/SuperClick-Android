package il.co.superclick.language

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import com.dm6801.framework.infrastructure.foregroundActivity
import il.co.superclick.data.Database
import java.util.*

//https://josipsalkovic.com/2020/05/01/changing-locale-uimode-runtime-android/

object LanguageManager {

    private const val KEY_LOCALE = "KEY_LOCALE"
    private val activity get() = foregroundActivity

    val locale: Locale
        get() {
            return try {
                Database.sharedPreferences[KEY_LOCALE] ?: Language.Hebrew.locale
            } catch (t: Throwable) {
                t.printStackTrace()
                Language.Hebrew.locale
            }
        }

    fun setLanguage(language: Language) {
        if (language.locale.language == locale.language) return
        setDefaultLocale(language.locale)
    }

    fun setDefaultLocale(locale: Locale) {
        Database.sharedPreferences[KEY_LOCALE] = locale.language
        Locale.setDefault(locale)
        activity?.recreate()
    }

    @SuppressLint("ObsoleteSdkInt")
    fun attachBaseContext(base: Context): Context {
        Locale.setDefault(locale)
        val config = Configuration(base.resources?.configuration)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setLocaleForApi24(config, locale)
            base.createConfigurationContext(config)
        } else {
            config.setLocale(locale)
            base.createConfigurationContext(config)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun setLocaleForApi24(
        config: Configuration,
        locale: Locale
    ) {
        val set: MutableSet<Locale> = LinkedHashSet()
        set.add(locale)
        val all = LocaleList.getDefault()
        for (i in 0 until all.size())
            set.add(all[i])
        val locales = set.toTypedArray()
        config.setLocales(LocaleList(*locales))
    }

}