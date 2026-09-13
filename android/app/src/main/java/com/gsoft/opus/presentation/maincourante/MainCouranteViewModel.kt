package com.gsoft.opus.presentation.maincourante

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.MainCourante
import com.gsoft.opus.domain.repository.MainCouranteCategorieRepository
import com.gsoft.opus.domain.repository.MainCouranteRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainCouranteUiState(
    val entries: List<MainCourante> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val categorieFilter: String = "",
    val categories: List<String> = emptyList(),
    val origine: String = "Secretariat",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: MainCourante? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<MainCourante>
        get() {
            val query = searchQuery.trim().lowercase()
            return entries.filter { e ->
                val matchesCat = categorieFilter.isEmpty() || e.categorie == categorieFilter
                val matchesQuery = query.isEmpty() ||
                    e.description.lowercase().contains(query) ||
                    e.categorie.lowercase().contains(query)
                matchesCat && matchesQuery
            }
        }
}

@HiltViewModel
class MainCouranteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mainCouranteRepository: MainCouranteRepository,
    private val categorieRepository: MainCouranteCategorieRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    val origine: String = savedStateHandle.get<String>("origine") ?: "Secretariat"
    private val module = moduleForOrigine(origine)

    private val _state = MutableStateFlow(MainCouranteUiState(origine = origine))
    val state: StateFlow<MainCouranteUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        loadCategories()
        refresh()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = categorieRepository.getCategories()) {
                is Resource.Success -> _state.update {
                    it.copy(categories = result.data.map { c -> c.label })
                }
                is Resource.Error -> {}
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, module, PermissionAction.CREATE),
                    canDelete = hasPermission(user, module, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = mainCouranteRepository.getMainCouranteList(origine = origine)) {
                is Resource.Success -> _state.update {
                    it.copy(entries = result.data, isLoading = false)
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

    fun setCategorieFilter(categorie: String) {
        _state.update { it.copy(categorieFilter = categorie) }
    }

    fun requestDelete(entry: MainCourante) {
        _state.update { it.copy(deleteTarget = entry) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (mainCouranteRepository.deleteMainCourante(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        entries = it.entries.filterNot { e -> e.id == target.id },
                        userMessage = "Main courante supprimée avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer cette main courante"
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
