package com.gsoft.opus.presentation.renseignementpj

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.RenseignementPj
import com.gsoft.opus.domain.model.RenseignementPjAttachment
import com.gsoft.opus.domain.repository.RenseignementPjRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RenseignementPjDetailUiState(
    val entry: RenseignementPj? = null,
    val attachments: List<RenseignementPjAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class RenseignementPjDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RenseignementPjRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("renseignementId") ?: 0

    private val _state = MutableStateFlow(RenseignementPjDetailUiState())
    val state: StateFlow<RenseignementPjDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(canEdit = hasPermission(user, PJ_RENSEIGNEMENT_MODULE, PermissionAction.EDIT))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getRenseignementPj(entryId)) {
                is Resource.Success -> {
                    _state.update { it.copy(entry = result.data) }
                    val attsDeferred = async { repository.getRenseignementPjAttachments(entryId) }
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
