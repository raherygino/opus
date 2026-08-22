package com.gsoft.opus.presentation.correspondance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Correspondance
import com.gsoft.opus.domain.repository.CorrespondanceRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val CORRESPONDANCE_MODULE = "sedentaire_secretariat_correspondance"

data class CorrespondanceUiState(
    val correspondances: List<Correspondance> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val sensFilter: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Correspondance? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<Correspondance>
        get() {
            val query = searchQuery.trim().lowercase()
            return correspondances.filter { c ->
                val matchesSens = sensFilter.isEmpty() || c.sens == sensFilter
                val matchesQuery = query.isEmpty() ||
                    c.reference.lowercase().contains(query) ||
                    c.emetteurDestinataire.lowercase().contains(query) ||
                    c.objet.lowercase().contains(query)
                matchesSens && matchesQuery
            }
        }
}

@HiltViewModel
class CorrespondanceViewModel @Inject constructor(
    private val correspondanceRepository: CorrespondanceRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CorrespondanceUiState())
    val state: StateFlow<CorrespondanceUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, CORRESPONDANCE_MODULE, PermissionAction.CREATE),
                    canDelete = hasPermission(user, CORRESPONDANCE_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = correspondanceRepository.getCorrespondanceList()) {
                is Resource.Success -> _state.update {
                    it.copy(correspondances = result.data, isLoading = false)
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

    fun setSensFilter(sens: String) {
        _state.update { it.copy(sensFilter = sens) }
    }

    fun requestDelete(correspondance: Correspondance) {
        _state.update { it.copy(deleteTarget = correspondance) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (correspondanceRepository.deleteCorrespondance(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        correspondances = it.correspondances.filterNot { c -> c.id == target.id },
                        userMessage = "Correspondance supprimée avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer cette correspondance"
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
