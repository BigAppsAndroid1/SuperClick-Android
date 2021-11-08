@file:Suppress("UNREACHABLE_CODE")

package il.co.superclick

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.ActivityLifecycleObserver
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.onClick
import il.co.superclick.data.Database
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.product_list.ProductListFragment
import il.co.superclick.remote.Remote.parseShopJson
import il.co.superclick.remote.Remote.parseUserJson
import org.json.JSONObject

object Dev {

    val isDev: Boolean
        get() = BuildConfig.DEBUG && BuildConfig.BUILD_TYPE != "release"

    fun init() {
        foregroundApplication.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
            }

            override fun onActivityStarted(p0: Activity) {
            }

            override fun onActivityResumed(p0: Activity) {
                return
                (foregroundActivity as? MainActivity)?.run {
                    val view = LayoutInflater.from(this).inflate(R.layout.dev, contentView, false)
                    view.apply {
                        findViewById<Button?>(R.id.dev_scroll_to)?.onClick {
                            (foregroundFragment as? BaseFragment)?.categoriesBar?.recycler
                                ?.scrollTo(100, 0)
                        }
                        findViewById<Button?>(R.id.dev_smooth_scroll_by)?.onClick {
                            (foregroundFragment as? BaseFragment)?.categoriesBar?.recycler
                                ?.smoothScrollBy(100, 0)
                        }
                        findViewById<Button?>(R.id.dev_layout_scroll)?.onClick {
                            ((foregroundFragment as? BaseFragment)?.categoriesBar?.recycler
                                ?.layoutManager as? LinearLayoutManager)
                                ?.scrollToPositionWithOffset(0, 100)
                        }
                        findViewById<Button?>(R.id.dev_smooth_scroll)?.onClick {

                        }
                    }
                    contentView?.addView(view)
                }
            }

            override fun onActivityPaused(p0: Activity) {
            }

            override fun onActivityStopped(p0: Activity) {
            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
            }

            override fun onActivityDestroyed(p0: Activity) {
            }

        })
    }

    private fun <T> exec(action: () -> T): T? {
        return if (isDev) action() else null
    }

    operator fun invoke(action: Dev.() -> Unit): Boolean {
        return isDev.also { if (it) action(this) }
    }

    fun loadShop() = exec {
        javaClass.classLoader?.getResource("shop.json")?.readText()
            ?.let(::JSONObject)?.parseShopJson()
            ?.also { Database.shop = it }
    }

    fun loadUser() = exec {
        javaClass.classLoader?.getResource("user.json")?.readText()
            ?.let(::JSONObject)?.parseUserJson()
            ?.also { Database.user = it }
    }

}