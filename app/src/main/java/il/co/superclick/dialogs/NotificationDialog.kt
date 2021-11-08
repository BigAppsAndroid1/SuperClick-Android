package il.co.superclick.dialogs

import android.view.Gravity
import android.view.View
import android.widget.TextView
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.dialog_notification.*

class NotificationDialog : BaseDialog() {

    companion object: Comp<NotificationDialog>(){
        private const val KEY_TEXT = "KEY_TEXT"
        fun open(message: String){
            open(KEY_TEXT to message)
        }
    }

    override val layout: Int get() = R.layout.dialog_notification
    private val dialogText:TextView? get() = dialog_notification_text
    private val dialogClose:TextView? get() = dialog_notification_close
   // override val widthFactor: Float? get() = 0.7f
   // override val heightFactor: Float? get() = 0.3f

    private var text: String? = ""

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_TEXT] as? String)?.let { text = it }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        setWindow()
        dialogClose?.backgroundTintList = mainColor
        dialogText?.text = text
        dialogClose?.onClick { dismiss() }
    }

    private fun setWindow() {
        window?.attributes?.gravity = Gravity.CENTER
        window?.setBackgroundDrawableResource(R.drawable.rect_white_corners_10)
    }

}