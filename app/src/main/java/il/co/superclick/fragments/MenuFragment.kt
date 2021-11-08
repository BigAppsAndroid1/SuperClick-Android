package il.co.superclick.fragments

import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.dm6801.framework.infrastructure.AbstractFragment
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.utilities.catch
import com.dm6801.framework.utilities.delay
import com.dm6801.framework.utilities.main
import com.dm6801.framework.utilities.openWebBrowser
import il.co.superclick.MainActivity
import il.co.superclick.R
import il.co.superclick.history.HistoryListFragment
import il.co.superclick.infrastructure.App
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.fragment_menu.*

class MenuFragment : AbstractFragment() {

    companion object : Comp() {
        private const val GUEST_USER_NAME = R.string.no_user_name
        private val database get() = Locator.database
        private val user get() = database.user

        fun open() = catch {
            if (foregroundFragment?.javaClass == clazz) return@catch
            open(replace = false)
        }
    }

    override val activity: MainActivity? get() = super.activity as? MainActivity
    override val layout = R.layout.fragment_menu
    private val container: View? get() = menu_container
    private val dimBackground: View? get() = menu_dim
    private val userName: TextView? get() = menu_user_name
    private val userPhone: TextView? get() = menu_user_phone
    private val userEdit: ImageView? get() = menu_user_edit
    private val ordersHistory: TextView? get() = menu_orders_history
    private val aboutShop: TextView? get() = menu_about_shop
    private val contactShop: TextView? get() = menu_contact_shop
    private val aboutUs: TextView? get() = menu_about_us
    private val regulations: TextView? get() = menu_regulations
    private val returnPolicy: TextView? get() = menu_return_policy
    private val privacyPolicy: TextView? get() = menu_privacy_policy
    private val menuAccessibility: TextView? get() = menu_accessibility
    private val separators: List<View?> get() = listOf(menu_separator_1, menu_separator_2, menu_separator_3, menu_separator_4)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setColors()
        loadUserDetails()
        initButtons()
    }

    private fun setColors() {
        separators.forEach { it?.backgroundTintList = mainColor }
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        val anim: Animation? = if (enter) {
            AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
                .apply {
                    setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationEnd(animation: Animation?) {}

                        override fun onAnimationStart(animation: Animation?) {
                            animation ?: return
                            delay(100) { container?.alpha = 1f }
                            delay(animation.duration - 20) {
                                dimFadeIn()
                            }
                        }

                        override fun onAnimationRepeat(animation: Animation?) {}
                    })
                }
        } else {
            AnimationUtils.loadAnimation(
                context,
                R.anim.slide_out_right
            )
        }
        return AnimationSet(true).apply { addAnimation(anim) }
    }

    private fun loadUserDetails() {
        user?.name?.let { username ->
            userName?.text = username
            user?.phone?.let { userPhone?.text = it }
                ?: run { userPhone?.isInvisible = true }
        } ?: run {
            userName?.setText(GUEST_USER_NAME)
            userPhone?.isInvisible = true
        }
    }

    private fun initButtons() {
        userEdit?.imageTintList = mainColor
        userPhone?.onClick { close { ProfileFragment.open() } }
        userName?.onClick { close { ProfileFragment.open() } }
        userEdit?.onClick { close { ProfileFragment.open() } }
        ordersHistory?.isVisible = user != null
        dimBackground?.onClick { close() }
        ordersHistory?.onClick { close { HistoryListFragment.open() } }
        aboutShop?.apply {
            val shopName = database.shop?.name
            if (shopName != null) {
                text = getString(R.string.menu_about_shop)
                onClick {
                   // close {
                    Companion.close()
                        InfoFragment.open(InfoFragment.Type.AboutShop)
                 //   }
                }
            } else {
                isGone = true
            }
        }
        contactShop?.onClick {
           // close {
            Companion.close()
                ContactShopFragment.open()
           // }
        }
        aboutUs?.onClick {
           // close {
            Companion.close()
                InfoFragment.open(
                    InfoFragment.Type.AboutApp
                )
          //  }

        }
        regulations?.onClick {
           // close {
            Companion.close()
                InfoFragment.open(
                    InfoFragment.Type.Regulations
                )
            //}
        }
        returnPolicy?.onClick {
           // close {
            Companion.close()
                InfoFragment.open(
                    InfoFragment.Type.Returns
                )
           // }
        }
        privacyPolicy?.onClick {
           // close {
            Companion.close()
                InfoFragment.open(
                    InfoFragment.Type.Privacy
                )
           // }
        }
        (foregroundApplication as? App)?.accessibilityLink?.takeIf { it.isNotBlank() }?.let { link ->
            menuAccessibility?.isVisible = true
            menuAccessibility?.onClick {
                openWebBrowser(link)
            }
        }
    }


    private fun close(anim: Boolean = true, doAfter: (() -> Unit)? = null) {
        if (anim) {
            dimFadeOut()
            delay(200) {
                Companion.close()
                doAfter?.let {

                    kotlinx.coroutines.delay(670)
                   main { it.invoke() }

                }
            }
        } else {
            Companion.close()
            doAfter?.let {
                delay(400) { it.invoke() }
            }
        }
    }

    private fun dimFadeIn() = catch {
        dimBackground?.animate()?.alpha(1f)?.setDuration(300)?.start()
    }

    private fun dimFadeOut() = catch {
        dimBackground?.animate()?.alpha(0f)?.setDuration(300)?.start()
    }

    override fun onBackPressed(): Boolean {
        close()
        return true
    }

}