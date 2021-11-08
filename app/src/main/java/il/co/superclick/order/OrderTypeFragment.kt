package il.co.superclick.order

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.*
import il.co.superclick.infrastructure.Locator
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.widgets.PriceView
import il.co.superclick.utilities.disable
import il.co.superclick.utilities.enable
import il.co.superclick.widgets.CustomDatePickerDialog
import com.dm6801.framework.utilities.delay
import com.dm6801.framework.utilities.enumValue
import com.dm6801.framework.utilities.main
import il.co.superclick.data.*
import il.co.superclick.data.Database.shop
import il.co.superclick.data.ShopBranch.Companion.BRANCH_KEY
import il.co.superclick.dialogs.MinimumForOrderAlertDialog
import il.co.superclick.utilities.formatPrice
import il.co.superclick.utilities.mainColor
import kotlinx.android.synthetic.main.fragment_order_type.*
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

class OrderTypeFragment : BaseFragment() {

    companion object : Comp() {
        private const val CHECKED_BACKGROUND = R.drawable.order_type_checkbox_label
        private const val UNCHECKED_BACKGROUND = R.drawable.order_type_checkbox_label_unchecked
        private val CHECKED_TEXT by lazy { getColor(R.color.white) ?: Color.WHITE }
        private val UNCHECKED_TEXT by lazy { getColor(R.color.text_color) ?: Color.BLACK }
        private val database get() = Locator.database
        private val user get() = database.user
        private val cart get() = database.cart
        private val orderTypes get() = database.shop?.orderTypes ?: emptyList()
        private val dateFormatter = SimpleDateFormat("dd.MM.yy", Locale.ROOT)

        fun open(branch:ShopBranch? = null){
            open(BRANCH_KEY to branch)
        }
    }

