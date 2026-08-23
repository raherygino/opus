package com.gsoft.opus.presentation.declarationperte

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.DeclarationPerte
import com.gsoft.opus.domain.model.DeclarationPerteAttachment
import com.gsoft.opus.domain.repository.DeclarationPerteRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeclarationPerteDetailUiState(
    val declaration: DeclarationPerte? = null,
    val attachments: List<DeclarationPerteAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class DeclarationPerteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val declarationPerteRepository: DeclarationPerteRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val declarationId: Int = savedStateHandle.get<Int>("declarationId") ?: 0

    private val _state = MutableStateFlow(DeclarationPerteDetailUiState())
    val state: StateFlow<DeclarationPerteDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canEdit = hasPermission(user, DECLARATION_PERTE_MODULE, PermissionAction.EDIT)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = declarationPerteRepository.getDeclarationPerte(declarationId)) {
                is Resource.Success -> {
                    _state.update { it.copy(declaration = result.data) }
                    val attachmentsDeferred = async { declarationPerteRepository.getAttachments(declarationId) }
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
