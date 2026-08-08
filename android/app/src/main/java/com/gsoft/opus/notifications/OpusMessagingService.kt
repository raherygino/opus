package com.gsoft.opus.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for OPUS.
 *
 * Responsibilities:
 *  - Receives incoming push messages and displays them as system notifications.
 *  - Handles FCM token rotation by re-registering the new token with the backend.
 *
 * Declared in AndroidManifest.xml with an intent-filter for the FCM instance ID
 * service (com.google.firebase.MESSAGING_EVENT).
 */
@AndroidEntryPoint
class OpusMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "OpusMessagingService"
        private const val DEFAULT_NOTIFICATION_ID = 1
    }

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var navigationBus: NotificationNavigationBus

    /**
     * Called when a new FCM token is issued. The token may rotate, so we
     * re-register it with the backend.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: ${token.take(12)}…")
        fcmTokenManager.registerIfLoggedIn(token)
    }

    /**
     * Called when an FCM message is received.
     *
     * The PHP backend sends messages with a `notification` payload (title/body)
     * and an optional `data` payload (notification_id, type, service, click_action).
     *
     * When the app is in the foreground, FCM does NOT display the notification
     * automatically — we must show it ourselves. In the background, the system
     * tray handles it, but we still get this callback if a data payload is present.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Message received from: ${message.from}")

        val data = message.data
        val title = message.notification?.title
            ?: data["title"]
            ?: "OPUS"
        val body = message.notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: ""

        val type = data["type"] ?: "info"
        val highPriority = type == "warning" || type == "error"

        val notificationId = data["notification_id"]?.toIntOrNull()
            ?: (System.currentTimeMillis() and 0xFFFFFFFFL).toInt()

        Log.d(TAG, "Showing notification: id=$notificationId title=$title type=$type")

        // Let the notifications screen refresh its list in real time.
        navigationBus.notifyNotificationReceived()

        notificationHelper.showNotification(
            notificationId = notificationId,
            title = title,
            body = body,
            data = data,
            highPriority = highPriority
        )
    }
}
