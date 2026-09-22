package com.gsoft.opus.presentation.evenement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.EvenementSurvenu
import com.gsoft.opus.domain.repository.EvenementSurvenuRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EvenementUiState(
    val entries: List<EvenementSurvenu> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: EvenementSurvenu? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<EvenementSurvenu>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return entries
            return entries.filter { e ->
                e.lieuExact.lowercase().contains(query) ||
                    e.typeLabel.lowercase().contains(query) ||
                    (e.auteursPresumes ?: "").lowercase().contains(query) ||
                    (e.victimes ?: "").lowercase().contains(query) ||
                    (e.temoins ?: "").lowercase().contains(query)
            }
        }
}

@HiltViewModel
class EvenementViewModel @Inject constructor(
    private val repository: EvenementSurvenuRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EvenementUiState())
    val state: StateFlow<EvenementUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, EVENEMENT_MODULE, PermissionAction.CREATE),
                    canDelete = hasPermission(user, EVENEMENT_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getEvenementList()) {
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

    fun requestDelete(entry: EvenementSurvenu) {
        _state.update { it.copy(deleteTarget = entry) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (repository.deleteEvenement(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        entries = it.entries.filterNot { e -> e.id == target.id },
                        userMessage = "Évènement supprimé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer cet évènement"
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
