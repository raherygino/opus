package com.gsoft.opus.presentation.objet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.ObjetSaisi
import com.gsoft.opus.domain.model.ObjetSaisiAttachment
import com.gsoft.opus.domain.repository.ObjetSaisiRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObjetSaisiDetailUiState(
    val entry: ObjetSaisi? = null,
    val attachments: List<ObjetSaisiAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class ObjetSaisiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ObjetSaisiRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("objetId") ?: 0

    private val _state = MutableStateFlow(ObjetSaisiDetailUiState())
    val state: StateFlow<ObjetSaisiDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(canEdit = hasPermission(user, PJ_OBJETS_MODULE, PermissionAction.EDIT))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getObjetSaisi(entryId)) {
                is Resource.Success -> {
                    _state.update { it.copy(entry = result.data) }
                    val attsDeferred = async { repository.getObjetSaisiAttachments(entryId) }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            attachments = attsDeferred.await().getOrNull() ?: emptyList()
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
