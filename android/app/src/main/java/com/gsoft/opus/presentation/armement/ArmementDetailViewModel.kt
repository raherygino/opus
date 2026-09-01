package com.gsoft.opus.presentation.armement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Armement
import com.gsoft.opus.domain.model.ArmementAttachment
import com.gsoft.opus.domain.repository.ArmementRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArmementDetailUiState(
    val armement: Armement? = null,
    val attachments: List<ArmementAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class ArmementDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val armementRepository: ArmementRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val armementId: Int = savedStateHandle.get<Int>("armementId") ?: 0

    private val _state = MutableStateFlow(ArmementDetailUiState())
    val state: StateFlow<ArmementDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canEdit = hasPermission(user, ARMEMENT_MODULE, PermissionAction.EDIT)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = armementRepository.getArmement(armementId)) {
                is Resource.Success -> {
                    _state.update { it.copy(armement = result.data) }
                    val attachmentsDeferred = async { armementRepository.getAttachments(armementId) }
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

    // ─── Réintégration ──────────────────────────────────────────────

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
