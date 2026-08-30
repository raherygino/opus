package com.gsoft.opus.presentation.armement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Armement
import com.gsoft.opus.domain.repository.ArmementRepository
import com.gsoft.opus.domain.repository.ReintegrationData
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArmementUiState(
    val armements: List<Armement> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Armement? = null,
    val isDeleting: Boolean = false,
    val reintegrationTarget: Armement? = null,
    val isReintegrating: Boolean = false,
    val reintegrationError: String? = null,
    val userMessage: String? = null
) {
    val filtered: List<Armement>
        get() {
            val query = searchQuery.trim().lowercase()
            return armements.filter { a ->
                query.isEmpty() ||
                    a.agentPreneurNom?.lowercase()?.contains(query) == true ||
                    a.agentPreneurIm?.lowercase()?.contains(query) == true ||
                    a.typeArme.lowercase().contains(query) ||
                    a.matriculeArme.lowercase().contains(query) ||
                    a.secteurMission?.lowercase()?.contains(query) == true
            }
        }
}

@HiltViewModel
class ArmementViewModel @Inject constructor(
    private val armementRepository: ArmementRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ArmementUiState())
    val state: StateFlow<ArmementUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, ARMEMENT_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, ARMEMENT_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, ARMEMENT_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = armementRepository.getArmementList()) {
                is Resource.Success -> _state.update {
                    it.copy(armements = result.data, isLoading = false)
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

    fun requestDelete(armement: Armement) {
        _state.update { it.copy(deleteTarget = armement) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (armementRepository.deleteArmement(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        armements = it.armements.filterNot { a -> a.id == target.id },
                        userMessage = "Armement supprimé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer cet armement"
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Réintégration ──────────────────────────────────────────────

    /** Opens the reintegration dialog — only while the weapon is en cours. */
    fun requestReintegration(armement: Armement) {
        if (armement.isReintegree) return
        _state.update { it.copy(reintegrationTarget = armement, reintegrationError = null) }
    }

    fun cancelReintegration() {
        _state.update { it.copy(reintegrationTarget = null, reintegrationError = null) }
    }

    fun confirmReintegration(data: ReintegrationData) {
        val target = _state.value.reintegrationTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isReintegrating = true, reintegrationError = null) }
            when (val result = armementRepository.reintegrateArmement(target.id, data)) {
                is Resource.Success -> _state.update { s ->
                    s.copy(
                        isReintegrating = false,
                        reintegrationTarget = null,
                        armements = s.armements.map { a -> if (a.id == target.id) result.data else a },
                        userMessage = "Arme réintégrée avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isReintegrating = false, reintegrationError = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
