package com.gsoft.opus.presentation.materiel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.AffectationMateriel
import com.gsoft.opus.domain.model.TypeMateriel
import com.gsoft.opus.domain.repository.AffectationMaterielFormData
import com.gsoft.opus.domain.repository.MaterielRepository
import com.gsoft.opus.domain.repository.TypeMaterielFormData
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaterielUiState(
    val affectations: List<AffectationMateriel> = emptyList(),
    val types: List<TypeMateriel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: AffectationMateriel? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filteredAffectations: List<AffectationMateriel>
        get() {
            val query = searchQuery.trim().lowercase()
            return affectations.filter { a ->
                query.isEmpty() ||
                    a.agentNom?.lowercase()?.contains(query) == true ||
                    a.agentIm?.lowercase()?.contains(query) == true ||
                    a.materielsSummary.lowercase().contains(query) ||
                    a.observations?.lowercase()?.contains(query) == true
            }
        }

    val filteredTypes: List<TypeMateriel>
        get() {
            val query = searchQuery.trim().lowercase()
            return types.filter { t ->
                query.isEmpty() ||
                    t.nom.lowercase().contains(query) ||
                    t.description?.lowercase()?.contains(query) == true
            }
        }
}

@HiltViewModel
class MaterielViewModel @Inject constructor(
    private val materielRepository: MaterielRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MaterielUiState())
    val state: StateFlow<MaterielUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, MATERIEL_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, MATERIEL_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, MATERIEL_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val affResult = materielRepository.getAffectationMaterielList()
            val typeResult = materielRepository.getTypeMaterielList()
            val aff = (affResult as? Resource.Success)?.data ?: emptyList()
            val types = (typeResult as? Resource.Success)?.data ?: emptyList()
            val errors = mutableListOf<String>()
            if (affResult is Resource.Error) errors.add("Affectations indisponibles")
            if (typeResult is Resource.Error) errors.add("Types de matériel indisponibles")
            _state.update {
                it.copy(
                    affectations = aff,
                    types = types,
                    isLoading = false,
                    errorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun requestDelete(affectation: AffectationMateriel) {
        _state.update { it.copy(deleteTarget = affectation) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (materielRepository.deleteAffectationMateriel(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        affectations = it.affectations.filterNot { a -> a.id == target.id },
                        userMessage = "Affectation supprimée avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer cette affectation"
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
