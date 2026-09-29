package com.gsoft.opus.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSaving: Boolean = false,
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun updateCurrentPassword(value: String) = _state.update {
        it.copy(currentPassword = value, currentPasswordError = null, errorMessage = null)
    }

    fun updateNewPassword(value: String) = _state.update {
        it.copy(newPassword = value, newPasswordError = null, errorMessage = null)
    }

    fun updateConfirmPassword(value: String) = _state.update {
        it.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null)
    }

    fun changePassword() {
        val current = _state.value
        if (current.isSaving) return

        var hasError = false
        var currentError: String? = null
        var newError: String? = null
        var confirmError: String? = null

        if (current.currentPassword.isBlank()) {
            currentError = "Le mot de passe actuel est requis"
            hasError = true
        }
        if (current.newPassword.isBlank()) {
            newError = "Le nouveau mot de passe est requis"
            hasError = true
        } else if (current.newPassword.length < 6) {
            newError = "Le nouveau mot de passe doit contenir au moins 6 caractères"
            hasError = true
        } else if (current.newPassword == current.currentPassword) {
            newError = "Le nouveau mot de passe doit être différent de l'actuel"
            hasError = true
        }
        if (current.confirmPassword != current.newPassword) {
            confirmError = "La confirmation ne correspond pas au nouveau mot de passe"
            hasError = true
        }

        if (hasError) {
            _state.update {
                it.copy(
                    currentPasswordError = currentError,
                    newPasswordError = newError,
                    confirmPasswordError = confirmError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            when (val result = authRepository.changePassword(current.currentPassword, current.newPassword)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isSaving = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        successMessage = "Mot de passe modifié avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    // The API flags a wrong current password under the
                    // current_password key — surface it on that field.
                    val msg = result.message
                    if (msg.contains("current_password") || msg.contains("actuel", ignoreCase = true)) {
                        it.copy(isSaving = false, currentPasswordError = "Mot de passe actuel incorrect")
                    } else {
                        it.copy(isSaving = false, errorMessage = msg)
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
