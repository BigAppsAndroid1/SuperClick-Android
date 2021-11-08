package il.co.superclick.meal

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.dm6801.framework.ui.dpToPx
import il.co.superclick.R
import il.co.superclick.cart.CartFragment
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import il.co.superclick.utilities.setThemeColor
import kotlinx.android.synthetic.main.dialog_add_or_edit_meal.*

class AddOrEditMealDialog: BaseDialog() {

    companion object: Comp<AddOrEditMealDialog>(){
        private const val KEY_ON_NEW = "KEY_ON_NEW"

        fun open(onNew:()->Unit){
            open(KEY_ON_NEW to onNew)?.show()
        }
    }
    override val closeWithActivity: Boolean get() = false
    //override val widthFactor: Float get() = 0.9f
    //override val heightFactor: Float get() = 0.4f
    override val layout: Int get() = R.layout.dialog_add_or_edit_meal

    private val new: TextView? get() = dialog_meal_new_product
    private val edit:TextView? get() = dialog_meal_edit_product
    private var onNew: (()->Unit)? = null

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        new?.setThemeColor()
        new?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(1.dpToPx, mainColor)
            cornerRadius = 16.dpToPx.toFloat()
        }
        new?.onClick { onNew?.invoke(); dismiss() }
        edit?.onClick { CartFragment.open(); dismiss() }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_ON_NEW] as? (()->Unit))?.let { onNew = it }
    }
}