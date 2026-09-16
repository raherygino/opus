package com.gsoft.opus.presentation.renseignementpj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.RenseignementPj
import com.gsoft.opus.domain.repository.RenseignementPjRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RenseignementPjListUiState(
    val items: List<RenseignementPj> = emptyList(),
    val filtered: List<RenseignementPj> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: RenseignementPj? = null,
    val userMessage: String? = null
)

@HiltViewModel
class RenseignementPjViewModel @Inject constructor(
    private val repository: RenseignementPjRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RenseignementPjListUiState())
    val state: StateFlow<RenseignementPjListUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, PJ_RENSEIGNEMENT_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, PJ_RENSEIGNEMENT_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, PJ_RENSEIGNEMENT_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getRenseignementPjList()) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = result.data,
                            filtered = applyFilters(result.data, it.searchQuery)
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                filtered = applyFilters(it.items, query)
            )
        }
    }

    private fun applyFilters(items: List<RenseignementPj>, query: String): List<RenseignementPj> {
        if (query.isBlank()) return items
        val q = query.lowercase()
        return items.filter {
            it.natureInfraction.contains(q, ignoreCase = true) ||
            (it.dateLieuFaits?.contains(q, ignoreCase = true) ?: false) ||
            (it.circonstances?.contains(q, ignoreCase = true) ?: false)
        }
    }

    fun requestDelete(item: RenseignementPj) {
        _state.update { it.copy(deleteTarget = item) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            when (val res = repository.deleteRenseignementPj(target.id)) {
                is Resource.Success -> {
                    _state.update { it.copy(deleteTarget = null, userMessage = "Renseignement supprimé") }
                    refresh()
                }
                is Resource.Error -> _state.update {
                    it.copy(deleteTarget = null, userMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
