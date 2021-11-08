package il.co.superclick.order

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import il.co.superclick.infrastructure.Locator
import il.co.superclick.R
import il.co.superclick.data.*
import il.co.superclick.login.SmsDialog
import il.co.superclick.remote.Remote
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.utilities.*
import il.co.superclick.widgets.UserDetailsWidget
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.infrastructure.showProgressBar
import com.dm6801.framework.ui.getColor
import com.dm6801.framework.ui.getDrawable
import com.dm6801.framework.utilities.*
import com.google.android.gms.maps.model.LatLng
import il.co.superclick.data.ShopBranch.Companion.BRANCH_KEY
import il.co.superclick.dialogs.AddressNotInRadiusDialog
import il.co.superclick.fragments.InfoFragment
import kotlinx.android.synthetic.main.fragment_order_details.*

class OrderDetailsFragment : BaseFragment() {

    companion object : Comp() {
        private const val KEY_ORDER = "KEY_ORDER"
        private const val CASH_DELIVERY = R.string.delivery_cash_delivery
        private const val CASH_PICKUP = R.string.delivery_cash_pickup
        private val database get() = Locator.database
        private val user get() = database.user
        private val cart get() = database.cart
        private var canAction: Boolean = true

        fun open(order: NewOrder, branch: ShopBranch? = null) {
            open(KEY_ORDER to order, BRANCH_KEY to branch)
        }
    }

    override val layout = R.layout.fragment_order_details
    override val themeBackground: Drawable? = getDrawable(R.drawable.bg_pay)
    private val shop: Shop? get() = database.shop
    private val userDetails: UserDetailsWidget? get() = user_details
    private val credit: TextView? get() = order_details_credit
    private val cash: TextView? get() = order_details_cash
    private val policyText: TextView? get() = order_details_policy_text
    private val policyCheckBox: CheckBox? get() = order_details_policy_checkbox
    private var branch: ShopBranch? = null
    private var userLocationFromMap: LatLng? = null

