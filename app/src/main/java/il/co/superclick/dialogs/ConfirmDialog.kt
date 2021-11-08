package il.co.superclick.dialogs

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.getString
import com.dm6801.framework.utilities.main
import il.co.superclick.R
import il.co.superclick.cart.CartFragment
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.infrastructure.Locator
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.dialog_confirmation.*
import kotlinx.coroutines.delay
import kotlin.system.exitProcess


internal typealias ButtonLambda = Pair<Any?, () -> Unit>

open class ConfirmDialog : BaseDialog() {

    companion object : Comp<ConfirmDialog>() {
        private const val KEY_TEXT = "KEY_TEXT"
        private const val KEY_ON_CONFIRM = "KEY_ON_CONFIRM"
        private const val KEY_ON_CANCEL = "KEY_ON_CANCEL"
        private const val KEY_ON_CLOSE = "KEY_ON_CLOSE"

        private val cart get() = Locator.database.cart

        fun open(
            text: Any?,
            onConfirm: ButtonLambda,
            onCancel: ButtonLambda,
            onClose: (() -> Unit)? = onCancel.second
        ) {
            open(
                KEY_TEXT to text.getString(),
                KEY_ON_CONFIRM to onConfirm,
                KEY_ON_CANCEL to onCancel,
                KEY_ON_CLOSE to onClose
            )
        }

        private fun Any?.getString(): String {
            return when (this) {
                is Int -> getString(this).toString()
                is CharSequence -> toString()
                else -> this.toString()
            }
        }

        fun deleteProduct(productId: Int, onConfirm: (() -> Unit)? = null, onCancel: (() -> Unit)? = null) {
            open(
                R.string.item_cart_delete_confirm_text,
                R.string.item_cart_delete_confirm_button to { cart.remove(productId)?.let { _ -> onConfirm?.invoke() } },
                R.string.item_cart_delete_cancel_button to { onCancel?.invoke() }
            )
        }

        fun deleteProduct(onConfirm: (() -> Unit)? = null, onCancel: (() -> Unit)? = null) {
            open(
                R.string.item_cart_delete_confirm_text,
                R.string.item_cart_delete_confirm_button to { onConfirm?.invoke() },
                R.string.item_cart_delete_cancel_button to { onCancel?.invoke() }
            )
        }

        fun additionalProduct(title: Int = R.string.toppings_additional_product_text, onCancel: () -> Unit, onConfirm: () -> Unit) {
            open(
               // R.string.toppings_additional_product_text,
                title,
                R.string.toppings_additional_product_confirm_button to onConfirm,
                R.string.toppings_additional_product_cancel_button to onCancel
            )
        }

        fun deleteAdditionalProduct(onConfirm: (() -> Unit)? = null) {
            open(
                R.string.item_cart_delete_confirm_text,
                R.string.item_cart_delete_confirm_button to {
                    onConfirm?.invoke()
                    Unit
                },
                R.string.item_cart_delete_cancel_button to {}
            )
        }

        fun editOrAddMeal(addNew:(() -> Unit)? = null){
            open(
                R.string.what_to_do,
                R.string.order_one_more to {
                    addNew?.invoke()
                },
                R.string.edit to {
                    CartFragment.open()
                }

            )
        }

        fun exit() {
            open(
                R.string.dialog_exit_confirm_text,
                R.string.dialog_exit_confirm to { foregroundActivity?.finishAndRemoveTask(); exitProcess(0); Unit },
                R.string.dialog_exit_cancel to {}
            )
        }
    }

    override val layout = R.layout.dialog_confirmation
    override val gravity: Int = Gravity.CENTER
    override val widthFactor = 0.95f
    override val heightFactor = 0.5f
    private val textView: TextView? get() = dialog_confirm_text
    private val confirm: TextView? get() = dialog_confirm_button
    protected val cancel: TextView? get() = dialog_confirm_cancel

    private var text: String? = null
    private var confirmText: String? = null
    private var onConfirm: (() -> Unit)? = null
    private var cancelText: String? = null
    private var onCancel: (() -> Unit)? = null

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_TEXT] as? String)?.let { text = it }
        (arguments[KEY_ON_CONFIRM] as? ButtonLambda)?.let { (text, callback) ->
            confirmText = text.getString()
            onConfirm = callback
        }
        (arguments[KEY_ON_CANCEL] as? ButtonLambda)?.let { (text, callback) ->
            cancelText = text.getString()
            onCancel = callback
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        textView?.text = text
        confirm?.apply {
            text = confirmText
            onClick { dismiss() }
            setTextColor(mainColor)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setStroke(1.dpToPx, mainColor)
                cornerRadius = 16.dpToPx.toFloat()
            }
        }
        cancel?.apply {
            text = cancelText
            setTextColor(mainColor)
            onClick { main { onCancel?.invoke(); delay(300); cancel() } }
        }
    }

    override fun onDismiss() {
        super.onDismiss()
        onConfirm?.invoke()
    }

    override fun onCancel() {
        super.onCancel()
    }

}