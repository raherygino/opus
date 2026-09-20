package com.gsoft.opus.presentation.rassemblement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.RassemblementJournalier
import com.gsoft.opus.domain.repository.RassemblementJournalierRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RassemblementUiState(
    val entries: List<RassemblementJournalier> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: RassemblementJournalier? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<RassemblementJournalier>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return entries
            return entries.filter { e ->
                e.brigadeService.lowercase().contains(query) ||
                    (e.officierPermanence ?: "").lowercase().contains(query) ||
                    (e.inspecteurPermanence ?: "").lowercase().contains(query) ||
                    (e.chefPoste ?: "").lowercase().contains(query)
            }
        }
}

@HiltViewModel
class RassemblementViewModel @Inject constructor(
    private val repository: RassemblementJournalierRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RassemblementUiState())
    val state: StateFlow<RassemblementUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, RASSEMBLEMENT_MODULE, PermissionAction.CREATE),
                    canDelete = hasPermission(user, RASSEMBLEMENT_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getRassemblementList()) {
                is Resource.Success -> _state.update {
                    it.copy(entries = result.data, isLoading = false)
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

    fun requestDelete(entry: RassemblementJournalier) {
        _state.update { it.copy(deleteTarget = entry) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (repository.deleteRassemblement(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        entries = it.entries.filterNot { e -> e.id == target.id },
                        userMessage = "Rassemblement supprimé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer ce rassemblement"
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
