package il.co.superclick.order

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.updateMargins
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.utilities.formatPrice
import il.co.superclick.utilities.getString
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.dialog_deliver_price.*

class DeliveryPriceDialog: BaseDialog() {

    companion object: Comp<DeliveryPriceDialog>(){
        private val KEY_ON_COMPLETE = "key_on_complete"
        private val KEY_PRICE = "key_price"
        fun open(price: Double, onComplete: () -> Unit){
            open(
                KEY_ON_COMPLETE to onComplete,
                KEY_PRICE to price
            )
        }
    }

    override val layout: Int get() = R.layout.dialog_deliver_price
    //override val heightFactor: Float? get() = 0.5f
    //override val widthFactor: Float? get() = 0.8f
    override val isBackgroundDim: Boolean get() = true
    override val closeWithActivity: Boolean get() = false
    override val isCancelable: Boolean get() = false
    private val btnClose: ImageView? get() = close
    private val button: TextView? get() = complete
    private val priceView: TextView? get() = price_view
    private val titleDialog: TextView? get() = title
    private val priceImage: ImageView? get() = image
    private val dialogText: TextView? get() = text
    private var onComplete: (() -> Unit)? = null
    private var price: Double? = null

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        setCancelable(false)
        btnClose?.onClick {
            hideProgressBar()
            MapFragment.isCanAction = true
            dismiss() }
        if((price ?: 0.0) > 0.0) {
            priceView?.text = price?.formatPrice()
          }
        else{
            priceView?.isVisible = false
            titleDialog?.isVisible = false
            priceImage?.isVisible = true
            dialogText?.updateMargins(60.dpToPx)
            dialogText?.text = getString(R.string.no_delivery_payment)

        }
        initButton()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_ON_COMPLETE] as? (() -> Unit))?.let { onComplete = it }
        (arguments[KEY_PRICE] as? Double)?.let { price = it }
    }

    private fun initButton() {
        button?.run {

            setTextColor(mainColor)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setStroke(1.dpToPx, mainColor)
                cornerRadius = 16.dpToPx.toFloat()
            }
            onClick {
                hideProgressBar()
                onComplete?.invoke()
                dismiss()
            }
        }
    }

}