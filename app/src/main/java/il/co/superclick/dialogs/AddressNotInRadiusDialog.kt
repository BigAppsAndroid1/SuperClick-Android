package il.co.superclick.dialogs

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.ui.getString
import com.dm6801.framework.ui.onClick
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.order.MapFragment
import il.co.superclick.utilities.setThemeColor
import kotlinx.android.synthetic.main.dialog_address_not_in_radius.*

class AddressNotInRadiusDialog : BaseDialog() {

    override val layout: Int get() = R.layout.dialog_address_not_in_radius
    override val isCancelable: Boolean
        get() = false
    private var address: String? = null
    private val closeButton: ImageView? get() = dialog_address_not_in_radius_close
    private val visitUsText: TextView? get() = dialog_address_not_in_radius_visit_us
    private val title: TextView? get() = dialog_address_not_in_radius_title

    companion object : Comp<MinimumForOrderAlertDialog>() {
        private const val KEY_ADDRESS = "KEY_ADDRESS"

        fun open(address: String) {
            AddressNotInRadiusDialog.open(KEY_ADDRESS to address)
        }
    }

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_ADDRESS] as? String)?.let { this.address = it }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        setCancelable(false)
        hideProgressBar()
        setAddress()
        title?.setThemeColor()
        closeButton?.onClick {
            MapFragment.isCanAction = true
            dismiss() }

    }

    private fun setAddress() {
        visitUsText?.text = getString(R.string.visit_us_with_address, address ?: return)
    }

}