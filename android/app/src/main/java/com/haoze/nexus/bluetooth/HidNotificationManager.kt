package com.haoze.nexus.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.haoze.nexus.MainActivity
import com.haoze.nexus.R
import com.haoze.nexus.ui.AppLanguageManager

/**
 * Handles creation and updates of foreground and connection event notifications
 * for [BluetoothHidService].
 */
class HidNotificationManager(private val context: Context) {

    private val localizedContext: Context
        get() = AppLanguageManager.wrap(context)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "bluetooth_hid_channel"
        const val NOTIFICATION_ID = 1001
        const val CONNECTION_NOTIFICATION_CHANNEL_ID = "connection_events"
        const val CONNECTION_NOTIFICATION_ID = 1002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                localizedContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = localizedContext.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(serviceChannel)

            val connectionChannel = NotificationChannel(
                CONNECTION_NOTIFICATION_CHANNEL_ID,
                localizedContext.getString(R.string.notification_connection_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = localizedContext.getString(R.string.notification_connection_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(connectionChannel)
        }
    }

    fun createForegroundNotification(contentText: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(localizedContext.getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun updateForegroundNotification(contentText: String) {
        val notification = createForegroundNotification(contentText)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showConnectionNotification(contentText: String, enabled: Boolean) {
        if (!enabled) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CONNECTION_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(localizedContext.getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(CONNECTION_NOTIFICATION_ID, notification)
    }

    fun dismissConnectionNotification() {
        notificationManager.cancel(CONNECTION_NOTIFICATION_ID)
    }
}
