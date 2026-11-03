package com.gsoft.opus.presentation.roles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.ModulePermissions
import com.gsoft.opus.domain.repository.RoleFormData
import com.gsoft.opus.domain.repository.RoleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoleFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val code: String = "",
    val name: String = "",
    val description: String = "",
    /** module key → flags; covers every module in [RoleModules.MODULES]. */
    val permissions: Map<String, ModulePermissions> = emptyMap(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class RoleFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val roleId: Int = savedStateHandle.get<Int>("roleId") ?: 0

    private val _state = MutableStateFlow(
        RoleFormUiState(
            isEdit = roleId > 0,
            permissions = RoleModules.emptyPermissions()
        )
    )
    val state: StateFlow<RoleFormUiState> = _state.asStateFlow()

    init {
        if (roleId > 0) loadRole()
    }

    private fun loadRole() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = roleRepository.getRole(roleId)) {
                is Resource.Success -> {
                    val r = result.data
                    val perms = RoleModules.emptyPermissions().toMutableMap()
                    r.permissions.forEach { p ->
                        perms[p.module]?.let {
                            perms[p.module] = ModulePermissions(
                                canView = p.canView,
                                canCreate = p.canCreate,
                                canEdit = p.canEdit,
                                canDelete = p.canDelete,
                                canExport = p.canExport
                            )
                        }
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            code = r.code,
                            name = r.name,
                            description = r.description ?: "",
                            permissions = perms
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateName(v: String) { _state.update { it.copy(name = v) } }
    fun updateDescription(v: String) { _state.update { it.copy(description = v) } }

    fun togglePermission(module: String, action: PermissionAction) {
        _state.update { s ->
            val cur = s.permissions[module] ?: ModulePermissions.NONE
            val next = when (action) {
                PermissionAction.VIEW -> cur.copy(canView = !cur.canView)
                PermissionAction.CREATE -> cur.copy(canCreate = !cur.canCreate)
                PermissionAction.EDIT -> cur.copy(canEdit = !cur.canEdit)
                PermissionAction.DELETE -> cur.copy(canDelete = !cur.canDelete)
                PermissionAction.EXPORT -> cur.copy(canExport = !cur.canExport)
            }
            s.copy(permissions = s.permissions + (module to next))
        }
    }

    /** Set or clear every action of a module ("Tous" / "Aucun"). */
    fun setAllPermissions(module: String, grant: Boolean) {
        _state.update { s ->
            s.copy(permissions = s.permissions + (module to if (grant) ModulePermissions.ALL else ModulePermissions.NONE))
        }
    }

    fun save() {
        val s = _state.value
        val error = RoleFormValidator.validate(s.name)
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = RoleFormData(
                name = s.name,
                description = s.description.trim().ifBlank { null }
            )
            if (s.isEdit) {
                val result = roleRepository.updateRole(roleId, data)
                if (result is Resource.Error) {
                    _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                    return@launch
                }
                savePermissions(roleId)
            } else {
                when (val result = roleRepository.createRole(data)) {
                    is Resource.Success -> savePermissions(result.data.id)
                    is Resource.Error -> _state.update {
                        it.copy(isSaving = false, errorMessage = result.message)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private suspend fun savePermissions(id: Int) {
        when (val result = roleRepository.updateRolePermissions(id, _state.value.permissions)) {
            is Resource.Success -> _state.update { it.copy(isSaving = false, saved = true) }
            is Resource.Error -> _state.update {
                it.copy(isSaving = false, errorMessage = result.message)
            }
            is Resource.Loading -> {}
        }
    }

    fun dismissError() { _state.update { it.copy(errorMessage = null) } }
}
