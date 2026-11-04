package com.gsoft.opus.presentation.evenement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.EditWindow
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.EvenementSurvenu
import com.gsoft.opus.domain.model.EvenementSurvenuAttachment
import com.gsoft.opus.domain.repository.EvenementSurvenuRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EvenementDetailUiState(
    val entry: EvenementSurvenu? = null,
    val attachments: List<EvenementSurvenuAttachment> = emptyList(),
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
class EvenementDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EvenementSurvenuRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("evenementId") ?: 0

    private val _state = MutableStateFlow(EvenementDetailUiState())
    val state: StateFlow<EvenementDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canEditPermission = hasPermission(user, EVENEMENT_MODULE, PermissionAction.EDIT),
                    isAdmin = EditWindow.isAdmin(user)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getEvenement(entryId)) {
                is Resource.Success -> {
                    _state.update { it.copy(entry = result.data) }
                    val attachmentsDeferred = async { repository.getAttachments(entryId) }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            attachments = attachmentsDeferred.await().getOrNull() ?: emptyList()
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

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
