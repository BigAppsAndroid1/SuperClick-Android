package il.co.superclick.meal

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.utilities.main
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.utilities.*
import kotlinx.android.synthetic.main.dialog_item_added_to_meal.*
import kotlinx.coroutines.delay

class ItemMealDialog : BaseDialog() {

    companion object : Comp<ItemMealDialog>() {

        private const val KEY_ITEM_INDEX = "KEY_ITEM_INDEX"
        private const val KEY_MAX_INDEX = "KEY_MAX_INDEX"
        private const val KEY_ON_CONFIRM = "KEY_TYPE"
        private const val KEY_NAME = "KEY_NAME"


        fun open(
            itemIndex: Int = 0,
            maxIndex: Int,
            name: String? = null,
            delay: Long = 1800,
            onConfirm: (() -> Unit)? = null,
        ) {
            val dialog = open(
                KEY_ITEM_INDEX to itemIndex,
                KEY_MAX_INDEX to maxIndex,
                KEY_ON_CONFIRM to onConfirm,
                KEY_NAME to name
            )
            dialog?.show()
            if (name == null) {
                main {
                    delay(delay)
                    dialog?.dismiss()
                }
            }
        }
    }

    override val layout: Int get() = R.layout.dialog_item_added_to_meal
   // override val widthFactor: Float get() = 0.9f
   // override val heightFactor: Float get() = 0.5f
    override val closeWithActivity: Boolean
        get() = false
    override val isCancelable: Boolean get() = false
    private val text: TextView? get() = dialog_item_meal_added_text
    private val buttonsContainer: LinearLayout? get() = dialog_meals_buttons_view
    private val btnConfirm: TextView? get() = dialog_meal_confirm
    private val btnCancel: TextView? get() = dialog_meal_cancel
    private var itemIndex: Int = 0
    private var maxIndex: Int = 0
    private var name: String? = null
    private var onConfirm: (() -> Unit)? = null

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        if (name?.isEmpty() == false) {
            buttonsContainer?.isVisible = true
            text?.text = context.getString(R.string.meal_added_to_cart_text, name)
            btnCancel?.onClick { foregroundActivity?.popBackStack(); dismiss() }
            btnConfirm?.apply {
                setThemeColor()
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setStroke(1.dpToPx, mainColor)
                    cornerRadius = 16.dpToPx.toFloat()
                }
                onClick { onConfirm?.invoke(); dismiss() }
            }
        } else {
            if (itemIndex < maxIndex)
                text?.text =
                    getString(R.string.meal_item_added_text, itemIndex, maxIndex, itemIndex + 1)
            else
                text?.text = getString(R.string.meal_last_item_added_text, itemIndex, maxIndex)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_ITEM_INDEX] as? Int)?.let { itemIndex = it }
        (arguments[KEY_MAX_INDEX] as? Int)?.let { maxIndex = it }
        (arguments[KEY_NAME] as? String)?.let { name = it }
        (arguments[KEY_ON_CONFIRM] as? () -> Unit)?.let { onConfirm = it }

    }

}