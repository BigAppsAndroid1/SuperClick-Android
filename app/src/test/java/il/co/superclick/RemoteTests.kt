package il.co.superclick

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import il.co.superclick.remote.Remote.parseShopJson
import il.co.superclick.remote.Remote.parseUserJson
import il.co.superclick.data.Database
import il.co.superclick.data.Shop
import il.co.superclick.remote.Remote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

@Suppress("EXPERIMENTAL_API_USAGE")
class RemoteTests {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            Dispatchers.setMain(TestCoroutineDispatcher())
            setShop()
        }

        fun setShop() {
            Database.shop = Shop(
                id = 1,
                code = "",
                name = "",
                image = null,
                description = null,
                address = null,
                phone = null,
                extraPhone = null,
                paymentEndpoint = "",
                paymentKey = "",
                categories = emptyList(),
                paymentTypes = emptyList(),
                orderTypes = emptyList(),
                minimumOrder = 35.0,
                deliveryRadius = 1f,
                deliveryCost = 20.0,
                deliveryTimes = emptyList(),
                workingTimes = emptyList(),
                pickupTimes = emptyList(),
                info = emptyMap(),
                about = null,
                mainColor = "AABBCC",
                backgroundName = "bg_1",
                branches = emptyList(),
                withSound = 1,
                layout = 1,
                paymentDescription = "test",
                withCoupons = 1,
                withoutFutureDelivery = false,
                withoutFuturePickup = false,
                deliveryZones = emptyList(),
                directPayment = 0
            )
        }
    }

    @Test
    fun parseShop() = runTest {
        val shop = JSONObject(getResource("shop.json")).parseShopJson()
        println(shop)
    }

    @Test
    fun parseUser() = runTest {
        val user = JSONObject(getResource("user.json")).parseUserJson()
        println(user)
    }

    @Test
    fun getCategoryProducts() = runTest {
        val page = Remote.getProducts("vegetables")
        println(page)
    }

    @Test
    fun getCartProducts() = runTest {
        val products = Remote.getCartProducts(listOf(1, 2))
        println(products)
    }

}