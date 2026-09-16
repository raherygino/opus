package com.gsoft.opus.presentation.convocation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Convocation
import com.gsoft.opus.domain.model.ConvocationAttachment
import com.gsoft.opus.domain.repository.ConvocationRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConvocationDetailUiState(
    val entry: Convocation? = null,
    val attachments: List<ConvocationAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val canCreate: Boolean = false,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class ConvocationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val convocationRepository: ConvocationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("convocationId") ?: 0

    private val _state = MutableStateFlow(ConvocationDetailUiState())
    val state: StateFlow<ConvocationDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canEdit = hasPermission(user, PJ_CONVOCATION_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, PJ_CONVOCATION_MODULE, PermissionAction.DELETE),
                    canCreate = hasPermission(user, PJ_CONVOCATION_MODULE, PermissionAction.CREATE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = convocationRepository.getConvocation(entryId)) {
                is Resource.Success -> {
                    val entry = result.data
                    _state.update { it.copy(entry = entry) }
                    val attachmentsDeferred = async { convocationRepository.getConvocationAttachments(entryId) }
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
