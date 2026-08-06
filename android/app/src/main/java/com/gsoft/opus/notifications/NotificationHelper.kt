package com.gsoft.opus.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gsoft.opus.MainActivity
import com.gsoft.opus.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for creating and displaying system notifications from FCM push messages.
 *
 * Creates the notification channel on Android 8+ and builds notifications that
 * open [MainActivity] when tapped.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_DEFAULT = "opus_notifications"
        const val CHANNEL_ID_HIGH = "opus_notifications_high"
        const val CHANNEL_NAME_DEFAULT = "OPUS Notifications"
        const val CHANNEL_NAME_HIGH = "OPUS Alerts"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_CLICK_ACTION = "extra_click_action"
        const val ACTION_OPEN_NOTIFICATIONS = "OPEN_NOTIFICATIONS"
    }

    /**
     * Register notification channels. Safe to call multiple times.
     * Must be called on app startup (e.g. in Application.onCreate).
     */
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Default priority channel
        if (manager.getNotificationChannel(CHANNEL_ID_DEFAULT) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                CHANNEL_NAME_DEFAULT,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General OPUS notifications"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        // High priority channel (alerts)
        if (manager.getNotificationChannel(CHANNEL_ID_HIGH) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID_HIGH,
                CHANNEL_NAME_HIGH,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority OPUS alerts"
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Display a notification from an FCM message.
     *
     * @param notificationId Unique ID for this notification (use the server-side notification ID if available)
     * @param title Notification title
     * @param body Notification body text
     * @param data Optional data payload from FCM (used to determine click action)
     * @param highPriority If true, uses the high-importance channel (heads-up notification)
     */
    fun showNotification(
        notificationId: Int,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        highPriority: Boolean = false
    ) {
        createChannels()

        val clickAction = data["click_action"] ?: ACTION_OPEN_NOTIFICATIONS

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CLICK_ACTION, clickAction)
            data["notification_id"]?.let { putExtra(EXTRA_NOTIFICATION_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = if (highPriority) CHANNEL_ID_HIGH else CHANNEL_ID_DEFAULT

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_bell_fill_24)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, builder.build())
    }
}
