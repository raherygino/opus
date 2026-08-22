package com.gsoft.opus.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.gsoft.opus.core.Constants
import com.gsoft.opus.MainActivity
import com.gsoft.opus.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for creating and displaying system notifications from FCM push messages.
 *
 * Creates the notification channel on Android 8+ and builds notifications that
 * open [MainActivity] when tapped.
 *
 * **Grouping:** When multiple notifications are active, they are grouped into a
 * single expandable summary notification in the status bar (similar to WhatsApp
 * or Gmail). The summary shows "N new notifications" and, when expanded, lists
 * the individual notifications. Each individual notification shows the sender's
 * first name and profile picture when available.
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
        const val EXTRA_LINK = "extra_link"
        const val ACTION_OPEN_NOTIFICATIONS = "OPEN_NOTIFICATIONS"

        private const val GROUP_KEY = "opus_notifications_group"
        private const val SUMMARY_NOTIFICATION_ID = -1
        private const val MAX_SUMMARY_LINES = 6
        private const val MAX_ACTIVE_NOTIFICATIONS = 20
    }

    /** A lightweight snapshot of a notification for grouping/summary purposes. */
    private data class NotificationInfo(
        val id: Int,
        val title: String,
        val body: String,
        val senderName: String,
        val senderPhotoUrl: String?,
        val highPriority: Boolean
    )

    // Thread-safe list of currently active (not yet dismissed) notifications.
    private val activeNotifications = Collections.synchronizedList(mutableListOf<NotificationInfo>())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
     * @param data Optional data payload from FCM (used to determine click action and sender info)
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

        // Extract sender info from the FCM data payload.
        val senderName = data["creator_firstname"]?.takeIf { it.isNotBlank() }
            ?: data["sender_name"]?.takeIf { it.isNotBlank() }
            ?: "OPUS"
        val creatorPersonnelId = data["creator_personnel_id"]?.toIntOrNull()
        val creatorHasPhoto = data["creator_has_photo"] == "1"
        val senderPhotoUrl = if (creatorPersonnelId != null && creatorHasPhoto) {
            "${Constants.BASE_URL.trimEnd('/')}/api/personnel/$creatorPersonnelId/photo"
        } else {
            null
        }

        // Track this notification for the summary.
        val info = NotificationInfo(
            id = notificationId,
            title = title,
            body = body,
            senderName = senderName,
            senderPhotoUrl = senderPhotoUrl,
            highPriority = highPriority
        )
        // Remove any existing entry with the same ID, then add the new one.
        activeNotifications.removeAll { it.id == notificationId }
        activeNotifications.add(info)
        // Cap the list to prevent unbounded growth.
        if (activeNotifications.size > MAX_ACTIVE_NOTIFICATIONS) {
            activeNotifications.subList(0, activeNotifications.size - MAX_ACTIVE_NOTIFICATIONS).clear()
        }

        // Build and show the individual notification (grouped).
        showIndividualNotification(info, data)

        // Update (or create) the summary notification.
        showSummaryNotification()
    }

    private fun showIndividualNotification(
        info: NotificationInfo,
        data: Map<String, String>
    ) {
        val clickAction = data["click_action"] ?: ACTION_OPEN_NOTIFICATIONS

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CLICK_ACTION, clickAction)
            data["notification_id"]?.let { putExtra(EXTRA_NOTIFICATION_ID, it) }
            data["link"]?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_LINK, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            info.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = if (info.highPriority) CHANNEL_ID_HIGH else CHANNEL_ID_DEFAULT

        // Use the sender's name as the title (like a messaging app), and the
        // notification title + body as the content.
        val contentTitle = info.senderName
        val contentText = if (info.title.isNotBlank() && info.body.isNotBlank()) {
            "${info.title}: ${info.body}"
        } else {
            info.title.ifBlank { info.body }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_opus_simple)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY)
            .setPriority(
                if (info.highPriority) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )

        val manager = context.getSystemService(NotificationManager::class.java)

        // Try to load the sender's profile photo as the large icon.
        // This is done asynchronously — if it fails or is slow, the notification
        // is still shown immediately without the large icon.
        if (info.senderPhotoUrl != null) {
            scope.launch {
                val bitmap = loadPhotoBitmap(info.senderPhotoUrl)
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                }
                manager?.notify(info.id, builder.build())
            }
        } else {
            manager?.notify(info.id, builder.build())
        }
    }

    private fun showSummaryNotification() {
        val snapshot = activeNotifications.toList()
        if (snapshot.isEmpty()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CLICK_ACTION, ACTION_OPEN_NOTIFICATIONS)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            SUMMARY_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val count = snapshot.size
        val contentTitle = if (count == 1) {
            "1 nouvelle notification"
        } else {
            "$count nouvelles notifications"
        }

        // Build the InboxStyle with lines from the most recent notifications.
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(contentTitle)
            .setSummaryText("OPUS")

        snapshot.takeLast(MAX_SUMMARY_LINES).reversed().forEach { info ->
            val line = if (info.title.isNotBlank() && info.body.isNotBlank()) {
                "${info.senderName}: ${info.title}"
            } else {
                val msg = info.title.ifBlank { info.body }
                "${info.senderName}: $msg"
            }
            inboxStyle.addLine(line)
        }

        if (count > MAX_SUMMARY_LINES) {
            inboxStyle.setSummaryText("OPUS · +${count - MAX_SUMMARY_LINES} de plus")
        }

        val anyHighPriority = snapshot.any { it.highPriority }
        val channelId = if (anyHighPriority) CHANNEL_ID_HIGH else CHANNEL_ID_DEFAULT

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_opus_simple)
            .setContentTitle(contentTitle)
            .setContentText(if (count == 1) snapshot.first().title else "OPUS")
            .setStyle(inboxStyle)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setPriority(
                if (anyHighPriority) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(SUMMARY_NOTIFICATION_ID, builder.build())
    }

    /**
     * Load a profile photo as a [Bitmap] using Coil. Returns null on failure.
     * Must be called from a background thread.
     */
    private suspend fun loadPhotoBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // Hardware bitmaps can't be used as notification large icons
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                result.drawable?.toBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Remove a notification from the active list and update the summary.
     * Called when the user dismisses a notification.
     */
    fun cancelNotification(notificationId: Int) {
        activeNotifications.removeAll { it.id == notificationId }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(notificationId)
        if (activeNotifications.isEmpty()) {
            manager?.cancel(SUMMARY_NOTIFICATION_ID)
        } else {
            showSummaryNotification()
        }
    }

    /**
     * Clear all active notifications and the summary.
     */
    fun cancelAll() {
        activeNotifications.clear()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancelAll()
    }
}

/**
 * Extension to convert an android.graphics.drawable.Drawable to a Bitmap.
 */
private fun android.graphics.drawable.Drawable.toBitmap(): Bitmap {
    if (this is android.graphics.drawable.BitmapDrawable) {
        return bitmap
    }
    val bitmap = Bitmap.createBitmap(
        intrinsicWidth.coerceAtLeast(1),
        intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
