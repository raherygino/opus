package com.gsoft.opus.presentation.plainte

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.PlainteSortie
import com.gsoft.opus.domain.model.PlainteSortieAttachment
import com.gsoft.opus.domain.repository.PlainteRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlainteSortieDetailUiState(
    val sortie: PlainteSortie? = null,
    val attachments: List<PlainteSortieAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class PlainteSortieDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val plainteRepository: PlainteRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val sortieId: Int = savedStateHandle.get<Int>("plainteSortieId") ?: 0

    private val _state = MutableStateFlow(PlainteSortieDetailUiState())
    val state: StateFlow<PlainteSortieDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canEdit = hasPermission(user, PJ_PLAINTE_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, PJ_PLAINTE_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = plainteRepository.getPlainteSortie(sortieId)) {
                is Resource.Success -> {
                    val sortie = result.data
                    _state.update { it.copy(sortie = sortie) }
                    val attachmentsDeferred = async { plainteRepository.getSortieAttachments(sortieId) }
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
