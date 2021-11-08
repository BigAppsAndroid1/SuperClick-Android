package il.co.superclick.order

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.InputType.TYPE_NULL
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import il.co.superclick.order.address_lookup.AddressLookupFragment
import com.dm6801.framework.infrastructure.ensurePermissions
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.infrastructure.showProgressBar
import com.dm6801.framework.ui.getDrawable
import com.dm6801.framework.utilities.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.remote.Place
import il.co.superclick.utilities.*
import il.co.superclick.utilities.delay
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.Dispatchers
import java.util.*


class MapFragment : BaseFragment() {

    companion object : Comp() {

        private val ON_DISTANCE_KEY = "ON_DISTANCE_KEY"
        private val ADDRESS_KEY = "ADDRESS_KEY"
        var isCanAction = true

        fun open(address: String? = null, onAddress: (Float, LatLng) -> Unit) {
            open(
                ON_DISTANCE_KEY to onAddress,
                ADDRESS_KEY to address
            )
        }
    }

    override val layout: Int get() = R.layout.fragment_map
    override val themeBackground: Drawable? get() = getDrawable(android.R.drawable.screen_background_light)
    private val mapView: MapView? get() = map_view
    private val backButton: ImageView? get() = back
    private val titleText: TextView? get() = title
    private val confirmButton: TextView? get() = confirm_address
    private val addressField: EditText? get() = address_field
    private val pencilView: ImageView? get() = pencil
    private val mapFocus: ImageView? get() = map_focus
    private var map: GoogleMap? = null
    private val mapZoom = 18f
    private var onDistance: ((Float, LatLng) -> Unit)? = null
    private var startCoordinates: LatLng? = null

    private var startAddress: String? = null
        set(value) {
            field = value
            startCoordinates = DistanceUtil.getLatLngFromAddress(field ?: return)
        }
    private val locationManager =
        foregroundActivity?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var receivedLocationOnce: Boolean = false
    private val mapIcon: ImageView? get() = center_map
    private var latLng: LatLng = LatLng(32.0768296, 34.7903317)
    private val locationListenerGPS: android.location.LocationListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocation?.invoke(location)
        }
        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
    }
    private var onLocation: ((Location) -> Unit)? = null
    private val callback:((Place) -> Unit) = { place ->
        place.coordinates?.toLatLng()?.let {
            latLng = it
            confirmButton?.callOnClick()
        }
    }

    override fun onResume() {
        super.onResume()
        isCanAction = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backButton?.onClick { foregroundActivity?.popBackStack() }
        setColor()
        ensurePermissions(ACCESS_FINE_LOCATION to {
            setMap(savedInstanceState)
        })
        addressField?.run {
            isFocusable = true
            isFocusableInTouchMode = true
            inputType = TYPE_NULL
            setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP)
                    AddressLookupFragment.open(callback = callback)
                true
            }
        }
        pencilView?.onClick {
            AddressLookupFragment.open(callback = callback)
        }
        mapFocus?.onClick {
            receivedLocationOnce = false
            focusMap()
        }
        confirmButton?.onClick {
            if (isCanAction) {
                DistanceUtil.getDistanceFromShopLatLng(latLng)
                    ?.let { it1 ->
                        onDistance?.invoke(it1, latLng)
                    }
                isCanAction = false
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[ON_DISTANCE_KEY] as? ((Float, LatLng) -> Unit))?.let { onDistance = it }
        (arguments[ADDRESS_KEY] as? String)?.let { startAddress = it }

    }

    private fun setColor() {
        backButton?.imageTintList = mainColor
        titleText?.setTextColor(mainColor)
        confirmButton?.backgroundTintList = mainColor
        pencilView?.imageTintList = mainColor
        mapFocus?.imageTintList = mainColor
        mapIcon?.imageTintList = mainColor
    }

    @SuppressLint("SetTextI18n")
    private fun setMap(savedInstanceState: Bundle?) {
        mapView?.onCreate(savedInstanceState)
        mapView?.onResume()
        try {
            MapsInitializer.initialize(activity?.applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mapView?.getMapAsync {
            map = it
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(startCoordinates ?: latLng, mapZoom))
            if(startCoordinates == null)
                focusMap()
            map?.setOnCameraIdleListener {
                latLng = it.cameraPosition.target
                background {
                    catch {
                        val address = Geocoder(context, Locale.getDefault()).getFromLocation(
                            latLng.latitude,
                            latLng.longitude,
                            1
                        )
                        main {
                            addressField?.setText(startAddress ?: address?.get(0)?.getAddressLine(0).toString())
                            startAddress = null
                        }
                    } ?: kotlin.run { main { toast("אין חיבור לאינטרנט") } }
                }
            }
        }
    }

    private fun checkProvider(onEnabled: () -> Unit) {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(
            foregroundActivity ?: return
        ).checkLocationSettings(builder.build())

        result.addOnCompleteListener {
            try {
                onEnabled.invoke()
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val resolvable: ResolvableApiException =
                                exception as ResolvableApiException
                            resolvable.startResolutionForResult(
                                activity, LocationRequest.PRIORITY_HIGH_ACCURACY
                            )
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> { }
                }
            }
        }
    }

    private fun focusMap() {
        showProgressBar()
        delay(2_000, Dispatchers.Main) {
            hideProgressBar()
        }
        ensurePermissions(ACCESS_FINE_LOCATION to {
            getLocation { location ->
                if (!receivedLocationOnce) {
                    LatLng(location.latitude, location.longitude).run {
                        latLng = this
                        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(this, mapZoom))
                    }
                }
                receivedLocationOnce = true
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun getLocation(onLocation: (Location) -> Unit) {
        this.onLocation = onLocation
        com.dm6801.framework.utilities.catch {
            checkProvider {
                if (locationManager?.getProviders(true)
                        ?.contains(LocationManager.GPS_PROVIDER) == false
                ) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0,
                        0f,
                        locationListenerGPS
                    )
                    com.dm6801.framework.utilities.delay(2_000, Dispatchers.Main) {
                        locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                            ?.let { onLocation.invoke(it) }
                    }
                } else {
                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0,
                        0f,
                        locationListenerGPS
                    )
                    com.dm6801.framework.utilities.delay(2_000, Dispatchers.Main) {
                        locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                            ?.let { onLocation.invoke(it) }
                    }
                }
            }
        }
    }
}