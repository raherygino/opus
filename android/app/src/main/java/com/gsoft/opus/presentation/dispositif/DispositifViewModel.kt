package com.gsoft.opus.presentation.dispositif

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.DispositifExceptionnel
import com.gsoft.opus.domain.repository.DispositifExceptionnelRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DispositifListUiState(
    val entries: List<DispositifExceptionnel> = emptyList(),
    val filteredEntries: List<DispositifExceptionnel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val pendingDelete: DispositifExceptionnel? = null,
    val isDeleting: Boolean = false
)

@HiltViewModel
class DispositifViewModel @Inject constructor(
    private val repository: DispositifExceptionnelRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DispositifListUiState())
    val state: StateFlow<DispositifListUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, DISPOSITIF_MODULE, PermissionAction.CREATE),
                    canDelete = hasPermission(user, DISPOSITIF_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getDispositifList()) {
                is Resource.Success -> _state.update {
                    it.copy(
                        entries = result.data,
                        filteredEntries = filter(result.data, it.searchQuery),
                        isLoading = false
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query, filteredEntries = filter(it.entries, query)) }
    }

    private fun filter(entries: List<DispositifExceptionnel>, query: String): List<DispositifExceptionnel> {
        if (query.isBlank()) return entries
        val q = query.lowercase()
        return entries.filter {
            it.natureEvenement.lowercase().contains(q) ||
                it.dateDebut.contains(q) ||
                it.dateFin.contains(q) ||
                it.agentDisplayName.lowercase().contains(q)
        }
    }

    fun requestDelete(entry: DispositifExceptionnel) {
        _state.update { it.copy(pendingDelete = entry) }
    }

    fun cancelDelete() {
        _state.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val entry = _state.value.pendingDelete ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (val result = repository.deleteDispositif(entry.id)) {
                is Resource.Success -> {
                    _state.update { it.copy(isDeleting = false, pendingDelete = null) }
                    refresh()
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(isDeleting = false, pendingDelete = null, userMessage = result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
