package com.gsoft.opus.presentation.mandat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Mandat
import com.gsoft.opus.domain.repository.MandatRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MandatListUiState(
    val items: List<Mandat> = emptyList(),
    val filtered: List<Mandat> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val typeFilter: String? = null,
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Mandat? = null,
    val userMessage: String? = null
)

@HiltViewModel
class MandatViewModel @Inject constructor(
    private val repository: MandatRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MandatListUiState())
    val state: StateFlow<MandatListUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, PJ_MANDAT_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, PJ_MANDAT_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, PJ_MANDAT_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getMandatList()) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = result.data,
                            filtered = applyFilters(result.data, it.searchQuery, it.typeFilter)
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
                filtered = applyFilters(it.items, query, it.typeFilter)
            )
        }
    }

    fun setTypeFilter(type: String?) {
        _state.update {
            it.copy(
                typeFilter = type,
                filtered = applyFilters(it.items, it.searchQuery, type)
            )
        }
    }

    private fun applyFilters(items: List<Mandat>, query: String, type: String?): List<Mandat> {
        var result = items
        if (type != null) {
            result = result.filter { it.type == type }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter {
                it.numero.contains(q, ignoreCase = true) ||
                it.personneNom.contains(q, ignoreCase = true) ||
                (it.autorite?.contains(q, ignoreCase = true) ?: false) ||
                (it.qualificationInfraction?.contains(q, ignoreCase = true) ?: false)
            }
        }
        return result
    }

    fun requestDelete(item: Mandat) {
        _state.update { it.copy(deleteTarget = item) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            when (val res = repository.deleteMandat(target.id)) {
                is Resource.Success -> {
                    _state.update { it.copy(deleteTarget = null, userMessage = "Mandat supprimé") }
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
