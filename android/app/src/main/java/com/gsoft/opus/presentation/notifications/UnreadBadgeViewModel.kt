package com.gsoft.opus.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.notifications.NotificationNavigationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin ViewModel that exposes the app-wide [UnreadCountStore] to Compose.
 *
 * Used by [com.gsoft.opus.presentation.main.MainScreen] to drive the
 * bottom-navigation badge. Keeping this separate from
 * [NotificationsViewModel] means the badge count is available even before the
 * user opens the notifications screen, and it survives navigation between
 * screens because [UnreadCountStore] is a singleton.
 */
@HiltViewModel
class UnreadBadgeViewModel @Inject constructor(
    private val unreadCountStore: UnreadCountStore,
    private val navigationBus: NotificationNavigationBus
) : ViewModel() {

    val unreadCount: StateFlow<Int> = unreadCountStore.count

    init {
        // Sync from server on creation (app start / process recreation).
        refreshFromServer()
        // Whenever a push arrives while the app is running, the store bumps
        // the count optimistically and then re-syncs from the server.
        viewModelScope.launch {
            navigationBus.notificationEvents.collect {
                unreadCountStore.onPushReceived()
            }
        }
    }

    /** Force a refresh of the unread count from the server. */
    fun refreshFromServer() {
        unreadCountStore.refreshFromServer()
    }
}
