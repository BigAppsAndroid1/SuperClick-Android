package il.co.superclick.remote

import com.dm6801.framework.utilities.catch
import com.dm6801.framework.utilities.main
import com.dm6801.framework.utilities.toast
import il.co.superclick.data.CreditCard
import il.co.superclick.infrastructure.Locator
import il.co.superclick.remote.Remote.throwIfNoNetwork
import org.json.JSONObject

object ZCredit {
    private val client = Http.instance
    private val user = Locator.database.user
    private const val url = "https://pci.zcredit.co.il/ZCreditWS/api/Transaction/"
    private val tkname get() = Locator.database.shop?.paymentEndpoint
    private val tkpass get() = Locator.database.shop?.paymentKey

    data class Result(
        val status: CreditCard.Status?,
        val index: Int,
        val confirmCode: String,
        val raw: JSONObject
    )

    suspend fun validateCard(cardNumber: String, expireDate: String): String? {
        throwIfNoNetwork()
        client.post(
            url+"ValidateCard",
            mapOf(
                "TerminalNumber" to (tkname ?: return null),
                "Password" to (tkpass ?: return null),
                "CardNumber" to cardNumber,
                "ExpDate_MMYY" to expireDate
            )
        ).await().getOrNull()?.let(::JSONObject)?.let {
            if (it.getInt("ReturnCode") == 0)
                return it.optString("Token")
            main{ toast(it.getString("ReturnMessage")) }
            return null
        } ?: return null
    }

    suspend fun j5Transaction(
        creditCard: CreditCard,
        cardNumber: String,
        cvv: String,
        sum: Double,
        payments: Int = 1
    ): Result? {
        throwIfNoNetwork()
        client.post(
            url + "CommitFullTransaction",
            mapOf(
                "TerminalNumber" to tkname.toString(),
                "Password" to tkpass.toString(),
                "CardNumber" to if(creditCard.token.isNotEmpty()) creditCard.token else cardNumber,
                "ExpDate_MMYY" to creditCard.expDate,
                "CVV" to cvv,
                "TransactionSum" to String.format("%.2f", sum),
                "CurrencyType" to 1,
                "J" to 5,
                "HolderID" to creditCard.holderId,
                "CustomerName" to creditCard.holderName,
                "PhoneNumber" to  user?.phone,
                "CustomerEmail" to user?.email,
                "ObeligoAction" to "0",
                "NumberOfPayments" to payments
            )
        ).await().getOrNull()?.let(::JSONObject)?.let {
            catch { Locator.database.user?.update { user?.copy(creditCard = creditCard.apply { token = it.optString("Token") })!! }  }
            return Result(
                status = CreditCard.Status.find(it.optString("ReturnCode").trim())?.apply { msg = it.optString("ReturnMessage") }
                    ?: CreditCard.Status.Other,
                index = it.optInt("ReferenceNumber"),
                confirmCode = it.optString("Token"),
                raw = it
            )
        }
        return null
    }
}
