package il.co.superclick.data

import il.co.superclick.infrastructure.Locator
import il.co.superclick.remote.Remote
import com.dm6801.framework.utilities.catch
import com.google.android.gms.maps.model.LatLng
import il.co.superclick.remote.ZCredit

object Database {

    private const val KEY_USER = "user"
    private const val KEY_TERMS = "terms"
    private const val KEY_COUPON = "coupon"
    private const val KEY_SHOP_ID = "shop_id"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_ONESIGNAL_ID = "onesignal_id"

    @PublishedApi
    internal val sharedPreferences
        get() = Locator.sharedPreferences

    var network: Network? = null
    var lastKnownLocation: LatLng? = null

    var shop: Shop? = null
        set(value) {
            field = value
            value?.id?.let { shopId ->
                if (!setUserShopId(shopId))
                    setShopId(shopId)
            }
        }

    var user: User? = null
        set(value) {
            field = value
            save(value, KEY_USER)
        }

    var termsAccepted: Boolean?
        set(value) {
            save(value, KEY_TERMS)
        }
        get() = get(KEY_TERMS)

    var coupon: Coupon? = null
        set(value) {
            field = value
            if (value == null)
                sharedPreferences.delete(KEY_COUPON)
            else
                save(value, KEY_COUPON)
        }
        get() = get(KEY_COUPON)

    var onesignalId: String?
        get() = SharedPrefs[KEY_ONESIGNAL_ID]
        set(value) {
            save(value, KEY_ONESIGNAL_ID)
        }

    val shopId: Int? get() = user?.shopId ?: shop?.id ?: sharedPreferences[KEY_SHOP_ID]
    val cart by lazy { Cart }

    val isNotifications: Boolean
        get() = SharedPrefs.get<Boolean>(KEY_NOTIFICATIONS) == true

    inline fun <reified T : Any> save(obj: T?, key: String): Boolean {
        if (obj == null) return false
        return SharedPrefs.set(key, obj)
    }

    inline fun <reified T : Any> get(key: String): T? {
        return SharedPrefs.get<T>(key)
    }

    fun delete(key: String) {
        SharedPrefs.delete(key)
    }

    inline fun <reified T : Any> load(key: String): T? = sharedPreferences[key]

    fun loadUser() {
        user = sharedPreferences[KEY_USER]
        loadCart()
    }

    suspend fun loadShop(): Shop? {
        return (shopId?.let { setShop(it) } ?: setShop())
            ?.also { Cart.load() }
    }

    suspend fun setShop(shopId: Int): Shop? {
        return Remote.setShop(shopId = shopId)
            ?.also { shop = it }
    }

    suspend fun setShop(shopCode: String): Shop? {
        return Remote.setShop(shopCode = shopCode)
            ?.also { shop = it }
    }

    suspend fun setShop(): Shop? {
        return Remote.setShop()
            ?.also { shop = it }
    }

    suspend fun authorizePhone(phone: String, sms: String): User? {
        return Remote.authorizePhone(phone, sms)
            ?.also { user = it }
    }

    suspend fun upsertUser(user: User): Remote.Response<User>? {
        return Remote.upsertUser(user)
            ?.also { response ->
                response.parse()
                    ?.let { this.user = it }
            }
    }

    suspend fun createCreditToken(
        creditNumber: String,
        holderName: String,
        holderId: String,
        expDate: String
    ): CreditCard? {
        if (creditNumber.isBlank()) return null
        val token = ZCredit.validateCard(creditNumber, expDate)
        if (token.isNullOrBlank()) return null
        return CreditCard(
            holderName,
            holderId,
            creditNumber.takeLast(4),
            expDate,
            token
        )
            .also { user = user?.copy(creditCard = it) }
    }

    private fun loadCart() {
        Cart.load()
    }

    fun findCategory(name: String): Category? {
        return shop?.categories?.find { it.name == name }
    }

    private fun setUserShopId(shopId: Int): Boolean {
        return user?.copy(shopId = shopId)?.also {
            user = it
            deleteShopId()
        } != null
    }

    private fun setShopId(shopId: Int) {
        sharedPreferences[KEY_SHOP_ID] = shopId
    }

    fun deleteShopId() {
        SharedPrefs.delete(KEY_SHOP_ID)
    }

    fun setNotifications(isEnabled: Boolean) {
        sharedPreferences[KEY_NOTIFICATIONS] = isEnabled
    }

    fun logout() = catch {
        ProductsRepository.clear()
        shop = null
        delete(KEY_SHOP_ID)
        user = null
        delete(KEY_USER)
        delete(KEY_NOTIFICATIONS)
        Cart.clear()
    }

}