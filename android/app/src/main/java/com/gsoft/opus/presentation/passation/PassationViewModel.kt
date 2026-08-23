package com.gsoft.opus.presentation.passation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Passation
import com.gsoft.opus.domain.repository.PassationRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PassationUiState(
    val passations: List<Passation> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Passation? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<Passation>
        get() {
            val query = searchQuery.trim().lowercase()
            return passations.filter { p ->
                query.isEmpty() ||
                    p.chefDescendantLastname?.lowercase()?.contains(query) == true ||
                    p.chefMontantLastname?.lowercase()?.contains(query) == true ||
                    p.instructionsAutorite?.lowercase()?.contains(query) == true ||
                    p.incidentsSurvenus?.lowercase()?.contains(query) == true
            }
        }
}

@HiltViewModel
class PassationViewModel @Inject constructor(
    private val passationRepository: PassationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PassationUiState())
    val state: StateFlow<PassationUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, PASSATION_MODULE, PermissionAction.CREATE),
                    canDelete = hasPermission(user, PASSATION_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = passationRepository.getPassationList()) {
                is Resource.Success -> _state.update {
                    it.copy(passations = result.data, isLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun requestDelete(passation: Passation) {
        _state.update { it.copy(deleteTarget = passation) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (passationRepository.deletePassation(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        passations = it.passations.filterNot { p -> p.id == target.id },
                        userMessage = "Passation supprimée avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer cette passation"
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
