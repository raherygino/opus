package com.gsoft.opus.presentation.requisition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Requisition
import com.gsoft.opus.domain.repository.RequisitionRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequisitionListUiState(
    val items: List<Requisition> = emptyList(),
    val filtered: List<Requisition> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Requisition? = null,
    val userMessage: String? = null
)

@HiltViewModel
class RequisitionViewModel @Inject constructor(
    private val requisitionRepository: RequisitionRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RequisitionListUiState())
    val state: StateFlow<RequisitionListUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, PJ_REQUISITION_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, PJ_REQUISITION_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, PJ_REQUISITION_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = requisitionRepository.getRequisitionList()) {
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

    private fun applyFilters(items: List<Requisition>, query: String): List<Requisition> {
        if (query.isBlank()) return items
        val q = query.lowercase()
        return items.filter {
            it.numero.contains(q, ignoreCase = true) ||
            it.affaire.contains(q, ignoreCase = true) ||
            (it.nomSubstitut?.contains(q, ignoreCase = true) ?: false) ||
            (it.opj?.contains(q, ignoreCase = true) ?: false)
        }
    }

    fun requestDelete(item: Requisition) {
        _state.update { it.copy(deleteTarget = item) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            when (val res = requisitionRepository.deleteRequisition(target.id)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(deleteTarget = null, userMessage = "Réquisition supprimée")
                    }
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
