package com.gsoft.opus.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) {
        _state.update {
            it.copy(username = value, usernameError = null, errorMessage = null)
        }
    }

    fun onPasswordChange(value: String) {
        _state.update {
            it.copy(password = value, passwordError = null, errorMessage = null)
        }
    }

    fun onPasswordVisibilityToggle() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRememberMeToggle() {
        _state.update { it.copy(rememberMe = !it.rememberMe) }
    }

    fun onDismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun login() {
        val current = _state.value
        if (current.isLoading) return

        var hasError = false
        var usernameError: String? = null
        var passwordError: String? = null

        if (current.username.isBlank()) {
            usernameError = "Username is required"
            hasError = true
        }
        if (current.password.isBlank()) {
            passwordError = "Password is required"
            hasError = true
        }

        if (hasError) {
            _state.update { it.copy(usernameError = usernameError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = loginUseCase(current.username, current.password, current.rememberMe)
            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, isLoginSuccess = true) }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }
}
