package com.gsoft.opus.presentation.registreenquete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.RegistreEnquete
import com.gsoft.opus.domain.repository.RegistreEnqueteRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegistreEnqueteListUiState(
    val items: List<RegistreEnquete> = emptyList(),
    val filtered: List<RegistreEnquete> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: RegistreEnquete? = null,
    val userMessage: String? = null
)

@HiltViewModel
class RegistreEnqueteViewModel @Inject constructor(
    private val repository: RegistreEnqueteRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegistreEnqueteListUiState())
    val state: StateFlow<RegistreEnqueteListUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, PJ_ENQUETE_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, PJ_ENQUETE_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, PJ_ENQUETE_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getEnqueteList()) {
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

    private fun applyFilters(items: List<RegistreEnquete>, query: String): List<RegistreEnquete> {
        if (query.isBlank()) return items
        return items.filter {
            it.numero.contains(query, ignoreCase = true) ||
            it.natureInfraction.contains(query, ignoreCase = true) ||
            (it.plaignant?.contains(query, ignoreCase = true) ?: false) ||
            (it.miseEnCause?.contains(query, ignoreCase = true) ?: false) ||
            (it.numeroDossier?.contains(query, ignoreCase = true) ?: false) ||
            (it.enqueteurNom?.contains(query, ignoreCase = true) ?: false) ||
            (it.enqueteurPrenoms?.contains(query, ignoreCase = true) ?: false)
        }
    }

    fun requestDelete(item: RegistreEnquete) {
        _state.update { it.copy(deleteTarget = item) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            when (val res = repository.deleteEnquete(target.id)) {
                is Resource.Success -> {
                    _state.update { it.copy(deleteTarget = null, userMessage = "Entrée supprimée") }
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
