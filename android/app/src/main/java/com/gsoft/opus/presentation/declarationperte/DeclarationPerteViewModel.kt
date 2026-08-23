package com.gsoft.opus.presentation.declarationperte

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.DeclarationPerte
import com.gsoft.opus.domain.repository.DeclarationPerteRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeclarationPerteUiState(
    val declarations: List<DeclarationPerte> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: DeclarationPerte? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    val filtered: List<DeclarationPerte>
        get() {
            val query = searchQuery.trim().lowercase()
            return declarations.filter { d ->
                query.isEmpty() ||
                    d.numeroAttestation.lowercase().contains(query) ||
                    d.identiteDeclarant.lowercase().contains(query) ||
                    d.natureObjet.lowercase().contains(query) ||
                    d.lieuPerte.lowercase().contains(query)
            }
        }
}

@HiltViewModel
class DeclarationPerteViewModel @Inject constructor(
    private val declarationPerteRepository: DeclarationPerteRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DeclarationPerteUiState())
    val state: StateFlow<DeclarationPerteUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, DECLARATION_PERTE_MODULE, PermissionAction.CREATE),
                    canDelete = hasPermission(user, DECLARATION_PERTE_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = declarationPerteRepository.getDeclarationPerteList()) {
                is Resource.Success -> _state.update {
                    it.copy(declarations = result.data, isLoading = false)
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

    fun requestDelete(declaration: DeclarationPerte) {
        _state.update { it.copy(deleteTarget = declaration) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (declarationPerteRepository.deleteDeclarationPerte(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        declarations = it.declarations.filterNot { d -> d.id == target.id },
                        userMessage = "Déclaration de perte supprimée avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer cette déclaration de perte"
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
