package il.co.superclick.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dm6801.framework.infrastructure.foregroundActivity
import il.co.superclick.remote.Remote
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.utilities.*
import com.onesignal.NotificationExtenderService
import com.onesignal.OSNotificationReceivedResult
import com.onesignal.OneSignal
import il.co.superclick.MainActivity
import il.co.superclick.R
import il.co.superclick.data.Database
import il.co.superclick.dialogs.NotificationDialog

class OneSignalExtender : NotificationExtenderService() {

    companion object {
        val oneSignalId: String? get() = OneSignal.getPermissionSubscriptionState()?.subscriptionStatus?.userId
        private val notificationManager: NotificationManager?
            get() = foregroundApplication.getSystemService(
                NotificationManager::class.java
            )
        private var requestcode = 3421

        fun init() {
            try {
                OneSignal.setSubscription(true)
                OneSignal.startInit(foregroundApplication)
                    .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.None)
                    .unsubscribeWhenNotificationsAreDisabled(true)
                    .init()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @Suppress("DeferredResultUnused")
        fun sendOneSignalId(userId: String? = oneSignalId) {
            if (userId.isNullOrBlank()) return
            Database.onesignalId = userId
            background {
                suspendCatch {
                    Remote.setOneSignal()
                }
            }
        }
    }

    override fun onNotificationProcessing(notification: OSNotificationReceivedResult?): Boolean {
        Log("onNotificationProcessing(): ${notification?.payload}")
        notification?.payload?.body ?: return false
        foregroundActivity?.apply {
            main {
                NotificationDialog.open(notification.payload.body)
            }
        }
        showNotification(notification.payload.body)
        return true
    }

    private fun showNotification(message: String?) {
        val context = foregroundApplication

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestcode,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_ONE_SHOT
        )

        val NOTIFICATION_CHANNEL_ID = "${packageName}_channel"
        createNotificationChannel(NOTIFICATION_CHANNEL_ID)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .apply {
                setContentTitle(getString(R.string.app_name))
                setStyle(NotificationCompat.BigTextStyle().bigText(message))
                setContentText(message)
                setSmallIcon(R.mipmap.ic_launcher)
                setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                setAutoCancel(true)
                setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
                setContentIntent(pendingIntent)
            }
            .build()
        notificationManager?.notify(
            requestcode,
            notification.apply { flags = flags or Notification.FLAG_AUTO_CANCEL }
        )

        requestcode += 1
    }

    private fun createNotificationChannel(channelId: String, channelName: String = channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

            notificationChannel.apply {
                description = foregroundApplication.packageName
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

}