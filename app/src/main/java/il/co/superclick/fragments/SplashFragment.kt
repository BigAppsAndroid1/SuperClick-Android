package il.co.superclick.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.dm6801.framework.infrastructure.AbstractFragment
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.utilities.*
import com.google.android.gms.maps.model.LatLng
import il.co.superclick.BuildConfig
import il.co.superclick.R
import il.co.superclick.data.Cart
import il.co.superclick.data.Database
import il.co.superclick.data.ShopProduct
import il.co.superclick.infrastructure.Locator
import il.co.superclick.network.NetworkFragment
import il.co.superclick.notifications.OneSignalExtender
import il.co.superclick.product_list.ProductListFragment
import il.co.superclick.remote.Remote
import il.co.superclick.utilities.preloadImage
import il.co.superclick.utilities.set
import kotlinx.android.synthetic.main.fragment_splash.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SplashFragment : AbstractFragment() {

    companion object : Comp() {
        private val database get() = Locator.database
        private val repository get() = Locator.repository

        fun open() {
            open(addToBackStack = false)
        }
    }

    override val layout = R.layout.fragment_splash
    private val banner: ImageView? get() = splash_image
    private val developer: ImageView? get() = splash_dev_by
    private val version: TextView? get() = splash_version
    private var productIds: List<Int> = emptyList()
    val locationListenerGPS: android.location.LocationListener =
        object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocation?.invoke(location)

            }

            override fun onProviderDisabled(provider: String) {
                Log(provider)
            }

            override fun onProviderEnabled(provider: String) {
                Log(provider)
            }
        }
    private var onLocation: ((Location) -> Unit)? = null

    init {
        background {
            database.loadUser()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        version?.text = "v${BuildConfig.VERSION_NAME}"
        developer?.onClick { openWebBrowser("https://www.bigapps.co.il/") }
        routeUser()
    }

    private fun routeUser() {
        Log("H! route start ${System.currentTimeMillis()}")
        com.dm6801.framework.infrastructure.requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        getLastKnownLocation {
            Log("H! got location ${System.currentTimeMillis()} + ${it}")
            database.lastKnownLocation = it
            CoroutineScope(Dispatchers.IO).launch {
                database.user?.shopId
                Remote.getShops()?.run {
                    Log("H! got shops ${System.currentTimeMillis()}")
                    if (shops.isNullOrEmpty()) {
                        setShopOld()
                        return@launch
                    }
                    database.network = this
                    database.network?.filterShopsByLocation {
                        Log("H! filtered shops ${System.currentTimeMillis()}")
                        main {
                            close()
                            NetworkFragment.open()
                        }
                    }
                } ?: kotlin.run {
                    setShopOld()
                }
            }
        }
    }

    private fun setShopOld() {
        Log("H! get old shop ${System.currentTimeMillis()}")
        CoroutineScope(Dispatchers.IO).launch {
            Remote.setShop()?.also { Database.shop = it }?.let { shop ->
                productIds = database.cart.products.map { it.key }
                shop.categories.forEach { context.preloadImage(it.iconUrl) }
                val productsMap = getProducts(productIds)
                val cartProducts = productsMap?.get("cart")
                for (id in productIds) {
                    if (cartProducts?.firstOrNull { it.id == id || !it.isOutOfStock } == null)
                        database.cart.remove(id)
                }
                if (productsMap?.isNotEmpty() == true)
                    OneSignalExtender.sendOneSignalId()
                lifecycleScope.launch lifecycle@{
                    close()
                    Log("H! go to main ${System.currentTimeMillis()}")
                    ProductListFragment.open(
                        shop.categories.firstOrNull()?.name ?: ""
                    )
                    Cart.liveSum.set(Cart.products.filterValues { if (it.meals == null) it.isChecked else it.meals?.firstOrNull { meal -> meal.first } != null }.values.sumByDouble { it.sum })
                }
            } ?: routeUser()
        }
    }

    private fun close() = catch {
        activity?.supportFragmentManager?.commit(allowStateLoss = true) {
            setCustomAnimations(
                R.anim.fragment_fade_enter,
                R.anim.fragment_fade_exit,
                R.anim.fragment_fade_enter,
                R.anim.fragment_fade_exit
            )
            remove(this@SplashFragment)
        }
    }

    private suspend fun getProducts(cartProducts: List<Int>): Map<String, List<ShopProduct>>? =
        suspendCatch {
            repository.fetchAll(cartProducts)
        }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(onSuccess: (LatLng) -> Unit) {
        main {
            val lm =
                foregroundApplication.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            lm ?: return@main
            val providers = lm.getProviders(true)
            var location: Location? = null
            try {
                lm.requestLocationUpdates(providers.first(), 0, 0f, locationListenerGPS)
            } catch (_: Throwable) {
                onSuccess.invoke(LatLng(32.0879994, 34.7622266))
            }
            onLocation = {
                location = it
            }
            location?.let {
                onSuccess.invoke(
                    LatLng(
                        location?.latitude ?: return@main,
                        location?.longitude ?: return@main
                    )
                )
            } ?: kotlin.run {
                for (i in providers.indices.reversed()) {
                    location = lm.getLastKnownLocation(providers[i])
                    if (location != null) break
                }
                location?.let {
                    onSuccess.invoke(
                        LatLng(
                            location?.latitude ?: return@main,
                            location?.longitude ?: return@main
                        )
                    )
                } ?: onSuccess.invoke(LatLng(32.0879994, 34.7622266))
            }
        }
    }
}
