package il.co.superclick.dialogs

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.utilities.glide
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.utilities.delay
import il.co.superclick.data.ShopProduct
import il.co.superclick.utilities.getString
import kotlinx.android.synthetic.main.dialog_product_alert.*

class ProductAlertDialog : BaseDialog() {

    companion object : Comp<ProductAlertDialog>() {
        private const val KEY_PRODUCT = "KEY_PRODUCT"
        private const val KEY_TEXT = "KEY_TEXT"
        private const val KEY_MAX_LINES = "KEY_MAX_LINES"
        private const val KEY_TEXT_ALIGNMENT = "KEY_TEXT_ALIGNMENT"
        private const val KEY_CLOSE_BUTTON = "KEY_CLOSE_BUTTON"
        private const val KEY_AUTO_CLOSE = "KEY_AUTO_CLOSE"
        const val AUTO_CLOSE_MS = 700L

        fun open(
            product: ShopProduct,
            text: String,
            lines: Int? = null,
            textAlignment: Int = Gravity.CENTER,
            closeButton: Boolean = false,
            autoClose: Long? = AUTO_CLOSE_MS
        ) {
            open(
                KEY_PRODUCT to product,
                KEY_TEXT to text,
                KEY_MAX_LINES to lines,
                KEY_TEXT_ALIGNMENT to textAlignment,
                KEY_CLOSE_BUTTON to closeButton,
                KEY_AUTO_CLOSE to autoClose
            )
        }

        fun productAdded(product: ShopProduct?) {
            open(
                product ?: return,
                getString(R.string.dialog_product_alert_add_success),
                lines = 1
            )
        }

        fun productUpdated(product: ShopProduct?) {
            open(
                product ?: return,
                getString(R.string.dialog_product_alert_update_success),
                lines = 1
            )
        }

        fun productRemoved(product: ShopProduct?) {
            open(
                product ?: return,
                getString(R.string.dialog_product_alert_remove_success)
            )
        }
    }

    override val layout = R.layout.dialog_product_alert
    override val gravity: Int = Gravity.CENTER
    override val widthFactor = 0.9f
    override val heightFactor = 0.3f
    override val isCancelable: Boolean get() = false
    private val image: ImageView? get() = dialog_product_alert_image
    private val textView: TextView? get() = dialog_product_alert_text
    private val closeButton: ImageView? get() = dialog_product_alert_close
    private var product: ShopProduct? = null
    private var text: String? = null
    private var maxLines: Int? = null
    private var textAlignment: Int? = null
    private var autoCloseMs: Long? = null
    private var isCloseButton: Boolean = false

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_PRODUCT] as? ShopProduct)?.let { this.product = it }
        (arguments[KEY_TEXT] as? String)?.let { this.text = it }
        (arguments[KEY_MAX_LINES] as? Int)?.let { this.maxLines = it }
        (arguments[KEY_TEXT_ALIGNMENT] as? Int)?.let { this.textAlignment = it }
        (arguments[KEY_CLOSE_BUTTON] as? Boolean)?.let { this.isCloseButton = it }
        (arguments[KEY_AUTO_CLOSE] as? Long)?.let { this.autoCloseMs = it }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        displayProduct()
        maxLines?.let { textView?.setLines(it) }
        textAlignment?.let { textView?.gravity = it }
        autoCloseMs?.takeIf { it >= 0 }?.run(::autoClose)
        closeButton?.isVisible = isCloseButton
        closeButton?.onClick { close() }
    }

    private fun displayProduct() = product?.run {
        image?.glide(product.image)
        textView?.text = text
    }

    private fun autoClose(delay: Long) = delay(delay) {
        close()
        dismiss()
    }

}