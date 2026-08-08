package com.gsoft.opus.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decouples push-notification events from the UI:
 *
 *  - [requestOpenNotifications] is called when the user taps a system
 *    notification (from [com.gsoft.opus.MainActivity]). The request is sticky
 *    so it survives a cold start — the main shell consumes it as soon as it
 *    is composed and selects the Notifications tab.
 *  - [notifyNotificationReceived] is called by [OpusMessagingService] whenever
 *    a push arrives while the app is running, so the notifications screen can
 *    refresh its list in real time.
 */
@Singleton
class NotificationNavigationBus @Inject constructor() {

    private val _openRequests = MutableStateFlow(false)
    val openRequests: StateFlow<Boolean> = _openRequests.asStateFlow()

    private val _notificationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val notificationEvents: SharedFlow<Unit> = _notificationEvents.asSharedFlow()

    fun requestOpenNotifications() {
        _openRequests.value = true
    }

    fun consumeOpenRequest() {
        _openRequests.value = false
    }

    fun notifyNotificationReceived() {
        _notificationEvents.tryEmit(Unit)
    }
}
