package il.co.superclick.dialogs

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.utilities.formatPrice
import il.co.superclick.utilities.onClick
import il.co.superclick.utilities.setThemeColor
import kotlinx.android.synthetic.main.dialog_minimum_for_order.*

class MinimumForOrderAlertDialog : BaseDialog() {

    override val layout: Int get() = R.layout.dialog_minimum_for_order
    override val gravity: Int = android.view.Gravity.CENTER
    override val isCancelable: Boolean get() = false
    //override val widthFactor = 0.8f
    //override val heightFactor = 0.4f
    private var minForOrderAmount: Double? = null
    private val title: TextView? get() = dialog_min_title
    private val minForOrderText: TextView? get() = dialog_minimum_for_order_amount
    private val minimumForOrderText: TextView? get() = dialog_min_text
    private val closeButton: ImageView? get() = dialog_product_alert_close
    private val minimumForOrder: TextView? get() = dialog_minimum_for_order_amount

    companion object : Comp<MinimumForOrderAlertDialog>() {
        private const val KEY_MIN_FOR_ORDER = "KEY_MIN_FOR_ORDER"

        fun open(minPriceForOrder: Double) {
            open(KEY_MIN_FOR_ORDER to minPriceForOrder)
        }
    }

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_MIN_FOR_ORDER] as? Double)?.let { this.minForOrderAmount = it }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        minimumForOrderText?.setThemeColor()
        title?.setThemeColor()
        minForOrderText?.text = context.getString(
            R.string.min_price_for_order,
            (minForOrderAmount ?: 0.0).formatPrice()
        )
        closeButton?.onClick { dismiss() }
        minimumForOrder?.onClick { dismiss() }
    }

}