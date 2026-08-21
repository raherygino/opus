package com.gsoft.opus.presentation.notifications

import android.util.Log
import com.gsoft.opus.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide single source of truth for the unread notification count.
 *
 * The bottom-navigation badge and every other UI element that shows a
 * notification dot observe [count] — a hot [StateFlow] that always reflects
 * the latest value known to the app.
 *
 * The store is updated from three sources:
 *  1. **FCM push** — [OpusMessagingService] calls [onPushReceived] whenever a
 *     new push arrives. The store increments optimistically and then refreshes
 *     from the server so the count is correct even if multiple pushes arrive.
 *  2. **Notifications screen** — [NotificationsViewModel] calls [setCount]
 *     whenever it loads the full list or marks items as read, so the badge
 *     stays in sync with what the user sees.
 *  3. **App foreground / periodic refresh** — [refreshFromServer] is called
 *     when the app comes to the foreground and periodically so the badge is
 *     correct even when the notifications screen has never been opened.
 *
 * Because this is a [@Singleton] scoped to the application graph, the count
 * survives navigation between screens and configuration changes.
 */
@Singleton
class UnreadCountStore @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    companion object {
        private const val TAG = "UnreadCountStore"
    }

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Set the count directly (used by the notifications screen after a load). */
    fun setCount(value: Int) {
        _count.value = value.coerceAtLeast(0)
    }

    /** Increment optimistically when a push arrives, then sync from server. */
    fun onPushReceived() {
        _count.value = _count.value + 1
        refreshFromServer()
    }

    /**
     * Fetch the authoritative unread count from the server and publish it.
     * Safe to call from any thread; failures are logged and silently ignored
     * so the badge keeps showing the last known value.
     */
    fun refreshFromServer() {
        scope.launch {
            try {
                val result = notificationRepository.getUnreadCount()
                if (result is com.gsoft.opus.core.Resource.Success) {
                    _count.value = result.data.coerceAtLeast(0)
                }
            } catch (e: Exception) {
                Log.w(TAG, "refreshFromServer failed: ${e.message}")
            }
        }
    }
}
