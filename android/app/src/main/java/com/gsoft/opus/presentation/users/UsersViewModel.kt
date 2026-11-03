package com.gsoft.opus.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.User
import com.gsoft.opus.domain.repository.UserRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val USERS_MODULE = "users"

data class UsersUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isSuperAdmin: Boolean = false,
    val currentUserId: Int = 0,
    val canCreate: Boolean = false,
    val deleteTarget: User? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<User>
        get() {
            val query = searchQuery.trim().lowercase()
            return if (query.isEmpty()) users else users.filter { u ->
                u.username.lowercase().contains(query) ||
                    (u.lastName?.lowercase()?.contains(query) == true) ||
                    (u.firstName?.lowercase()?.contains(query) == true) ||
                    (u.im?.lowercase()?.contains(query) == true) ||
                    (u.roleName?.lowercase()?.contains(query) == true)
            }
        }

    /** An admin may only edit their own account, not another admin's. */
    fun canEditUser(user: User): Boolean =
        user.id == currentUserId || !user.isAdminRole

    /** Admin accounts can never be deleted, and no one deletes themselves. */
    fun canDeleteUser(user: User): Boolean =
        isSuperAdmin && user.id != currentUserId && !user.isAdminRole
}

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UsersUiState())
    val state: StateFlow<UsersUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    isSuperAdmin = user?.roleCode == "SUPER_ADMIN",
                    currentUserId = user?.id ?: 0,
                    canCreate = hasPermission(user, USERS_MODULE, PermissionAction.CREATE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = userRepository.getUsers()) {
                is Resource.Success -> _state.update {
                    it.copy(users = result.data, isLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun requestDelete(user: User) {
        _state.update { it.copy(deleteTarget = user) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (userRepository.deleteUser(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        users = it.users.filterNot { u -> u.id == target.id },
                        userMessage = "Utilisateur supprimé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer cet utilisateur"
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
