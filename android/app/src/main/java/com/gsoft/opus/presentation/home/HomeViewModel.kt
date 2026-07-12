package com.gsoft.opus.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import com.gsoft.opus.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val username: String = "",
    val firstName: String? = null,
    val lastName: String? = null,
    val personnelId: Int? = null,
    val photo: String? = null,
    val roleName: String? = null,
    val grade: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(isLoading = true))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val result = getCurrentUserUseCase()
            val user = result.getOrNull()
            _state.update {
                it.copy(
                    username = user?.username ?: "User",
                    firstName = user?.firstName,
                    lastName = user?.lastName,
                    personnelId = user?.personnelId,
                    photo = user?.photo,
                    roleName = user?.roleName,
                    grade = user?.grade,
                    isLoading = false
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
