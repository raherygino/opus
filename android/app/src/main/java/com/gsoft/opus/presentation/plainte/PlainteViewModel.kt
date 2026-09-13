package com.gsoft.opus.presentation.plainte

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.PlainteEntree
import com.gsoft.opus.domain.model.PlainteSortie
import com.gsoft.opus.domain.repository.PlainteRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which tab is active in the PLAINTE list. */
enum class PlainteTab { ENTREE, SORTIE }

data class PlainteUiState(
    val tab: PlainteTab = PlainteTab.ENTREE,
    val entrees: List<PlainteEntree> = emptyList(),
    val sorties: List<PlainteSortie> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val typeFilter: String = "",
    val natureFilter: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTargetEntree: PlainteEntree? = null,
    val deleteTargetSortie: PlainteSortie? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filteredEntrees: List<PlainteEntree>
        get() {
            val query = searchQuery.trim().lowercase()
            return entrees.filter { e ->
                val matchesType = typeFilter.isEmpty() || e.type == typeFilter
                val matchesQuery = query.isEmpty() ||
                    e.numeroDossier.lowercase().contains(query) ||
                    (e.partieCivile?.lowercase()?.contains(query) ?: false) ||
                    (e.miseEnCause?.lowercase()?.contains(query) ?: false) ||
                    (e.infraction?.lowercase()?.contains(query) ?: false)
                matchesType && matchesQuery
            }
        }

    val filteredSorties: List<PlainteSortie>
        get() {
            val query = searchQuery.trim().lowercase()
            return sorties.filter { s ->
                val matchesNature = natureFilter.isEmpty() || s.nature == natureFilter
                val matchesQuery = query.isEmpty() ||
                    s.numero.lowercase().contains(query) ||
                    (s.numeroTtr?.lowercase()?.contains(query) ?: false) ||
                    (s.nomSubstitut?.lowercase()?.contains(query) ?: false) ||
                    (s.entreeNumeroDossier?.lowercase()?.contains(query) ?: false)
                matchesNature && matchesQuery
            }
        }
}

@HiltViewModel
class PlainteViewModel @Inject constructor(
    private val plainteRepository: PlainteRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PlainteUiState())
    val state: StateFlow<PlainteUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, PJ_PLAINTE_MODULE, PermissionAction.CREATE),
                    canDelete = hasPermission(user, PJ_PLAINTE_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = plainteRepository.getPlainteEntreeList()) {
                is Resource.Success -> _state.update { it.copy(entrees = result.data) }
                is Resource.Error -> _state.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> {}
            }
            when (val result = plainteRepository.getPlainteSortieList()) {
                is Resource.Success -> _state.update {
                    it.copy(sorties = result.data, isLoading = false)
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun setTab(tab: PlainteTab) {
        _state.update { it.copy(tab = tab, searchQuery = "", typeFilter = "", natureFilter = "") }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setTypeFilter(type: String) {
        _state.update { it.copy(typeFilter = type) }
    }

    fun setNatureFilter(nature: String) {
        _state.update { it.copy(natureFilter = nature) }
    }

    fun requestDeleteEntree(entry: PlainteEntree) {
        _state.update { it.copy(deleteTargetEntree = entry) }
    }

    fun requestDeleteSortie(sortie: PlainteSortie) {
        _state.update { it.copy(deleteTargetSortie = sortie) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTargetEntree = null, deleteTargetSortie = null) }
    }

    fun confirmDelete() {
        val entree = _state.value.deleteTargetEntree
        val sortie = _state.value.deleteTargetSortie
        if (entree != null) {
            viewModelScope.launch {
                _state.update { it.copy(isDeleting = true) }
                when (plainteRepository.deletePlainteEntree(entree.id)) {
                    is Resource.Success -> _state.update {
                        it.copy(
                            isDeleting = false,
                            deleteTargetEntree = null,
                            entrees = it.entrees.filterNot { e -> e.id == entree.id },
                            userMessage = "Plainte supprimée avec succès"
                        )
                    }
                    is Resource.Error -> _state.update {
                        it.copy(
                            isDeleting = false,
                            deleteTargetEntree = null,
                            userMessage = "Impossible de supprimer cette plainte"
                        )
                    }
                    is Resource.Loading -> {}
                }
            }
        } else if (sortie != null) {
            viewModelScope.launch {
                _state.update { it.copy(isDeleting = true) }
                when (plainteRepository.deletePlainteSortie(sortie.id)) {
                    is Resource.Success -> _state.update {
                        it.copy(
                            isDeleting = false,
                            deleteTargetSortie = null,
                            sorties = it.sorties.filterNot { s -> s.id == sortie.id },
                            userMessage = "Sortie supprimée avec succès"
                        )
                    }
                    is Resource.Error -> _state.update {
                        it.copy(
                            isDeleting = false,
                            deleteTargetSortie = null,
                            userMessage = "Impossible de supprimer cette sortie"
                        )
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
