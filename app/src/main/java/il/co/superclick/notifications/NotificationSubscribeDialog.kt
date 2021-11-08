package il.co.superclick.notifications

import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import il.co.superclick.infrastructure.Locator
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseDialog
import com.dm6801.framework.ui.onClick
import kotlinx.android.synthetic.main.dialog_notification_subscribe.*

class NotificationSubscribeDialog : BaseDialog() {

    companion object : Comp<NotificationSubscribeDialog>() {
        private val database get() = Locator.database
        private val notifications get() = Locator.notifications
    }

    override val layout = R.layout.dialog_notification_subscribe
    override val gravity: Int = Gravity.CENTER
    private val submit: TextView? get() = notification_subscribe_submit
    private val cancel: TextView? get() = notification_subscribe_cancel

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        submit?.onClick {
            database.setNotifications(true)
            notifications.init()
            dismiss()
        }
        cancel?.onClick {
            cancel()
        }
    }

}