package com.gsoft.opus.presentation.correspondance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Correspondance
import com.gsoft.opus.domain.model.CorrespondanceAttachment
import com.gsoft.opus.domain.repository.CorrespondanceRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CorrespondanceDetailUiState(
    val correspondance: Correspondance? = null,
    val attachments: List<CorrespondanceAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class CorrespondanceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val correspondanceRepository: CorrespondanceRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val correspondanceId: Int = savedStateHandle.get<Int>("correspondanceId") ?: 0

    private val _state = MutableStateFlow(CorrespondanceDetailUiState())
    val state: StateFlow<CorrespondanceDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canEdit = hasPermission(user, CORRESPONDANCE_MODULE, PermissionAction.EDIT)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = correspondanceRepository.getCorrespondance(correspondanceId)) {
                is Resource.Success -> {
                    _state.update { it.copy(correspondance = result.data) }
                    val attachmentsDeferred = async { correspondanceRepository.getAttachments(correspondanceId) }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            attachments = attachmentsDeferred.await().getOrNull() ?: emptyList()
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message, notFound = true)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
