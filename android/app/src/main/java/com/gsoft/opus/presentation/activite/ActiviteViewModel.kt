package com.gsoft.opus.presentation.activite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Activite
import com.gsoft.opus.domain.repository.ActiviteRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiviteUiState(
    val entries: List<Activite> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Activite? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<Activite>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return entries
            return entries.filter { e ->
                e.patrouilleSummary.lowercase().contains(query) ||
                    (e.operationCiblee ?: "").lowercase().contains(query) ||
                    (e.faitsConstates ?: "").lowercase().contains(query) ||
                    (e.natureIntervention ?: "").lowercase().contains(query) ||
                    (e.suitesDonnees ?: "").lowercase().contains(query)
            }
        }
}

@HiltViewModel
class ActiviteViewModel @Inject constructor(
    private val repository: ActiviteRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ActiviteUiState())
    val state: StateFlow<ActiviteUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, ACTIVITE_MODULE, PermissionAction.CREATE),
                    canDelete = hasPermission(user, ACTIVITE_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getActiviteList()) {
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

    fun requestDelete(entry: Activite) {
        _state.update { it.copy(deleteTarget = entry) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (repository.deleteActivite(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        entries = it.entries.filterNot { e -> e.id == target.id },
                        userMessage = "Activité supprimée avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer cette activité"
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
