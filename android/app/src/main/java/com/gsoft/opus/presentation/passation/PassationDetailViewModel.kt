package com.gsoft.opus.presentation.passation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Passation
import com.gsoft.opus.domain.model.PassationAttachment
import com.gsoft.opus.domain.repository.PassationRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PassationDetailUiState(
    val passation: Passation? = null,
    val attachments: List<PassationAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class PassationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val passationRepository: PassationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val passationId: Int = savedStateHandle.get<Int>("passationId") ?: 0

    private val _state = MutableStateFlow(PassationDetailUiState())
    val state: StateFlow<PassationDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canEdit = hasPermission(user, PASSATION_MODULE, PermissionAction.EDIT)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = passationRepository.getPassation(passationId)) {
                is Resource.Success -> {
                    _state.update { it.copy(passation = result.data) }
                    val attachmentsDeferred = async { passationRepository.getAttachments(passationId) }
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
