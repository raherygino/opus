package com.gsoft.opus.presentation.materielroulant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.MaterielRoulant
import com.gsoft.opus.domain.repository.MaterielRoulantRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaterielRoulantUiState(
    val items: List<MaterielRoulant> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: MaterielRoulant? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filteredItems: List<MaterielRoulant>
        get() {
            val query = searchQuery.trim().lowercase()
            return items.filter { m ->
                query.isEmpty() ||
                    m.agentConducteurNom?.lowercase()?.contains(query) == true ||
                    m.agentConducteurIm?.lowercase()?.contains(query) == true ||
                    m.chefDeBordNom?.lowercase()?.contains(query) == true ||
                    m.chefDeBordIm?.lowercase()?.contains(query) == true ||
                    m.numeroImmatriculation?.lowercase()?.contains(query) == true ||
                    m.descriptionVehicule?.lowercase()?.contains(query) == true ||
                    m.observationsTechniques?.lowercase()?.contains(query) == true ||
                    m.defaillances?.lowercase()?.contains(query) == true ||
                    m.typeMateriel.lowercase().contains(query)
            }
        }
}

@HiltViewModel
class MaterielRoulantViewModel @Inject constructor(
    private val materielRoulantRepository: MaterielRoulantRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MaterielRoulantUiState())
    val state: StateFlow<MaterielRoulantUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, MATERIEL_ROULANT_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, MATERIEL_ROULANT_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, MATERIEL_ROULANT_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = materielRoulantRepository.getMaterielRoulantList()) {
                is Resource.Success -> _state.update {
                    it.copy(items = result.data, isLoading = false)
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

    fun requestDelete(item: MaterielRoulant) {
        _state.update { it.copy(deleteTarget = item) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (materielRoulantRepository.deleteMaterielRoulant(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        items = it.items.filterNot { m -> m.id == target.id },
                        userMessage = "Matériel roulant supprimé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer ce matériel roulant"
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
