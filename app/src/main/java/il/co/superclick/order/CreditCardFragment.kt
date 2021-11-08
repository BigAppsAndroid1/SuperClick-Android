package il.co.superclick.order

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.text.isDigitsOnly
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.infrastructure.showProgressBar
import com.dm6801.framework.ui.getColor
import com.dm6801.framework.ui.getDrawable
import com.dm6801.framework.ui.hideKeyboard
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.utilities.*
import com.google.android.gms.maps.model.LatLng
import il.co.superclick.R
import il.co.superclick.data.*
import il.co.superclick.data.Database.shop
import il.co.superclick.data.ShopBranch.Companion.BRANCH_KEY
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator
import il.co.superclick.remote.Remote
import il.co.superclick.remote.ZCredit
import il.co.superclick.utilities.disable
import il.co.superclick.utilities.enable
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.watch
import kotlinx.android.synthetic.main.fragment_credit_card.*
import java.util.*

class CreditCardFragment : BaseFragment() {

    companion object : Comp() {
        private const val KEY_ORDER = "KEY_ORDER"
        private const val KEY_USER_DETAILS = "KEY_USER_DETAILS"
        private const val KEY_LAT_LNG = "KEY_LAT_LNG"

        private val ENABLED_SUBMIT_TINT
                by lazy { mainColor }
        private val DISABLED_SUBMIT_TINT
                by lazy { ColorStateList.valueOf(getColor(R.color.grey) ?: Color.DKGRAY) }

        const val CREDIT_ERROR_TOAST = R.string.credit_card_submit_error_toast

        private val database get() = Locator.database
        private val dbUser get() = database.user
        private val cart get() = database.cart

        fun open(
            order: NewOrder? = null,
            userDetails: User? = null,
            branch: ShopBranch? = null,
            latLng: LatLng? = null
        ) {
            open(
                KEY_ORDER to order,
                KEY_USER_DETAILS to userDetails,
                BRANCH_KEY to branch,
                KEY_LAT_LNG to latLng,
            )
        }
    }

    enum class Mode {
        New, Token
    }