    override val layout = R.layout.fragment_order_type
    override val themeBackground: Drawable? = getDrawable(R.drawable.bg_pay)
    private val orderTypeSubtitle: TextView? get() = order_type_subtitle
    private val orderSumContainer: ConstraintLayout? get() = order_type_sum_view
    private val sumView: PriceView? get() = order_type_sum
    private val commentEdit: EditText? get() = order_type_comment
    private val deliveryCommentEdit: EditText? get() = order_type_delivery_comment
    private val datePicker: TextView? get() = order_type_date_picker
    private val editImage: ImageView? get() = edit_image
    private val timePicker: DaysOfWeekPicker? get() = order_type_time_picker
    private val deliveryCheckBox: CheckBox? get() = order_type_delivery
    private val deliveryLabel: TextView? get() = order_type_delivery_label
    private val pickupCheckBox: CheckBox? get() = order_type_pickup
    private val pickupLabel: TextView? get() = order_type_pickup_label
    private val deliveryPriceLabel: TextView? get() = order_type_delivery_price_label
    private val submit: Button? get() = order_type_submit
    private val sum get() = (cart.liveSum.value ?: 0.0)
    private var branch: ShopBranch? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        sumView?.set(sum)
        orderTypeSubtitle?.text = shop?.paymentDescription ?: ""
        deliveryCheckBox?.buttonTintList = mainColor
        pickupCheckBox?.buttonTintList = mainColor
        orderSumContainer?.backgroundTintList = mainColor
        submit?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(1.dpToPx, mainColor)
            cornerRadius = 16.dpToPx.toFloat()
        }
        submit?.setTextColor(mainColor)
        initComment()
        initDeliveryComment()
        initDatePicker()
        initCheckboxes()
        initSubmit()
        setStartDate()
        toggleDeliveryPrice()
    }

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[BRANCH_KEY] as? ShopBranch)?.let { branch = it }
    }

    private fun toggleDeliveryPrice() {
        if (shop?.deliveryZones?.isEmpty() == true) {
            deliveryPriceLabel?.text = getString(
                R.string.delivery_price,
                (shop?.deliveryCost ?: 0.0).formatPrice()
            )
            deliveryPriceLabel?.isInvisible = shop?.deliveryCost == 0.0 || shop?.isAreaDelivery == true
        }
    }

    private fun setStartDate() {
        var dates: List<Time>? = null
        var type: OrderType? = null
        when {
            deliveryCheckBox?.isChecked == true -> {
                dates = database.shop?.deliveryTimes
                type = OrderType.Delivery
            }
            pickupCheckBox?.isChecked == true -> {
                dates = database.shop?.pickupTimes
                type = OrderType.Pickup
            }
        }
        if (!dates.isNullOrEmpty() && type != null) {
            val date = Date(dates.first().date?.times(1000) ?: return)
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                .apply {
                    time = date
                    set(Calendar.HOUR, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            selectedDate = date
            delay(300, Dispatchers.Main) {
                updateDateText(date)
                updateTimePicker(type, calendar, dates)
            }
        }

    }

    private fun setToolbar() {
        menuBar?.toggleCartButton()
        menuBar?.toggleSearch(true)
    }

    private fun initComment() {
        commentEdit?.filterEmoticons()
        user?.orderComment?.let { commentEdit?.setText(it) }
    }

    private fun initDeliveryComment() {
        deliveryCommentEdit?.filterEmoticons()
        user?.deliveryComment?.let { deliveryCommentEdit?.setText(it) }
    }

    private var selectedDate: Date? = null

    private fun initDatePicker() {
        datePicker?.onClick {
            val dates: List<Time>?
            val type: OrderType
            when {
                deliveryCheckBox?.isChecked == true -> {
                    dates = database.shop?.deliveryTimes
                    type = OrderType.Delivery
                }
                pickupCheckBox?.isChecked == true -> {
                    dates = database.shop?.pickupTimes
                    type = OrderType.Pickup
                }
                else -> return@onClick
            }
            if (dates.isNullOrEmpty()) return@onClick
            CustomDatePickerDialog.show(
                beforeShow = {
                    selectableDays = dates.let(::filterDatePickerDates).toTypedArray()
                },
                onDateSet = { _, year, monthOfYear, dayOfMonth ->
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        .apply {
                            set(year, monthOfYear, dayOfMonth, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    selectedDate = calendar.time
                    delay(300, Dispatchers.Main) {
                        updateDateText(calendar.time)
                        updateTimePicker(type, calendar, dates)
                    }
                })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateDateText(date: Date?) {
        datePicker?.text = getString(R.string.date) + (date?.let(dateFormatter::format)?.let { " $it" } ?: "")
    }

    private fun clearDate() {
        updateDateText(null)
        selectedDate = null
    }

    private fun updateTimePicker(type: OrderType, selected: Calendar, dates: List<Time>) {
        val date1 = (selected.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val date2 = (selected.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 0)
        }.time
        timePicker?.set(
            type,
            dates.filter {
                it.date?.div(60 * 60 * 24) == date1.time / 1_000 / 60 / 60 / 24 ||
                        it.date?.times(60 * 60 * 24) == date2.time / 1_000 / 60 / 60 / 24
            }, date = selected.time, onSelected = { toggleSubmit() })
        timePicker?.setFirstSelected()
        toggleSubmit()
    }

    private fun filterDatePickerDates(list: List<Time>): List<Calendar> {
        return list.mapNotNull { serverTime ->
            serverTime.date?.let {
                Calendar.getInstance().apply { timeInMillis = it * 1_000 }
            }
        }.distinctBy { it.timeInMillis }
    }

    @SuppressLint("DefaultLocale", "ClickableViewAccessibility")
    private fun initCheckboxes() {
        val isDeliveryEnabled =
            orderTypes.contains(OrderType.Delivery) && !database.shop?.deliveryTimes.isNullOrEmpty()
        val isPickupEnabled =
            orderTypes.contains(OrderType.Pickup) && !database.shop?.pickupTimes.isNullOrEmpty()
        deliveryCheckBox?.isInvisible = !isDeliveryEnabled
        deliveryLabel?.isInvisible = !isDeliveryEnabled
        pickupCheckBox?.isInvisible = !isPickupEnabled
        pickupLabel?.isInvisible = !isPickupEnabled

        if (!isDeliveryEnabled && !isPickupEnabled) return

        if (isDeliveryEnabled) {
            deliveryCheckBox?.setOnTouchListener { _, event ->
                return@setOnTouchListener event.action == MotionEvent.ACTION_DOWN && deliveryCheckBox?.isChecked == true
            }
            setListeners(deliveryCheckBox, deliveryLabel, OrderType.Delivery, ::onDelivery)
        } else {
            main {
                kotlinx.coroutines.delay(400)
                pickupLabel?.performClick()
            }
            deliveryCheckBox?.disable(0f)
            deliveryLabel?.onClick { }
        }

        if (isPickupEnabled) {
            pickupCheckBox?.setOnTouchListener { _, event ->
                return@setOnTouchListener event.action == MotionEvent.ACTION_DOWN && pickupCheckBox?.isChecked == true
            }
            setListeners(pickupCheckBox, pickupLabel, OrderType.Pickup, ::onPickup)
        } else {
            main {
                kotlinx.coroutines.delay(400)
                deliveryLabel?.performClick()
            }
            pickupCheckBox?.disable(0f)
            pickupLabel?.onClick { }
        }


        user?.preferredOrderType?.let { orderTypeString ->
            enumValue<OrderType>(orderTypeString)?.let { orderType ->
                when (orderType) {
                    OrderType.Delivery -> deliveryCheckBox?.performClick()
                    OrderType.Pickup -> pickupCheckBox?.performClick()
                }
            } ?: run { deliveryCheckBox?.performClick() }
        } ?: run { deliveryCheckBox?.performClick() }
    }

    private fun setListeners(
        checkbox: CheckBox?,
        textView: TextView?,
        type: OrderType,
        action: () -> Unit
    ) {
        checkbox?.onClick {
            if (it.isChecked)
                timePicker?.set(type, emptyList(), date = null, onSelected = { toggleSubmit() })
            delay(300, Dispatchers.Main) {
                setStartDate()
            }
            checkFutureAvailable(checkbox)
            action()
        }
        textView?.onClick {
            if (checkbox?.isChecked != true) {
                timePicker?.set(type, emptyList(), date = null, onSelected = { toggleSubmit() })
                clearDate()
            }
            delay(300, Dispatchers.Main) {
                setStartDate()
            }
            checkFutureAvailable(textView)
            action()
        }
    }

    private fun checkFutureAvailable(view: View) {
        val needToShowViews = if (view == deliveryCheckBox || view == deliveryLabel)
            database.shop?.withoutFutureDelivery == false
        else
            database.shop?.withoutFuturePickup == false

        datePicker?.isVisible = needToShowViews
        timePicker?.isVisible = needToShowViews
        editImage?.isVisible = datePicker?.isVisible == true
    }

    private fun onDelivery() {
        check(deliveryCheckBox, deliveryLabel)
        uncheck(pickupCheckBox, pickupLabel)
        if (database.shop?.deliveryZones?.isEmpty() == true) {
            sumView?.set(sum + (if (database.shop?.isAreaDelivery == false) database.shop?.deliveryCost ?: 0.0 else 0.0))
        }
        deliveryCommentEdit?.isVisible = true
        toggleSubmit()
    }

    private fun onPickup() {
        check(pickupCheckBox, pickupLabel)
        uncheck(deliveryCheckBox, deliveryLabel)
        sumView?.set(sum)
        deliveryCommentEdit?.isVisible = false
        toggleSubmit()
    }

    private fun check(checkbox: CheckBox?, textView: TextView?) {
        checkbox?.takeIf { !it.isChecked }?.let { it.isChecked = true }
        textView?.setTextColor(CHECKED_TEXT)
        textView?.setBackgroundResource(CHECKED_BACKGROUND)
        textView?.backgroundTintList = mainColor
    }

    private fun uncheck(checkbox: CheckBox?, textView: TextView?) {
        checkbox?.isChecked = false
        textView?.setTextColor(UNCHECKED_TEXT)
        textView?.setBackgroundResource(UNCHECKED_BACKGROUND)
        textView?.backgroundTintList = null
    }

    private fun initSubmit() {
        submit?.disable()
        submit?.onClick {
            val orderType = timePicker?.type ?: return@onClick
            if (orderType == OrderType.Delivery)
                checkMinPrice(orderType)
            else
                route(orderType)
        }
    }

    private fun checkMinPrice(orderType: OrderType) {
        database.shop?.run {
            cart.liveSum.value?.let { sum ->
                if (sum < minimumOrder)
                    MinimumForOrderAlertDialog.open(minimumOrder)
                else
                    route(orderType)
            }
        }
    }

    private fun route(orderType: OrderType) {
        OrderDetailsFragment.open(
            NewOrder(
                products = cart.products.values.filter { if (it.meals == null) it.isChecked else it.meals?.firstOrNull { meal -> meal.first } != null }.toList(),
                userId = user?.id,
                shopId = database.shopId ?: return,
                type = orderType,
                time = timePicker?.selected,
                comment = commentEdit?.text?.toString(),
                deliveryComment = deliveryCommentEdit?.text?.toString()
            ),
            branch = branch
        )
    }


    private fun isFormValid(): Boolean {
        val isChecked = deliveryCheckBox?.isChecked == true || pickupCheckBox?.isChecked == true
        val isDatePicked = selectedDate != null
        val isTimePicked = timePicker?.selected != null
        return isChecked && isDatePicked && isTimePicked
    }

    private fun toggleSubmit() {
        if (isFormValid()) submit?.enable()
        else submit?.disable()
    }

}