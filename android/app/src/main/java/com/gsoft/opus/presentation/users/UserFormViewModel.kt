package com.gsoft.opus.presentation.users

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.Role
import com.gsoft.opus.domain.repository.RoleRepository
import com.gsoft.opus.domain.repository.UserFormData
import com.gsoft.opus.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val personnelId: Int = 0,
    val availablePersonnel: List<Personnel> = emptyList(),
    val username: String = "",
    val password: String = "",
    val roleId: Int = 0,
    val roles: List<Role> = emptyList(),
    val isActive: Boolean = true,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class UserFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val userId: Int = savedStateHandle.get<Int>("userId") ?: 0

    private val _state = MutableStateFlow(UserFormUiState(isEdit = userId > 0))
    val state: StateFlow<UserFormUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(isLoading = true) }
        loadRoles()
        if (userId > 0) {
            loadUser()
        } else {
            loadAvailablePersonnel()
        }
    }

    private fun loadRoles() {
        viewModelScope.launch {
            when (val result = roleRepository.getRoles()) {
                is Resource.Success -> _state.update { it.copy(roles = result.data) }
                is Resource.Error -> _state.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadAvailablePersonnel() {
        viewModelScope.launch {
            when (val result = userRepository.getAvailablePersonnel()) {
                is Resource.Success -> _state.update {
                    it.copy(availablePersonnel = result.data, isLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            when (val result = userRepository.getUser(userId)) {
                is Resource.Success -> {
                    val u = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            personnelId = u.personnelId ?: 0,
                            username = u.username,
                            roleId = u.roleId ?: 0,
                            isActive = u.isActive
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

    fun updatePersonnelId(id: Int) { _state.update { it.copy(personnelId = id) } }
    fun updateUsername(v: String) { _state.update { it.copy(username = v) } }
    fun updatePassword(v: String) { _state.update { it.copy(password = v) } }
    fun updateRoleId(id: Int) { _state.update { it.copy(roleId = id) } }
    fun updateIsActive(v: Boolean) { _state.update { it.copy(isActive = v) } }

    fun save() {
        val s = _state.value
        val error = UserFormValidator.validate(
            isEdit = s.isEdit,
            personnelId = s.personnelId,
            username = s.username,
            password = s.password,
            roleId = s.roleId
        )
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = UserFormData(
                personnelId = s.personnelId.takeIf { it > 0 },
                username = s.username,
                password = s.password.takeIf { it.isNotBlank() },
                roleId = s.roleId,
                isActive = if (s.isEdit) s.isActive else null
            )
            val result = if (s.isEdit) {
                userRepository.updateUser(userId, data)
            } else {
                userRepository.createUser(data)
            }
            when (result) {
                is Resource.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissError() { _state.update { it.copy(errorMessage = null) } }
}
