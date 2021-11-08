package il.co.superclick.dialogs

import android.view.View
import androidx.core.view.isGone
import com.dm6801.framework.utilities.main

class InfoDialog : ConfirmDialog() {

    companion object : Comp<InfoDialog>() {
        private const val KEY_TEXT = "KEY_TEXT"
        private const val KEY_ON_CONFIRM = "KEY_ON_CONFIRM"

        fun open(
            text: Any?,
            onClose: ButtonLambda
        ) = main {
            open(
                KEY_TEXT to text.getString(),
                KEY_ON_CONFIRM to onClose
            )
        }

        private fun Any?.getString(): String {
            return when (this) {
                is Int -> com.dm6801.framework.ui.getString(this).toString()
                is CharSequence -> toString()
                else -> this.toString()
            }
        }
    }

    override val closeWithActivity: Boolean get() = false
    override val isCancelable: Boolean get() = false

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        cancel?.isGone = true
    }

}