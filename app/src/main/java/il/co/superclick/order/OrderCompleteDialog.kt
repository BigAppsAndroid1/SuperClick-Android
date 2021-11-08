package il.co.superclick.order

import android.annotation.SuppressLint
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.ui.onClick
import il.co.superclick.MainActivity
import il.co.superclick.R
import il.co.superclick.data.Database
import il.co.superclick.data.HistoryOrder
import il.co.superclick.data.PaymentType
import il.co.superclick.data.Shop
import il.co.superclick.history.HistoryListFragment
import il.co.superclick.history.HistoryOrderFragment
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.infrastructure.Locator.database
import il.co.superclick.network.NetworkFragment
import il.co.superclick.product_list.ProductListFragment
import il.co.superclick.utilities.getString
import il.co.superclick.utilities.mainColor
import kotlinx.android.synthetic.main.dialog_order_complete.*

class OrderCompleteDialog(order: HistoryOrder) : BaseDialog() {

    override val layout: Int get() = R.layout.dialog_order_complete
    override val heightFactor: Float? get() = 0.44f
    override val widthFactor: Float? get() = 0.8f
    override val isBackgroundDim: Boolean get() = true
    override val closeWithActivity: Boolean get() = false
    override val isCancelable: Boolean get() = false
    private val backButton: ImageView? get() = order_received_close
    private val button: Button? get() = order_complete_button
    private val title: TextView? get() = dialog_order_received_title
    private val text: TextView? get() = order_received_middle_text
    private var order: HistoryOrder? = null

    init {
        this.order = order
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        initButton()
        hideProgressBar()
        initText(order?.paymentType ?: return)
    }

    @SuppressLint("SetTextI18n")
    private fun initText(paymentType: PaymentType) {
        if (paymentType == PaymentType.Credit){
            title?.text = getString(R.string.got_your_order)
            if (Shop.isDirectPayment == true) {
                text?.isVisible = false
                return
            }
            text?.text = getString(R.string.credit_order_complete)
        }
    }

    private fun initButton() {
        backButton?.onClick {
            (foregroundActivity as? MainActivity)?.clearBackStack()
            if (!database.network?.shops.isNullOrEmpty())
                NetworkFragment.open()
            ProductListFragment.open(Database.shop?.categories?.firstOrNull()?.name ?: return@onClick)
            dismiss()
        }
        button?.run {
            setTextColor(mainColor)
            onClick {
                (foregroundActivity as? MainActivity)?.clearBackStack()
                if (!database.network?.shops.isNullOrEmpty())
                    NetworkFragment.open()
                ProductListFragment.open(Database.shop?.categories?.firstOrNull()?.name ?: return@onClick)
                HistoryListFragment.open()
                order?.let { it1 -> HistoryOrderFragment.open(it1) }
                dismiss()
            }
        }
    }

}