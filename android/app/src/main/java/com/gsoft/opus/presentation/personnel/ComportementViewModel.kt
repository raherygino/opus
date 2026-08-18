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

data class ComportementUiState(
    val comportements: List<Comportement> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Comportement? = null,
    val isDeleting: Boolean = false,
    val detailTarget: Comportement? = null,
    val userMessage: String? = null
) {
    val filtered: List<Comportement>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return comportements
            return comportements.filter { c ->
                c.im.lowercase().contains(query) ||
                    (c.nom?.lowercase()?.contains(query) == true) ||
                    (c.prenoms?.lowercase()?.contains(query) == true) ||
                    c.type.lowercase().contains(query) ||
                    c.motif.lowercase().contains(query)
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
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, "personnel", PermissionAction.CREATE),
                    canDelete = hasPermission(user, "personnel", PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = comportementRepository.getComportementList()) {
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

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
