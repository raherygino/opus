package com.gsoft.opus.presentation.dispositif

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.EditWindow
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.DispositifExceptionnel
import com.gsoft.opus.domain.repository.DispositifExceptionnelRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DispositifDetailUiState(
    val entry: DispositifExceptionnel? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEditPermission: Boolean = false,
    val isAdmin: Boolean = false,
    val userMessage: String? = null
) {
    val canEdit: Boolean
        get() = canEditPermission && (isAdmin || EditWindow.isWithinWindow(entry?.createdAt))
}

@HiltViewModel
class DispositifDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DispositifExceptionnelRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("dispositifId") ?: 0

    private val _state = MutableStateFlow(DispositifDetailUiState())
    val state: StateFlow<DispositifDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canEditPermission = hasPermission(user, DISPOSITIF_MODULE, PermissionAction.EDIT),
                    isAdmin = EditWindow.isAdmin(user)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getDispositif(entryId)) {
                is Resource.Success -> _state.update {
                    it.copy(entry = result.data, isLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
