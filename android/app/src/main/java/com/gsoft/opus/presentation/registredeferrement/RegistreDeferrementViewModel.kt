package com.gsoft.opus.presentation.registredeferrement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.RegistreDeferrement
import com.gsoft.opus.domain.repository.RegistreDeferrementRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegistreDeferrementListUiState(
    val items: List<RegistreDeferrement> = emptyList(),
    val filtered: List<RegistreDeferrement> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: RegistreDeferrement? = null,
    val userMessage: String? = null
)

@HiltViewModel
class RegistreDeferrementViewModel @Inject constructor(
    private val repository: RegistreDeferrementRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegistreDeferrementListUiState())
    val state: StateFlow<RegistreDeferrementListUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, PJ_DEFERREMENT_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, PJ_DEFERREMENT_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, PJ_DEFERREMENT_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getDeferrementList()) {
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

    private fun applyFilters(items: List<RegistreDeferrement>, query: String): List<RegistreDeferrement> {
        if (query.isBlank()) return items
        return items.filter {
            it.numero.contains(query, ignoreCase = true) ||
            it.personneNom.contains(query, ignoreCase = true) ||
            (it.infraction?.contains(query, ignoreCase = true) ?: false) ||
            (it.autorite?.contains(query, ignoreCase = true) ?: false) ||
            (it.numeroDossier?.contains(query, ignoreCase = true) ?: false)
        }
    }

    fun requestDelete(item: RegistreDeferrement) {
        _state.update { it.copy(deleteTarget = item) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            when (val res = repository.deleteDeferrement(target.id)) {
                is Resource.Success -> {
                    _state.update { it.copy(deleteTarget = null, userMessage = "Entrée supprimée") }
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