    //    private val deliveryPrice: TextView? get() = order_details_delivery_label
    private var order: NewOrder? = null

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_ORDER] as? NewOrder)?.let { order = it }
        (arguments[BRANCH_KEY] as? ShopBranch)?.let { branch = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        credit?.backgroundTintList = mainColor
        cash?.backgroundTintList = mainColor
        initUserDetails()
        initCheckBox()
        initPolicyLink()
        initSubmitButtons()
    }

    override fun onForeground() {
        super.onForeground()
        menuBar?.toggleCartButton()
        menuBar?.toggleSearch(false)
    }

    private fun initUserDetails() {
        if (order?.type == OrderType.Pickup) userDetails?.hideAddressDetails()
        else userDetails?.setIsAddressRequired(true)
        userDetails?.setListener(onChanged = ::toggleSubmitButtons)
    }

    private fun initCheckBox() {
        policyCheckBox?.buttonTintList = mainColor
        policyCheckBox?.isChecked = database.termsAccepted == true
        policyCheckBox?.setOnCheckedChangeListener { _, _ ->
            toggleSubmitButtons()
            database.termsAccepted = true
        }
    }

    private fun initPolicyLink() {
        policyText?.link(
            getString(R.string.reglament) to { InfoFragment.open(InfoFragment.Type.Regulations) },
            getString(R.string.privacy_policy) to { InfoFragment.open(InfoFragment.Type.Privacy) },
            color = getColor(R.color.colorAccent) ?: Color.BLUE
        )
    }

    private fun initSubmitButtons() {
        credit?.disable()
        cash?.disable()
        when (order?.type) {
            OrderType.Delivery -> cash?.setText(CASH_DELIVERY)
            OrderType.Pickup -> cash?.setText(CASH_PICKUP)
        }
        credit?.onClick(5_000) {
            if (shop?.paymentTypes?.contains(PaymentType.Credit) == true)
                onSubmit(PaymentType.Credit)
        }
        cash?.onClick(10_000) {
            if (shop?.paymentTypes?.contains(PaymentType.Cash) == true)
                onSubmit(PaymentType.Cash)
        }
        toggleSubmitButtons()
    }

    private fun onSubmit(type: PaymentType) {
        showProgressBar()
        val userDetails = userDetails?.getUserDetails() ?: return
        val action = fun(type: PaymentType) {
            if (order?.type == OrderType.Delivery) {
                if (shop?.isAreaDelivery == false) {
                    if (shop?.deliveryZones?.isNotEmpty() == true) {
                        if ((order?.distance
                                ?: return) > shop?.deliveryZones?.last()?.to?.times(1000) ?: 0.0
                        ) {
                            AddressNotInRadiusDialog.open(shop?.address ?: return)
                            return
                        }
                    } else if ((order?.distance ?: return) > (shop?.deliveryRadius?.times(1000)
                            ?: 0f)
                    ) {
                        AddressNotInRadiusDialog.open(shop?.address ?: return)
                        return
                    }
                    if (order?.type == OrderType.Delivery)
                        DeliveryPriceDialog.open(order?.deliveryCost ?: return) {
                            makeOrder(userDetails, type)
                        }
                    else
                        makeOrder(userDetails, type)
                } else {
                    if (order?.type == OrderType.Delivery)
                        if (order?.deliveryCost == null || order!!.deliveryCost == -1.0)
                            AddressNotInRadiusDialog.open(shop?.address ?: return)
                        else
                            DeliveryPriceDialog.open(order?.deliveryCost ?: return) { makeOrder(userDetails, type) }
                    else
                        makeOrder(userDetails, type)
                }
            } else
                makeOrder(userDetails, type)
        }

        val upsertAndAct = fun() {
            background {
                suspendCatch {
                    val updateUserResponse = database.upsertUser(userDetails)
                    withMain {
                        if (updateUserResponse?.rawData != null) {
                            action(type)
                        } else SmsDialog.open(SmsDialog.Step.Sms, userDetails.phone) {
                            MapFragment.isCanAction = true
                            action(type)
                        }
                    }
                }
            }
        }
        if (order?.type == OrderType.Pickup)
            upsertAndAct()
        else
            MapFragment.open("${userDetails.city}, ${userDetails.streetName} ${userDetails.streetNumber}") { distance, latLng ->
                canAction = true
                order?.distance = distance.toInt()
                userLocationFromMap = latLng
                if (shop?.isAreaDelivery == true)
                    background {
                        order?.deliveryCost = Remote.getDeliveryPrice(latLng)
                        main {
                            upsertAndAct()
                        }
                    }
                else {
                    order?.deliveryCost = Shop.getCompleteDeliveryCost(distance)
                    upsertAndAct()
                }
            }
    }

    private fun makeOrder(userDetails: User, type: PaymentType) {
        when (type) {
            PaymentType.Cash -> makeCashOrder()
            PaymentType.Credit -> makeCreditOrder(userDetails)
        }
    }

    private fun makeCreditOrder(userDetails: User) {
        val order = order?.copy(
            paymentType = PaymentType.Credit,
            userId = user?.id ?: return
        ) ?: return
        this.order = order
        CreditCardFragment.open(
            order = order,
            userDetails = userDetails,
            branch = branch,
            latLng = userLocationFromMap
        )
    }

    private fun makeCashOrder() {
        if (!canAction) return
        canAction = false
        val order = order?.copy(
            paymentType = PaymentType.Cash,
            userId = user?.id ?: run { canAction = true; return }
        ) ?: run { canAction = true; return }
        this.order = order
        background {
            suspendCatch {
                Remote.makeOrder(order, branchId = branch?.id, latLng = userLocationFromMap)?.let {
                    withMain { onOrderSuccess(it) }
                } ?: run { canAction = true; hideProgressBar() }
            }
        }
    }

    private fun onOrderSuccess(order: HistoryOrder) {
        background {
            cart.clear()
            database.user?.update {
                copy(
                    hasReadPrivacyPolicy = true,
                    preferredOrderType = order.type?.name,
                    orderComment = order.comment,
                    deliveryComment = order.deliveryComment
                )
            }
        }
        canAction = true
        OrderCompleteDialog(order).show()
    }

    private fun toggleSubmitButtons() {
        if (isFormValid()) {
            credit?.enable()
            cash?.enable()
        } else {
            credit?.disable()
            cash?.disable()
        }
    }

    private fun isFormValid(): Boolean {
        return userDetails?.isFormValid() == true
                && policyCheckBox?.isChecked == true
    }

}