package com.gsoft.opus.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.AppNotification
import com.gsoft.opus.domain.repository.NotificationRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import com.gsoft.opus.notifications.NotificationNavigationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotificationFilter { ALL, UNREAD, PJ, SG, SEDENTAIRE }

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAdmin: Boolean = false,
    val activeFilter: NotificationFilter = NotificationFilter.ALL,
    val errorMessage: String? = null,
    val deleteTarget: AppNotification? = null,
    val isDeleting: Boolean = false,
    val isMarkingAll: Boolean = false
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }

    val filtered: List<AppNotification>
        get() = when (activeFilter) {
            NotificationFilter.ALL -> notifications
            NotificationFilter.UNREAD -> notifications.filter { !it.isRead }
            NotificationFilter.PJ -> notifications.filter { it.service == "PJ" }
            NotificationFilter.SG -> notifications.filter { it.service == "SG" }
            NotificationFilter.SEDENTAIRE -> notifications.filter { it.service == "Sedentaire" }
        }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val navigationBus: NotificationNavigationBus
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        loadRole()
        refresh()
        // Reload the list silently whenever a push arrives while the app runs.
        viewModelScope.launch {
            navigationBus.notificationEvents.collect { refresh(silent = true) }
        }
    }

    private fun loadRole() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            val isAdmin = user?.roleCode == "SUPER_ADMIN" || user?.roleCode == "STATION_ADMIN"
            _state.update { it.copy(isAdmin = isAdmin) }
        }
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = if (silent) it.isLoading else true,
                    isRefreshing = silent,
                    errorMessage = null
                )
            }
            when (val result = notificationRepository.getNotifications()) {
                is Resource.Success -> _state.update {
                    it.copy(notifications = result.data, isLoading = false, isRefreshing = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = if (silent) it.errorMessage else result.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setFilter(filter: NotificationFilter) {
        _state.update { it.copy(activeFilter = filter) }
    }

    fun markAsRead(id: Int) {
        viewModelScope.launch {
            if (notificationRepository.markAsRead(id)) {
                _state.update { s ->
                    s.copy(notifications = s.notifications.map {
                        if (it.id == id) it.copy(isRead = true) else it
                    })
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _state.update { it.copy(isMarkingAll = true) }
            val ok = notificationRepository.markAllAsRead()
            _state.update { s ->
                s.copy(
                    isMarkingAll = false,
                    notifications = if (ok) s.notifications.map { it.copy(isRead = true) } else s.notifications,
                    errorMessage = if (ok) null else "Impossible de tout marquer comme lu"
                )
            }
        }
    }

    fun requestDelete(notification: AppNotification) {
        _state.update { it.copy(deleteTarget = notification) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            val ok = notificationRepository.delete(target.id)
            _state.update { s ->
                s.copy(
                    isDeleting = false,
                    deleteTarget = null,
                    notifications = if (ok) s.notifications.filterNot { it.id == target.id } else s.notifications,
                    errorMessage = if (ok) null else "Impossible de supprimer la notification"
                )
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
