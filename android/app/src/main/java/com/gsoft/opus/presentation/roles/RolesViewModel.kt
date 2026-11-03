package com.gsoft.opus.presentation.roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Role
import com.gsoft.opus.domain.repository.RoleRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ROLES_MODULE = "roles"

data class RolesUiState(
    val roles: List<Role> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Role? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<Role>
        get() {
            val query = searchQuery.trim().lowercase()
            return if (query.isEmpty()) roles else roles.filter { r ->
                r.name.lowercase().contains(query) ||
                    r.code.lowercase().contains(query) ||
                    (r.description?.lowercase()?.contains(query) == true)
            }
        }
}

@HiltViewModel
class RolesViewModel @Inject constructor(
    private val roleRepository: RoleRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RolesUiState())
    val state: StateFlow<RolesUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, ROLES_MODULE, PermissionAction.CREATE),
                    canDelete = hasPermission(user, ROLES_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = roleRepository.getRoles()) {
                is Resource.Success -> _state.update {
                    it.copy(roles = result.data, isLoading = false)
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

    fun requestDelete(role: Role) {
        _state.update { it.copy(deleteTarget = role) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (roleRepository.deleteRole(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        roles = it.roles.filterNot { r -> r.id == target.id },
                        userMessage = "Rôle supprimé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer ce rôle"
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
