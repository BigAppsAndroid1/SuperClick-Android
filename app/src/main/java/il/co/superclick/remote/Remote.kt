@file:Suppress("UNREACHABLE_CODE")

package il.co.superclick.remote

import android.annotation.SuppressLint
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.remote.getList
import com.dm6801.framework.remote.iterator
import com.dm6801.framework.remote.string
import com.dm6801.framework.remote.toMap
import com.dm6801.framework.utilities.*
import com.dm6801.framework.utilities.Network.isConnected
import com.google.android.gms.maps.model.LatLng
import il.co.superclick.R
import il.co.superclick.data.*
import il.co.superclick.data.Network
import il.co.superclick.dialogs.InfoDialog
import il.co.superclick.fragments.InfoFragment
import il.co.superclick.infrastructure.App
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.Locator.repository
import il.co.superclick.order.OrderTypeFragment
import il.co.superclick.utilities.getString
import il.co.superclick.utilities.getWeekDay
import il.co.superclick.utilities.toByteString
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object Remote {

    private val client = Http.instance
    private const val domain = "https://superklikapp.bigapps.co.il/api/"
    //"https://babani-bigapps.jp.ngrok.io/api/"
     //"https://myshop.bigapps.co.il/api/"
    // "https://myshop.bigapps.co.il/api/" //"http://myshop.bigapps.eu.ngrok.io/api/"
     //"https://myshopplus.bigapps.co.il/api/" // "http://myplus.bigapps.eu.ngrok.io/api/" //BuildConfig.DOMAIN

    private fun String.domain() = "$domain$this"

    private const val KEY_BEARER = "Bearer"
    private val database get() = Locator.database
    private val shopId: Int? get() = database.shopId
    private val authToken get() = database.user?.authToken?.let { mapOf(KEY_BEARER to it) }

    //region google
    private val API_KEY = getString(R.string.google_maps_key)
    private val googleClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    //endregion

    init {
        Http.instance.log = true
    }

    data class Response<T>(
        val errorCode: String,
        val errorMessage: String?,
        val rawData: JSONObject?,
        val parser: (JSONObject.() -> T)? = null
    ) {
        val isOk: Boolean get() = errorCode == "0"
        fun parse(): T? = parser?.let { rawData?.run(it) }
    }

    fun throwIfNoNetwork() {
        if (!isConnected) {
            main { getString(R.string.error_no_network)?.let(::toast) }
            throw Exception()
        }
    }

    private suspend fun <T> action(
        name: String,
        arguments: Map<String, Any?>,
        raw: String? = null,
        headers: Map<String, Any?>? = null,
        loader: Boolean = true,
        parser: (JSONObject.() -> T)? = null
    ): Response<T>? {
        throwIfNoNetwork()
        return client.post(name.domain(), arguments, raw, headers, loader)
            .await()
            .parseJson(parser)
    }

    private suspend fun action(
        name: String,
        arguments: Map<String, Any?>,
        raw: String? = null,
        headers: Map<String, Any?>? = null,
        loader: Boolean = true
    ): Response<Any>? {
        throwIfNoNetwork()
        return client.post(name.domain(), arguments, raw, headers, loader)
            .await()
            .parseJson()
    }

    private suspend fun actionGet(
        name: String,
        loader: Boolean = true
    ): Response<Any>? {
        throwIfNoNetwork()
        return client.get(name.domain(), progressBar = loader).await().parseJson()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Result<String>.parseJson(parser: (JSONObject.() -> T)? = null): Response<T>? {
        return getOrNull()?.let(::JSONObject)?.run {
            Response(
                getString("errorCode"),
                optString("errorMessage"),
                optJSONObject("data"),
                parser
            )
        }
    }

    suspend fun getAddress(latLng: LatLng): String? {
        throwIfNoNetwork()
        return suspendCancellableCoroutine { cont ->
            val httpUrl = HttpUrl.Builder()
                .scheme("https")
                .host("maps.googleapis.com")
                .addPathSegments("maps/api/geocode/json")
                .addQueryParameter("key", API_KEY)
                .addQueryParameter("latlng", "${latLng.latitude}, ${latLng.longitude}")
                .addQueryParameter("language", "iw")
                .build()
            try {
                googleClient.newCall(
                    Request.Builder()
                        .get()
                        .url(httpUrl)
                        .build()
                ).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        response.body?.string()?.let {
                            JSONObject(it).getJSONArray("results").asSequence()
                                .firstOrNull()?.run {
                                    cont.resume(optString("formatted_address"))
                                } ?: cont.resume(null)
                        } ?: cont.resume(null)
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        cont.cancel(e)
                    }
                })
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    suspend fun getShops(): Network? {
        actionGet("get-shops", loader = false)?.run {
            return if (errorCode == "0") {
                this.rawData?.parseShops()
            } else {
                toast(errorMessage.toString())
                null
            }
        } ?: return null
    }

    suspend fun setShop(
        shopId: Int? = null,
        shopCode: String? = null
    ): Shop? {
        val args = mutableMapOf<String, Any?>().apply {
            when {
                shopId != null -> put("shopId", shopId)
                shopCode != null -> put("shopCode", shopCode)
            }
        }
        val result = action(
            name = "set-shop",
            arguments = args,
            loader = false,
            parser = { parseShopJson() }
        )
        return result?.parse()
    }

    suspend fun verifyPhone(phone: String): Boolean {
        //if (Dev {}) return true
        return action(
            name = "user-phone",
            arguments = mapOf("phone" to phone)
        )?.isOk ?: false
    }

    suspend fun authorizePhone(phone: String, smsCode: String): User? {
        //if (Dev { loadUser() }) return null
        val result = action(
            name = "verify-phone",
            arguments = mapOf(
                "phone" to phone,
                "code" to smsCode
            ),
            parser = { parseUserJson() }
        )
        return result?.parse()
    }

    suspend fun setOneSignal() {
        //if (shopId == null) return
        val arguments = mutableMapOf(
            "onesignalId" to Database.onesignalId,
            "shopId" to shopId
        )
        var headers: Map<String, Any?>? = null
        database.user?.let { user ->
            arguments["userId"] = user.id.toString()
            headers = mapOf(KEY_BEARER to user.authToken)
        }
        action(
            name = "set-onesignal",
            arguments = arguments,
            headers = headers,
            loader = false
        )
    }

    suspend fun getProducts(
        category: String = "",
        page: Int = 1,
        pageSize: Int = 20,
        shopId: Int? = Remote.shopId
    ): ProductPage? {
        shopId ?: return null
        val result = action(
            name = "get-products",
            arguments = mapOf(
                "shopId" to shopId,
                "categoryCodename" to category,
                "page" to page,
                "pageSize" to pageSize
            ),
            loader = false,
            parser = { parseProductPage(category) }
        )
        return result?.parse()
    }

    suspend fun getCartProducts(
        cartProductIds: List<Int>,
        shopId: Int? = Remote.shopId
    ): List<ShopProduct> {
        shopId ?: return emptyList()
        val result = action(
            name = "get-products",
            arguments = mapOf(
                "shopId" to shopId,
                "cartProductIds" to cartProductIds
            ),
            parser = { parseCartProducts() }
        )
        return result?.parse() ?: emptyList()
    }

    suspend fun getAllProducts(
        cartProductIds: List<Int>,
        shopId: Int? = Remote.shopId
    ): Map<String, List<ShopProduct>> {
        val _shopId = shopId ?: return emptyMap()
        val result = action(
            name = "get-products",
            arguments = mapOf(
                "shopId" to _shopId,
                "cartProductIds" to cartProductIds
            ),
            loader = false,
            parser = { parseAllProducts() }
        )
        return result?.parse() ?: emptyMap()
    }

    fun checkCoupon(coupon: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        background {
            val arguments = mutableMapOf(
                "shopId" to shopId,
                "coupon" to coupon
            )

            val response = action(
                name = "check-coupon",
                arguments = arguments,
                headers = authToken,
                loader = true
            )

            response ?: run { toast(getString(R.string.error)) }

            withMain {
                if (response?.isOk == true) onSuccess.invoke(
                    response.rawData?.getInt("discount") ?: return@withMain
                )
                else onError.invoke(response?.errorMessage ?: "")
            }
        }
    }

    suspend fun upsertUser(user: User): Response<User>? {
        val arguments = mutableMapOf(
            "shopId" to shopId,
            "name" to user.name,
            "phone" to user.phone,
            "email" to user.email,
            "streetName" to user.streetName,
            "streetNumber" to user.streetNumber,
            "floorNumber" to user.floorNumber,
            "entranceCode" to user.entranceCode,
            "apartmentNumber" to user.apartmentNumber,
            "city" to user.city
        )
        var headers: Map<String, Any?>? = null
        database.user?.let {
            arguments["userId"] = it.id
            headers = mapOf(KEY_BEARER to it.authToken)
        }
        return action(
            "upsert-user",
            arguments = arguments,
            headers = headers,
            loader = true,
            parser = { parseUserJson() }
        )
    }

    suspend fun getDeliveryPrice(latLon: LatLng): Double =
        action(
            name = "get-delivery-price",
            arguments = mapOf(
                "shopId" to shopId,
                "deliveryLat" to latLon.latitude,
                "deliveryLon" to latLon.longitude,
            )
        )?.run {
            if (isOk)
                String.format("%.2f", rawData?.optDouble("deliveryCost", -1.0) ?: -1.0).toDouble()
            else
                -1.0
        } ?: run { main { toast(getString(R.string.server_error)) }; -1.0 }

    private var maxIndex = 0

    @SuppressLint("DefaultLocale")
    suspend fun makeOrder(
        order: NewOrder,
        creditTxIndex: String? = null,
        creditTxConfirm: String? = null,
        creditTxExpDate: String? = null,
        creditTxHolderId: String? = null,
        cvv: String? = null,
        branchId: Int? = null,
        latLng: LatLng? = null,
        numberOfPayment: Int? = 1
    ): HistoryOrder? {
        Log("in transaction: $order")
        if (order.products.isNullOrEmpty()) return null
        if (order.paymentType == PaymentType.Credit && creditTxIndex == null && creditTxConfirm == null && creditTxHolderId == null) return null
        val time = order.time ?: return null

        maxIndex = 0
        val productsString =
            order.products.mapIndexed { _, item ->
                maxIndex += 1
                item.toQuery(
                    "products"
                )
            }.joinToString("&")

        val newTime =
            if ((database.shop?.withoutFuturePickup == true && order.type == OrderType.Pickup) || (database.shop?.withoutFutureDelivery == true && order.type == OrderType.Delivery))
                Time(-1, "", "12:00:00", "12:00:00", Date().time / 1000)
            else
                time

        val arguments = mutableMapOf(
            "userId" to order.userId,
            "shopId" to order.shopId,
            "onesignalId" to Database.onesignalId,
            "orderType" to (order.type?.name?.toLowerCase() ?: return null),
            "paymentType" to (order.paymentType?.name?.toLowerCase() ?: return null),
            "deliveryDate" to (newTime.date ?: return null),
            "deliveryFrom" to newTime.from,
            "deliveryTo" to newTime.to,
            "comment" to (order.comment ?: ""),
            "coupon" to (database.coupon?.coupon ?: ""),
            "distance" to order.distance,
            "deliveryCost" to order.deliveryCost,
            "source" to "app"
        )


        if (database.shop?.branches?.isNotEmpty() == true && branchId != null)
            arguments["branchId"] = branchId

        if (order.type == OrderType.Delivery) arguments["deliveryComment"] =
            order.deliveryComment ?: ""

        latLng?.run {
            arguments.putAll(
                mapOf(
                    "deliveryLat" to latLng.latitude,
                    "deliveryLon" to latLng.longitude
                )
            )
        }

        if (order.paymentType == PaymentType.Credit) {
            arguments.putAll(
                mapOf(
                    "creditTxHolderId" to creditTxHolderId,
                    "creditTxExpDate" to creditTxExpDate,
                    "creditTxConfirm" to creditTxConfirm,
                    "creditTxCardNo" to order.creditCard?.lastDigits,
                    "creditNumberOfPayments" to numberOfPayment

                )
            )
            if (Shop.isDirectPayment == true) {
                arguments["creditTxCVV"] = cvv
            } else {
                arguments["creditTxIndex"] = creditTxIndex
            }
        }

        return action(
            name = "make-order",
            arguments = arguments,
            raw = "&$productsString",
            headers = authToken,
            loader = true,
            parser = { parseHistoryOrder() }
        )?.run {
            when (errorCode) {
                "DATE_ERROR" -> {
                    main {
                        reloadOrderAlert(
                            if (database.shop?.withoutFutureDelivery == false && database.shop?.withoutFuturePickup == false)
                                R.string.dialog_make_order_date_error
                            else
                                R.string.no_current_order
                        )
                        hideProgressBar()
                    }
                    null
                }
                "QUERY_ERROR" -> {
                    main {
                        if (errorMessage?.contains("CVV") == true || errorMessage?.contains("creditTxIndex") == true) {
                            reloadOrderAlert(R.string.reorder_to_order_start_page)
                        } else {
                            toast(errorMessage.toString())
                        }
                        hideProgressBar()
                    }
                    null
                }
                else -> {
                    if (errorCode.contains("COUPON")) {
                        main { toast(errorMessage.toString()) }
                    }
                    if (errorCode != "0" || this.rawData == null) {
                        main { toast(errorMessage.toString()) }
                    }
                    Database.coupon = null
                    parse()
                }
            }
        } ?: run { main { toast("שגיאת שרת") }; null }
    }

    private fun reloadOrderAlert(text: Any) {
        InfoDialog.open(
            text,
            R.string.dialog_make_order_confirm to {
                foregroundActivity?.showProgressBar()
                background {
                    database.loadUser()
                    setShop(shopId)?.run {
                        Cart.load()
                        val color = database.shop?.mainColor ?: return@run
                        database.shop = this
                        database.shop?.mainColor = color
                        suspendCatch {
                            repository.fetchAll(database.cart.products.map { it.key })
                        }
                        main {
                            OrderTypeFragment.open()
                            foregroundActivity?.hideProgressBar()
                        }
                    }
                }
            }
        )
    }

    suspend fun remindOrder() {
        action(
            name = "remind-order",
            arguments = mapOf("onesignalId" to Database.onesignalId)
        )
    }

    private fun Cart.Item.toQuery(name: String): String {
        return when {
            meals?.isNullOrEmpty() == false -> {
                var itemString = ""
                meals?.forEachIndexed { _, meal ->
                    if (meal.first) {
                        var mealString = ""
                        maxIndex += 1
                        val mealIndex = maxIndex
                        meal.second.forEachIndexed { _, mealProduct ->
                            maxIndex += 1
                            when {
                                mealProduct.toppings.isNullOrEmpty() -> {
                                    mealString += "&$name[$maxIndex][shopProductId]=${mealProduct.shopProduct.id}"
                                    mealString += "&$name[$maxIndex][unitType]=${mealProduct.shopProduct.unitType.type}"
                                    mealString += "&$name[$maxIndex][amount]=${mealProduct.amount}"
                                    mealString += "&$name[$maxIndex][relatedToPack]=${mealIndex}"
                                    mealString += "&$name[$maxIndex][levelId]=${
                                        this@toQuery.product?.product?.levels?.get(
                                            mealProduct.level
                                        )?.id
                                    }"
                                }
                                else -> {
                                    var mealProductWithToppings = ""
                                    var totalSlices = 0
                                    var maxToppingIndex = 0
                                    val productToppings =
                                        mealProduct.toppings.first().mapIndexed { i, id ->
                                            var topping = ""
                                            maxToppingIndex += i
                                            when {
                                                id.second.first() > 0 -> {
                                                    if (product?.product?.levels?.get(mealProduct.level)?.toppingsAddPaid == 0)
                                                        topping += generateToppingWithPositionString(
                                                            name,
                                                            maxToppingIndex,
                                                            id,
                                                            0
                                                        )
                                                    else {
                                                        val slices = id.second
                                                        totalSlices += slices.size
                                                        when {
                                                            totalSlices / 4 == mealProduct.freeToppings && totalSlices % 4 > 0 -> {
                                                                totalSlices -= slices.size
                                                                val freeSlices =
                                                                    mutableListOf<Int>()
                                                                val paidSlices =
                                                                    mutableListOf<Int>()

                                                                slices.forEachIndexed { _, onOff ->
                                                                    if (totalSlices / 4 < mealProduct.freeToppings) freeSlices.add(
                                                                        onOff
                                                                    )
                                                                    else paidSlices.add(onOff)
                                                                    totalSlices += 1
                                                                }
                                                                if (freeSlices.isNotEmpty()) {
                                                                    topping += generateToppingWithPositionString(
                                                                        name,
                                                                        maxToppingIndex,
                                                                        id.copy(second = id.second.subList(0,freeSlices.size)),
                                                                        0
                                                                    )
                                                                }
                                                                if (paidSlices.isNotEmpty()) {
                                                                    maxToppingIndex += i
                                                                    if (freeSlices.isNotEmpty()) topping += "&"
                                                                    topping += generateToppingWithPositionString(
                                                                        name,
                                                                        maxToppingIndex,
                                                                        id.copy(second = id.second.subList(freeSlices.size, id.second.size)),
                                                                        1
                                                                    )
                                                                }
                                                            }
                                                            totalSlices / 4 > mealProduct.freeToppings -> topping += generateToppingWithPositionString(
                                                                name,
                                                                maxToppingIndex,
                                                                id,
                                                                1
                                                            )
                                                            else -> topping += generateToppingWithPositionString(
                                                                name,
                                                                maxToppingIndex,
                                                                id,
                                                                0
                                                            )
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    val isToppingPaid = when {
                                                        product?.product?.levels?.get(mealProduct.level)?.toppingsAddPaid == 0 -> 0
                                                        i < product?.product?.levels?.get(
                                                            mealProduct.level
                                                        )?.toppingsFree ?: 0 -> 0
                                                        else -> 1
                                                    }
                                                    topping += generateToppingWithPositionString(
                                                        name,
                                                        maxToppingIndex,
                                                        id,
                                                        isToppingPaid
                                                    )
                                                }
                                            }
                                            topping
                                        }.filter { it.isNotEmpty() }.joinToString("&")
                                            .let { "&$it" }

                                    mealProductWithToppings += "&$name[$maxIndex][shopProductId]=${mealProduct.shopProduct.id}"
                                    mealProductWithToppings += "&$name[$maxIndex][unitType]=${mealProduct.shopProduct.unitType.type}"
                                    mealProductWithToppings += "&$name[$maxIndex][amount]=1"
                                    mealProductWithToppings += if (mealProduct.shopProduct.shopProductOptions?.isNotEmpty() == true)
                                        "&$name[$maxIndex][shopProductOptionId]=${
                                            mealProduct.options?.first()?.first()?.first
                                        }"
                                    else
                                        ""
                                    mealProductWithToppings += "&$name[$maxIndex][shopProductOptionIsPaid]=${if (mealProduct.areOptionsFree) 0 else 1}"
                                    mealProductWithToppings += "&$name[$maxIndex][relatedToPack]=$mealIndex"
                                    mealProductWithToppings += "&$name[$maxIndex][levelId]=${
                                        this@toQuery.product?.product?.levels?.get(
                                            mealProduct.level
                                        )?.id
                                    }"
                                    mealProductWithToppings += productToppings

                                    mealString += mealProductWithToppings
                                }
                            }
                        }
                        itemString += if (itemString.isNotEmpty()) "&" else ""
                        itemString += "$name[$mealIndex][shopProductId]=$productId"
                        itemString += "&$name[$mealIndex][unitType]=$unitTypeName"
                        itemString += "&$name[$mealIndex][amount]=1"
                        itemString += "&$name[$mealIndex][isPack]=1"
                        itemString += "&$name[$mealIndex][comment]=${
                            comment?.split("_")?.get(mealIndex) ?: ""
                        }"
                        itemString += mealString
                    }
                }
                itemString
            }
            toppings.isNullOrEmpty() -> {
                var itemsString = ""
                itemsString += "$name[$maxIndex][shopProductId]=$productId"
                itemsString += "&$name[$maxIndex][unitType]=$unitTypeName"
                itemsString += "&$name[$maxIndex][amount]=$amount"
                itemsString += "&$name[$maxIndex][comment]=${comment ?: ""}"
                itemsString
            }
            else -> {
                var itemsString = ""
                toppings.forEachIndexed { toppingIndex, topping ->
                    maxIndex += 1
                    var productToppings = topping.mapIndexed { i, id ->
                        when {
                            id.second.first() > 0 -> "$name[$maxIndex][shopToppingIds][$i]=${id.first}&$name[$maxIndex][shopToppingPositions][$i]=${id.second.toByteString()}"
                            else -> "$name[$maxIndex][shopToppingIds][$i]=${id.first}"
                        }
                    }.filter { it.isNotEmpty() }.joinToString("&").let { "&$it" }

                    if (toppingIndex < toppings.size - 1 && productToppings.length > 1)
                        productToppings += "&"

                    itemsString += "$name[$maxIndex][shopProductId]=$productId"
                    itemsString += "&$name[$maxIndex][unitType]=$unitTypeName"
                    itemsString += if (options?.get(toppingIndex)
                            ?.firstOrNull { it.second.first() == -1 } != null && product?.shopProductOptions?.isNotEmpty() == true
                    )
                        "&$name[$maxIndex][shopProductOptionId]=${options[toppingIndex].first().first}"
                    else
                        ""
                    itemsString += "&$name[$maxIndex][amount]=1"
                    itemsString += "&$name[$maxIndex][comment]=${comment ?: ""}"
                    itemsString += productToppings
                }
                itemsString
            }
        }
    }

    private fun generateToppingWithPositionString(
        name: String,
        index: Int,
        pair: Pair<Int, List<Int>>,
        onlyPaid: Int
    ): String {
        var topping = ""
        topping += "$name[$maxIndex][shopToppingIds][$index]=${pair.first}"
        topping += "&$name[$maxIndex][shopToppingIsPaid][$index]=${onlyPaid}"
        if ((pair.second.firstOrNull() ?: 0) > 0) {
            topping += "&$name[$maxIndex][shopToppingPositions][$index]=${pair.second.toByteString()}"
        }
        return topping
    }

    suspend fun search(
        text: String,
        categoryId: String? = null,
        shopId: Int? = Remote.shopId
    ): List<ShopProduct>? {
        return action(
            name = "search-products",
            arguments = mutableMapOf(
                "shopId"        to shopId,
                "productName"   to text,
                "categoryId"    to categoryId
            ),
            loader = categoryId == null,
            parser = { getList<JSONObject, ShopProduct>("products") { parseProduct() } }
        )?.parse()
    }

    suspend fun getHistory(): List<HistoryOrder>? = suspendCatch {
        action(
            name = "get-order-history",
            arguments = mapOf(
                "shopId" to shopId,
                "userId" to database.user?.id
            ),
            headers = authToken,
            parser = { getList<JSONObject, HistoryOrder>("orders") { parseHistoryOrder() } }
        )?.parse()
    }

    @SuppressLint("DefaultLocale")
    @Throws(JSONException::class)
    private fun JSONObject.parseShops(): Network = Network(
        welcomeMessage = catch(true) { string("welcomeMessage") },
        shops = catch(true) { getList<JSONObject, ShortShop>("shops") { parseShop() } },
        tags = catch(true) { getList<JSONObject, Tag>("tags") { parseTags() } }
    ).also {
        (foregroundApplication as? App)?.accessibilityLink =
            catch(true) { string("accessabilityLink") }
    }

    @Suppress("UNCHECKED_CAST")
    private fun JSONObject.parseShop(): ShortShop =
        ShortShop(
            getInt("id"),
            getString("name"),
            getString("address"),
            getString("image"),
            getTypedList<Int>("shopTags") as? List<Int> ?: listOf(),
            catch(true) { getInt("deliveryTimeMinutesFrom") },
            catch(true) { getInt("deliveryTimeMinutesTo") },
            catch(true) { getInt("isMakingDelivery") == 1 } ?: false,
            catch(true) { getDouble("shopLat") },
            catch(true) { getDouble("shopLon") }
        )

    private fun JSONObject.parseTags(): Tag =
        Tag(
            getInt("id"),
            getString("name")
        )

    @SuppressLint("DefaultLocale")
    @Throws(JSONException::class)
    fun JSONObject.parseShopJson(): Shop = let { data ->
        getJSONObject("shop").run {
            Shop(
                id = getInt("id"),
                code = getString("code"),
                name = getString("name"),
                image = string("image"),
                description = string("description"),
                about = string("about"),
                address = string("address"),
                phone = string("phone"),
                extraPhone = string("phone2"),
                paymentEndpoint = getString("paymentEndpoint"),
                paymentKey = getString("paymentKey"),
                categories =
                getList<JSONObject, Category>("categories") { parseCategory() },
                subCategories = getList<JSONObject, Category>("subcategories") { parseCategory() },
                paymentTypes =
                getList<String, PaymentType>("paymentTypes") { PaymentType.valueOf(capitalize()) },
                orderTypes =
                getList<String, OrderType>("orderTypes") { OrderType.valueOf(capitalize()) },
                mainColor = getString("mainColor"),
                withSound = getInt("withSound"),
                withCoupons = getInt("withCoupons"),
                backgroundName = string("backgroundCodename") ?: "background1",
                minimumOrder = getDouble("minimalOrder"),
                deliveryRadius = getDouble("deliveryRadius").toFloat(),
                deliveryCost = getDouble("deliveryCost"),
                deliveryTimes = getList<JSONObject, Time>("deliveryTimes") { parseTime() },
                pickupTimes = getList<JSONObject, Time>("pickupTimes") { parseTime() },
                workingTimes = getList<JSONObject, Time>("workingTimes") { parseTime() },
                layout = getInt("layout"),
                paymentDescription = (data.optString("paymentDescription") ?: null),
                info = mapOf(
                    InfoFragment.Type.AboutApp.key
                        .let { it to data.optString(it) },
                    InfoFragment.Type.Regulations.key
                        .let { it to data.optString(it) },
                    InfoFragment.Type.Returns.key
                        .let { it to data.optString(it) },
                    InfoFragment.Type.Privacy.key
                        .let { it to data.optString(it) }
                ),
                withoutFutureDelivery = getInt("withoutFuture_delivery") == 1,
                withoutFuturePickup = getInt("withoutFuture_pickup") == 1,
                deliveryZones = getList<JSONObject, DeliveryZone>("deliveryZones") { parseZones() },
                directPayment = getInt("directPayment"),
                branches = getList<JSONObject, ShopBranch>("branches") { parseBranches() },

                maxPayments = catch(true) { getInt("maxPayments") } ?: 1,
                isAreaDelivery = catch(true) { getInt("isAreaDelivery") == 1 } ?: false,
                shopLat = catch(true) { getDouble("shopLat") },
                shopLon = catch(true) { getDouble("shopLon") },
//                linkFacebook =  catch(true) { string("linkFacebook")?.takeIf { it.isNotBlank() } },
//                phoneWhatsapp =  catch(true) { string("phoneWhatsapp")?.takeIf { it.isNotBlank() } },
            )
        }
    }

    private fun JSONObject.parseZones(): DeliveryZone = DeliveryZone(
        from = getDouble("from"),
        to = getDouble("to"),
        deliveryCost = getDouble("deliveryCost")
    )

    private fun JSONObject.parseBranches(): ShopBranch = ShopBranch(
        id = getInt("id"),
        name = getString("name")
    )

    @Throws(JSONException::class)
    private fun JSONObject.parseTime() = Time(
        id = getInt("id"),
        day = getString("weekday"),
        from = getString("from"),
        to = getString("to"),
        date = optLong("date")
    )

    @Throws(JSONException::class)
    fun JSONObject.parseCategory() = Category(
        id = optInt("id"),
        name = getString("codename"),
        parentId = catch(true) { getInt("parent_id") },
        displayName = getString("name"),
        iconUrl = getString("icon")
    )

    @Throws(JSONException::class)
    fun JSONObject.parseUserJson() = User(
        id = getInt("id"),
        authToken = getString("token"),
        name = getString("name"),
        phone = getString("phone"),
        email = getString("email"),
        shopId = getInt("shopId"),
        streetName = getString("streetName"),
        streetNumber = getString("streetNumber"),
        floorNumber = string("floorNumber"),
        entranceCode = string("entranceCode"),
        apartmentNumber = optString("apartmentNumber").toIntOrNull(),
        city = getString("city")
    )

    @Throws(JSONException::class)
    fun JSONObject.parseProductPage(category: String) =
        ProductPage(
            page = getInt("page"),
            pageSize = getInt("pageSize"),
            pages = getInt("pagesCount"),
            products = getJSONObject("categoryProducts")
                .getList<JSONObject, ShopProduct>(category) { parseProduct() }
        )

    @Throws(JSONException::class)
    private fun JSONObject.parseProduct() =
        ShopProduct(
            id = getInt("id"),
            unitTypes = getList<JSONObject, UnitType>("unitTypes") { parseUnitType() },
            defaultUnitType = getString("defaultUnitType"),
            isNew = getBoolean("isNew"),
            isOutOfStock = getBoolean("isStock"),
            isSeason = getBoolean("isSeason"),
            isSale = getBoolean("isSale"),
            product = getJSONObject("product").parseBaseProduct(),
            toppings = getList<JSONObject, ShopTopping>("shopToppings") { parseShopTopping() },
            shopProductOptions = catch { getList<JSONObject, ShopTopping>("shopProductOptions") { parseShopTopping() } }
        )

    @Throws(JSONException::class)
    private fun JSONObject.parseUnitType() = UnitType(
        type = getString("type"),
        price = getDouble("price"),
        multiplier = getDouble("multiplier").toFloat()
    )

    @Throws(JSONException::class)
    private fun JSONObject.parseBaseProduct() =
        BaseProduct(
            id = getInt("id"),
            name = getString("name"),
            maxToppings = try {
                getInt("maxToppings")
            } catch (t: Throwable) {
                null
            },
            image = string("image"),
            imageBig = string("imageBig"),
            category = getString("category"),
            productType = catch {
                if (this.getString("productType").toUpperCase(Locale.ROOT) == "NULL") null
                else ProductType.valueOf(this.getString("productType").toUpperCase(Locale.ROOT))
            },
            description = string("description"),
            toppingsDescription = catch { getString("toppingsDescription").takeIf { it != "null" } },
            toppings = getList<JSONObject, BaseTopping>("toppings") { parseBaseTopping() },
            optionsDescription = catch { getString("optionsDescription").takeIf { it != "null" } },
            levels = catch { getList<JSONObject, Level>("levels") { parseLevel() } },
            subCategory = string("subCategory")
        )

    @Throws(JSONException::class)
    fun JSONObject.parseLevel(): Level {
        val levelJson = this
        return Level(
            id = levelJson.getInt("id"),
            description = levelJson.getString("description"),
            productsAmount = levelJson.getInt("productsAmount"),
            toppingsFree = levelJson.getInt("toppingsFree"),
            optionsPaid = levelJson.getInt("optionsPaid"),
            toppingsAddPaid = levelJson.getInt("toppingsAddPaid"),
            products = levelJson.getList<JSONObject, ShopProduct>("products") { parseProduct() },
        )
    }

    @Throws(JSONException::class)
    fun JSONObject.parseCartProducts() =
        getList<JSONObject, ShopProduct>("cartProducts") { parseProduct() }

    @Throws(JSONException::class)
    fun JSONObject.parseAllProducts(): Map<String, List<ShopProduct>> {
        val byCategory = getJSONObject("categoryProducts").toMap().mapValues { jsonArray ->
            (jsonArray.value as? JSONArray)?.iterator()?.asSequence()
                ?.mapNotNull { it.parseProduct() }?.toList() ?: emptyList()
        }
        return mapOf("cart" to getList<JSONObject, ShopProduct>("cartProducts") { parseProduct() }) + byCategory
    }

    @SuppressLint("DefaultLocale")
    fun JSONObject.parseHistoryOrder(): HistoryOrder = run {
        val deliveryDate = getLong("deliveryDate") * 1_000
        HistoryOrder(
            id = getInt("id"),
            userId = getInt("userId"),
            shopId = getInt("shopId"),
            time = Time(
                id = -1,
                day = getWeekDay(deliveryDate) ?: "",
                from = getString("deliveryFrom"),
                to = getString("deliveryTo"),
                date = deliveryDate
            ),
            created = getLong("createdDate") * 1_000,
            comment = string("comment"),
            deliveryComment = string("deliveryComment"),
            type = enumValue<OrderType>(getString("orderType")),
            paymentType = enumValue<PaymentType>(getString("paymentType")),
            status = getString("status"),
            products = getList<JSONObject, HistoryProduct>("products") { parseHistoryProduct() },
            discount = getInt("discount"),
            deliveryCost = getDouble("deliveryCost"),
            link = getString("link"),
            totalNew = if (optDouble("totalNew").isNaN()) null else optDouble("totalNew"),
            totalPay = double("totalPay")
        )
    }

    @Throws(JSONException::class)
    fun JSONObject.parseHistoryProduct(): HistoryProduct = run {
        val product = getJSONObject("shopProduct").parseProduct()
        HistoryProduct(
            product = product,
            category = product.product.category,
            unitTypeName = getString("unitType"),
            amount = getDouble("amount").toFloat(),
            price = double("price"),
            total = double("total"),
            comment = string("comment"),
            toppings = getList<JSONObject, HistoryTopping>("toppings") { parseHistoryTopping() },
            productOption = try {
                getJSONObject("productOption").parseOption()
            } catch (t: Throwable) {
                null
            },
            link = null
        )
    }

    @Throws(JSONException::class)
    private fun JSONObject.parseOption(): ProductOption =
        ProductOption(
            price = getDouble("price"),
            name = getJSONObject("shopProductOption").getJSONObject("productOption")
                .getString("name"),
            id = getJSONObject("shopProductOption").getInt("id")
        )

    @Throws(JSONException::class)
    private fun JSONObject.parseHistoryTopping() =
        HistoryTopping(
            price = getDouble("price"),
            total = optDouble("total"),
            topping = getJSONObject("shopTopping").parseShopTopping(),
            positions = try {
                getString("positions")
            } catch (t: Throwable) {
                ""
            }
        )

    @Throws(JSONException::class)
    private fun JSONObject.parseShopTopping() =
        ShopTopping(
            id = getInt("id"),
            price = getDouble("price"),
            topping = try {
                getJSONObject("topping").parseBaseTopping()
            } catch (t: Throwable) {
                getJSONObject("productOption").parseBaseTopping()
            }
        )

    @Throws(JSONException::class)
    private fun JSONObject.parseBaseTopping() =
        BaseTopping(
            name = getString("name"),
            codename = try {
                getString("codename")
            } catch (t: Throwable) {
                null
            }
        )

    private fun JSONObject.double(name: String): Double? {
        val value = optDouble(name)
        return if (value.isNaN()) null else value
    }

}