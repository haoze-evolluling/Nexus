package com.haoze.nexus.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.haoze.nexus.R
import com.haoze.nexus.ui.audio.AudioReceiverActivity

/**
 * Manages foreground service notification and incoming connection authorization notifications.
 */
class AudioReceiverNotificationManager(private val context: Context) {

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 8
        const val AUTH_NOTIFICATION_ID = 9
        const val ACTION_RESPOND = "com.haoze.nexus.audio.action.RESPOND"
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_ALLOW = "allow"
        const val EXTRA_REMEMBER = "remember"
        private const val RECEIVER_CHANNEL_ID = "nexus-receiver"
        private const val AUTH_CHANNEL_ID = "nexus-auth"
    }

    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun createForegroundNotification(pcName: String?): Notification {
        val loc = LocaleManager.wrap(context)
        val channel = NotificationChannel(
            RECEIVER_CHANNEL_ID,
            loc.getString(R.string.receiver_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(context, RECEIVER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(loc.getString(R.string.app_name))
            .setContentText(
                if (pcName == null) loc.getString(R.string.receiver_notification)
                else loc.getString(R.string.receiver_notification_active, pcName)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    fun updateForegroundNotification(pcName: String?) {
        runCatching {
            notificationManager.notify(FOREGROUND_NOTIFICATION_ID, createForegroundNotification(pcName))
        }
    }

    fun postAuthNotification(prompt: PcAuthPrompt) {
        val loc = LocaleManager.wrap(context)
        val authChannel = NotificationChannel(
            AUTH_CHANNEL_ID,
            loc.getString(R.string.auth_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(authChannel)

        val openApp = PendingIntent.getActivity(
            context,
            prompt.requestId.hashCode(),
            Intent(context, AudioReceiverActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, AUTH_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(loc.getString(R.string.auth_title))
            .setContentText(loc.getString(R.string.auth_notification_text, prompt.name))
            .setContentIntent(openApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .addAction(0, loc.getString(R.string.auth_deny), respondIntent(prompt, false, false))
            .addAction(0, loc.getString(R.string.auth_allow), respondIntent(prompt, true, false))
            .addAction(0, loc.getString(R.string.auth_always_allow), respondIntent(prompt, true, true))
            .build()
        notificationManager.notify(AUTH_NOTIFICATION_ID, notification)
    }

    fun dismissAuthNotification() {
        notificationManager.cancel(AUTH_NOTIFICATION_ID)
    }

    private fun respondIntent(prompt: PcAuthPrompt, allow: Boolean, remember: Boolean): PendingIntent {
        val intent = Intent(context, AudioReceiverService::class.java).setAction(ACTION_RESPOND)
            .putExtra(EXTRA_REQUEST_ID, prompt.requestId)
            .putExtra(EXTRA_ALLOW, allow)
            .putExtra(EXTRA_REMEMBER, remember)
        return PendingIntent.getService(
            context,
            (prompt.requestId + allow + remember).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
