package il.co.superclick.remote

import il.co.superclick.data.CreditCard
import il.co.superclick.infrastructure.Locator
import il.co.superclick.remote.Remote.throwIfNoNetwork
import org.json.JSONObject

object Tranzila {

    private val client = Http.instance
    private const val url = "https://secure5.tranzila.com/cgi-bin/tranzila71u.cgi"
    private val tkname get() = Locator.database.shop?.paymentEndpoint
    private val tkpass get() = Locator.database.shop?.paymentKey
    //const val tkname = BuildConfig.TKNAME
    //const val tkpass = BuildConfig.TKPASS

    data class Result(
        val status: CreditCard.Status?,
        val index: Int,
        val confirmCode: String,
        val raw: JSONObject
    )

    suspend fun createToken(cardNumber: String): String? {
        throwIfNoNetwork()
        val result = client.post(
            url,
            mapOf(
                "response_return_format" to "json",
                "supplier" to (tkname ?: return null),
                "TranzilaPW" to (tkpass ?: return null),
                "TranzilaTK" to 1,
                "ccno" to cardNumber
            )
        ).await().getOrNull()
        return result?.let(::JSONObject)?.optString("TranzilaTK")
    }

    suspend fun j5Transaction(
        creditCard: CreditCard,
        cvv: String,
        sum: Double,
        email: String? = null
    ): Result? {
        return j5TokenTransaction(
            creditCard.token ?: return null,
            cvv,
            creditCard.expDate,
            creditCard.holderId,
            sum,
            email
        )?.let { response ->
            Result(
                status =
                CreditCard.Status.find(response.optString("Response"))
                    ?: CreditCard.Status.Other,
                index = response.optInt("index"),
                confirmCode = response.optString("ConfirmationCode"),
                raw = response
            )
        }
    }

    suspend fun j5TokenTransaction(
        token: String,
        cvv: String,
        expDate: String,
        holderId: String,
        sum: Double,
        email: String? = null
    ): JSONObject? {
        throwIfNoNetwork()
        val args = mutableMapOf(
            "response_return_format" to "json",
            "supplier" to (tkname ?: return null),
            "TranzilaPW" to (tkpass ?: return null),
            "TranzilaTK" to token,
            "sum" to sum,
            "currency" to 1,
            "cred_type" to 1,
            "tranmode" to "V",
            "expdate" to expDate,
            "mycvv" to cvv,
            "myid" to holderId
        )
        email?.let { args["email"] = email }
        val result = client.post(url, args).await().getOrNull()
        return result?.let(::JSONObject)
    }


}