package com.gsoft.opus.presentation.personnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Comportement
import com.gsoft.opus.domain.repository.ComportementRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Status filter for the comportement list. "" = all. */
typealias ComportementStatusFilter = String

data class ComportementUiState(
    val comportements: List<Comportement> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val statusFilter: ComportementStatusFilter = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val isAdmin: Boolean = false,
    val deleteTarget: Comportement? = null,
    val isDeleting: Boolean = false,
    val detailTarget: Comportement? = null,
    val confirmTarget: Comportement? = null,
    val isConfirming: Boolean = false,
    val rejectTarget: Comportement? = null,
    val rejectReason: String = "",
    val isRejecting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<Comportement>
        get() {
            val query = searchQuery.trim().lowercase()
            return comportements.filter { c ->
                if (query.isNotEmpty()) {
                    val matchesQuery = c.im.lowercase().contains(query) ||
                        (c.nom?.lowercase()?.contains(query) == true) ||
                        (c.prenoms?.lowercase()?.contains(query) == true) ||
                        c.type.lowercase().contains(query) ||
                        c.motif.lowercase().contains(query)
                    if (!matchesQuery) return@filter false
                }
                true
            }
        }
}

@HiltViewModel
class ComportementViewModel @Inject constructor(
    private val comportementRepository: ComportementRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ComportementUiState())
    val state: StateFlow<ComportementUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            val isAdmin = user?.roleCode == "SUPER_ADMIN" || user?.roleCode == "STATION_ADMIN"
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, "personnel", PermissionAction.CREATE),
                    canDelete = hasPermission(user, "personnel", PermissionAction.DELETE),
                    isAdmin = isAdmin
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val statusFilter = _state.value.statusFilter
            val statusParam = statusFilter.ifBlank { null }
            when (val result = comportementRepository.getComportementList(status = statusParam)) {
                is Resource.Success -> _state.update {
                    it.copy(comportements = result.data, isLoading = false)
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

    fun setStatusFilter(filter: ComportementStatusFilter) {
        _state.update { it.copy(statusFilter = filter) }
        refresh()
    }

    fun openDetail(comportement: Comportement) {
        _state.update { it.copy(detailTarget = comportement) }
    }

    fun closeDetail() {
        _state.update { it.copy(detailTarget = null) }
    }

    fun requestDelete(comportement: Comportement) {
        _state.update { it.copy(deleteTarget = comportement) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (comportementRepository.deleteComportement(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        detailTarget = null,
                        comportements = it.comportements.filterNot { c -> c.id == target.id },
                        userMessage = "Comportement supprimé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer ce comportement"
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun requestConfirm(comportement: Comportement) {
        _state.update { it.copy(confirmTarget = comportement) }
    }

    fun cancelConfirm() {
        _state.update { it.copy(confirmTarget = null) }
    }

    fun confirmComportement() {
        val target = _state.value.confirmTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isConfirming = true) }
            when (val result = comportementRepository.confirmComportement(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isConfirming = false,
                        confirmTarget = null,
                        detailTarget = null,
                        comportements = it.comportements.map { c -> if (c.id == target.id) result.data else c },
                        userMessage = "Comportement confirmé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isConfirming = false,
                        confirmTarget = null,
                        userMessage = result.message ?: "Impossible de confirmer ce comportement"
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun requestReject(comportement: Comportement) {
        _state.update { it.copy(rejectTarget = comportement, rejectReason = "") }
    }

    fun cancelReject() {
        _state.update { it.copy(rejectTarget = null, rejectReason = "") }
    }

    fun onRejectReasonChange(value: String) {
        _state.update { it.copy(rejectReason = value) }
    }

    fun rejectComportement() {
        val target = _state.value.rejectTarget ?: return
        val reason = _state.value.rejectReason.trim().ifBlank { null }
        viewModelScope.launch {
            _state.update { it.copy(isRejecting = true) }
            when (val result = comportementRepository.rejectComportement(target.id, reason)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isRejecting = false,
                        rejectTarget = null,
                        rejectReason = "",
                        detailTarget = null,
                        comportements = it.comportements.map { c -> if (c.id == target.id) result.data else c },
                        userMessage = "Comportement rejeté"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isRejecting = false,
                        rejectTarget = null,
                        userMessage = result.message ?: "Impossible de rejeter ce comportement"
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