    override val layout = R.layout.fragment_credit_card
    override val themeBackground: Drawable? = getDrawable(R.drawable.bg_credit)
    private val newCardGroup: Group? get() = credit_card_new
    private val nameEdit: EditText? get() = credit_card_name
    private val holderIdEdit: EditText? get() = credit_card_holder_id
    private val numberEdit: EditText? get() = credit_card_number
    private val expMonthSpinner: AppCompatSpinner? get() = credit_card_expire_month
    private val expYearSpinner: AppCompatSpinner? get() = credit_card_expire_year
    private val cvvLabel: TextView? get() = credit_card_token_cvv_label
    private val paymentsLabel: TextView? get() = payments_label
    private val paymentsSpinner: AppCompatSpinner? get() = credit_card_payments_number
    private val cvvEdit: EditText? get() = credit_card_cvv
    private val tokenGroup: Group? get() = credit_card_token
    private val tokenNumber: TextView? get() = credit_card_token_number
    private val tokenCvvEdit: EditText? get() = credit_card_token_cvv
    private val submit: TextView? get() = credit_card_submit
    private val newCardButton: TextView? get() = credit_card_new_card_button
    private var mode: Mode? = null
    private var order: NewOrder? = null
    private var userDetails: User? = null
    private var branch: ShopBranch? = null
    private var userLocatipnFromMap: LatLng? = null
    private val creditCard: CreditCard? get() = userDetails?.creditCard ?: dbUser?.creditCard
    private val cvv: String?
        get() {
            return when (mode) {
                Mode.New -> cvvEdit?.text?.trim()?.toString()
                Mode.Token -> tokenCvvEdit?.text?.trim()?.toString()
                else -> null
            }
        }

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_ORDER] as? NewOrder)?.let { order = it }
        (arguments[KEY_USER_DETAILS] as? User)?.let { userDetails = it }
        (arguments[BRANCH_KEY] as? ShopBranch)?.let { branch = it }
        (arguments[KEY_LAT_LNG] as? LatLng)?.let { userLocatipnFromMap = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MapFragment.isCanAction = true
        hideProgressBar()
        setToolbar()
        when (mode) {
            Mode.Token -> tokenCardMode()
            Mode.New -> newCardMode()
            else -> {
                if (creditCard != null && !creditCard?.token.isNullOrBlank()) tokenCardMode()
                else newCardMode()
            }
        }
        initDateSpinners()
        initPaymentsSpinner()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initPaymentsSpinner() {
        paymentsSpinner?.apply {
            if ((shop?.maxPayments ?: 1) == 1) {
                this.isVisible = false
            }
            shop?.maxPayments?.let { maxPayments ->
                adapter =
                    ArrayAdapter(
                        context ?: foregroundApplication.baseContext,
                        R.layout.payment_item,
                        IntArray(maxPayments) { it + 1 }.map { it.toString() }
                    )
            }
            setSelection(0)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) hideKeyboard()
                false
            }
        }
    }

    private fun setToolbar() {
        menuBar?.toggleCartButton()
        menuBar?.toggleSearch(true)
        menuBar?.toggleButtonsColor()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initDateSpinners() {
        val context = context ?: return
        val calendar = Calendar.getInstance()
        val monthIndex = calendar.get(Calendar.MONTH)
        expMonthSpinner?.setSelection(monthIndex)
        expMonthSpinner?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) hideKeyboard()
            false
        }
        val currentYear = calendar.get(Calendar.YEAR)
        expYearSpinner?.adapter = ArrayAdapter(
            context,
            R.layout.item_days_of_week_picker,
            (currentYear..(currentYear + 8)).map { it }
        )
        expYearSpinner?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) hideKeyboard()
            false
        }
    }

    private fun newCardMode() {
        main {
            userDetails = null
            database.user?.update { copy(creditCard = null) }
            kotlinx.coroutines.delay(200)
            this@CreditCardFragment.mode = Mode.New
            tokenGroup?.hide()
            newCardGroup?.show()
            nameEdit?.watch()
            holderIdEdit?.watch()
            numberEdit?.watch()
            expMonthSpinner?.watch()
            expYearSpinner?.watch()
            cvvEdit?.watch()
            disableSubmit()
            submit?.setOnClickListener(null)
            submit?.setOnClickListener {
                submit?.isClickable = false
                Log("ENTER Submit")
                onSubmit()
            }
            paymentsLabel?.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToTop = -1
                topToBottom = expMonthSpinner?.id ?: return@updateLayoutParams
            }
        }
    }

    private fun onSubmit() {
        val order =
            order?.copy(products = cart.products.values.filter { if (it.meals == null) it.isChecked else it.meals?.firstOrNull { meal -> meal.first } != null }
                .toList()) ?: return
        val user = userDetails ?: dbUser ?: return
        showProgressBar()
        background {
            if (Shop.isDirectPayment == true) {
                val creditCard = fetchCreditToken(user)
                creditCard ?: run {
                    submit?.isClickable = true; hideProgressBar(); return@background
                }
                Remote.makeOrder(
                    order.copy(creditCard = creditCard),
                    "",
                    creditCard.token,
                    creditCard.expDate,
                    creditCard.holderId,
                    cvv = cvv,
                    branchId = branch?.id,
                    latLng = userLocatipnFromMap,
                    paymentsSpinner?.selectedItemPosition?.plus(1)
                )?.let { order ->
                    withMain { onOrderSuccess(order) }
                } ?: let { submit?.isClickable = true }
            } else {
                val transaction = createJ5Transaction(user)
                transaction ?: run {
                    submit?.isClickable = true; hideProgressBar(); return@background
                }
                Remote.makeOrder(
                    order.copy(creditCard = creditCard),
                    transaction["txIndex"] ?: run {
                        submit?.isClickable = true; hideProgressBar(); return@background
                    },
                    transaction["txConfirmCode"] ?: run {
                        submit?.isClickable = true; hideProgressBar(); return@background
                    },
                    transaction["cardExpDate"] ?: run {
                        submit?.isClickable = true; hideProgressBar(); return@background
                    },
                    userDetails?.creditCard?.holderId ?: dbUser?.creditCard?.holderId
                    ?: run {
                        submit?.isClickable = true; hideProgressBar(); return@background
                    },
                    branchId = branch?.id,
                    latLng = userLocatipnFromMap,
                    numberOfPayment = paymentsSpinner?.selectedItemPosition?.plus(1)
                )?.let { order ->
                    withMain { onOrderSuccess(order) }
                } ?: let { submit?.isClickable = true }
            }
        }
    }


    private fun onOrderSuccess(order: HistoryOrder) {
        main {
            menuBar?.toggleCartButton()
            cart.clear()
            database.user?.update {
                copy(
                    hasReadPrivacyPolicy = true,
                    preferredOrderType = order.type?.name,
                    orderComment = order.comment,
                    deliveryComment = order.deliveryComment
                )
            }
            OrderCompleteDialog(order).show()
            hideProgressBar()
        }
    }

    private suspend fun createJ5Transaction(user: User): Map<String, String?>? {
        val creditCard =
            if (user.creditCard?.token?.isNotBlank() == true && user.creditCard.token != "null") user.creditCard
            else {
                val holderName = nameEdit?.text?.trim()?.toString() ?: return null
                val holderId = holderIdEdit?.text?.trim()?.toString() ?: return null
                val creditCard = numberEdit?.text?.trim()?.toString() ?: return null
                val expDate = (getExpireMonth() ?: "") + (getExpireYear() ?: "")
                if (expDate.isBlank() || expDate.length != 4) return null
                CreditCard(
                    holderName,
                    holderId,
                    creditCard.substring(creditCard.length - 4, creditCard.length),
                    expDate,
                    token = ""
                )
            }

        val cvv = cvv ?: return null
        val sum = order?.sum ?: return null
        val cardNumber = numberEdit?.text?.trim()?.toString() ?: return null
        val payments = (paymentsSpinner?.selectedItemPosition ?: 0) + 1
        val transaction = ZCredit.j5Transaction(creditCard, cardNumber, cvv, sum, payments)
        transaction ?: kotlin.run { hideProgressBar(); return null }
        val status = transaction.status
        if (status != CreditCard.Status.Valid /*&& !Dev {}*/) {
            creditErrorToast(status)
            hideProgressBar()
            return null
        }
        return mapOf(
            "txIndex" to transaction.index.toString(),
            "txConfirmCode" to transaction.confirmCode,
            "cardExpDate" to creditCard.expDate
        )
    }

    private fun creditErrorToast(status: CreditCard.Status?) = main {
        hideProgressBar()
        toast(getString(CREDIT_ERROR_TOAST, status?.msg ?: ""))
    }

    private suspend fun fetchCreditToken(user: User): CreditCard? {
        if (user.creditCard?.token?.isNotBlank() == true && user.creditCard.token != "null") return user.creditCard
        val holderName = nameEdit?.text?.trim()?.toString() ?: return null
        val holderId = holderIdEdit?.text?.trim()?.toString() ?: return null
        val creditCard = numberEdit?.text?.trim()?.toString() ?: return null
        val expDate = (getExpireMonth() ?: "") + (getExpireYear() ?: "")
        if (expDate.isBlank() || expDate.length != 4) return null
        return database.createCreditToken(creditCard, holderName, holderId, expDate)
    }

    private fun getExpireMonth(): String? = expMonthSpinner?.selectedItem?.toString()
    private fun getExpireYear(): String? = expYearSpinner?.selectedItem?.toString()?.substring(2..3)


    private fun <T : View> T?.watch() {
        val predicate = { _: T, _: Any? -> true }
        val onChanged = { _: T, _: Any? -> toggleSubmit() }
        val onValid = { _: T -> }
        val onInvalid = { _: T -> }
        watch(onChanged, onValid, onInvalid, predicate)
    }

    private fun toggleSubmit() {
        if (isNewCardFormValid()) enableSubmit()
        else disableSubmit()
    }

    private fun isNewCardFormValid(): Boolean {
        val views =
            newCardGroup?.referencedIds?.map { view?.findViewById<View?>(it) }?.filterNotNull()
        if (views.isNullOrEmpty()) return false
        views.forEach { if (!it.isValid()) return false }
        return true
    }

    private fun View.isValid(): Boolean {
        return when (this) {
            is EditText -> text?.trim()?.isNotBlank() ?: false
            is AppCompatSpinner -> true
            else -> true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun tokenCardMode() {
        database.user = userDetails
        this.mode = Mode.Token
        newCardGroup?.hide()
        newCardGroup?.getViews()?.forEach {
            when (it) {
                is EditText -> it.text.clear()
                is AppCompatSpinner -> it.setSelection(0)
            }
        }
        tokenGroup?.show()
        paymentsLabel?.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topToTop = cvvLabel?.id ?: return
        }
        tokenNumber?.text = "XXXX-XXXX-XXXX-${creditCard?.lastDigits}"
        tokenCvvEdit?.doOnTextChanged { text, _, _, _ ->
            if (text?.isDigitsOnly() == true && text.length >= 3) enableSubmit()
            else disableSubmit()
        }
        newCardButton?.onClick { newCardMode() }
        disableSubmit()
        submit?.setOnClickListener(null)
        submit?.setOnClickListener {
            submit?.isClickable = false
            onSubmit()
        }
    }

    private fun Group.show() {
        getViews().forEach { it.isVisible = true }
    }

    private fun Group.hide() {
        getViews().forEach { it.isGone = true }
    }

    private fun Group.getViews(): List<View> {
        return referencedIds.map { view?.findViewById<View?>(it) }.filterNotNull()
    }

    private fun enableSubmit() {
        submit?.enable()
        submit?.backgroundTintList = ENABLED_SUBMIT_TINT
    }

    private fun disableSubmit() {
        submit?.disable()
        submit?.backgroundTintList = DISABLED_SUBMIT_TINT
    }

}