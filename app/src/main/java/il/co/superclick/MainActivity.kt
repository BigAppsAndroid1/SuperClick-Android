package il.co.superclick

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.dm6801.framework.infrastructure.AbstractActivity
import com.dm6801.framework.utilities.catch
import il.co.superclick.data.Cart
import il.co.superclick.data.ProductsRepository
import il.co.superclick.dialogs.ConfirmDialog
import il.co.superclick.fragments.SplashFragment
import il.co.superclick.language.LanguageManager

class MainActivity : AbstractActivity() {

    override val layout = R.layout.activity_main
    override val fragmentContainer = R.id.fragment_container

    private val backgroundRefreshTimeout = 300000
    private var timeEnterToBackground: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SplashFragment.open()
        lockFontScale()
    }

    override fun attachBaseContext(base: Context) {
        try {
            super.attachBaseContext(LanguageManager.attachBaseContext(base))
        } catch (t: Throwable) {
            super.attachBaseContext(base)
        }
    }

    @Suppress("DEPRECATION")
    private fun lockFontScale() = catch {
        resources.updateConfiguration(
            resources.configuration.apply { fontScale = 1.06f },
            resources.displayMetrics
        )
    }

    override fun onPause() {
        super.onPause()
        timeEnterToBackground = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (timeEnterToBackground != 0L && (System.currentTimeMillis() - timeEnterToBackground) >= backgroundRefreshTimeout) {
            ProductsRepository.clear()
            this.clearBackStack()
            this.recreate()
        }
        timeEnterToBackground = 0
    }

    override fun onBackPressed() {
        if (isLastFragment) ConfirmDialog.exit()
        else super.onBackPressed()
    }

    override fun onStop() {
        super.onStop()
        Cart.save()
    }

    fun clearStack() {
        try {
            val isPopped = supportFragmentManager.popBackStackImmediate(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            if (!isPopped) supportFragmentManager.popBackStack(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}