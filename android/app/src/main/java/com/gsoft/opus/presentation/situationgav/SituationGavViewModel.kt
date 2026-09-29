package com.gsoft.opus.presentation.situationgav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.SituationGav
import com.gsoft.opus.domain.repository.SituationGavRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SituationGavListUiState(
    val items: List<SituationGav> = emptyList(),
    val filtered: List<SituationGav> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: SituationGav? = null,
    val userMessage: String? = null
)

@HiltViewModel
class SituationGavViewModel @Inject constructor(
    private val situationGavRepository: SituationGavRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SituationGavListUiState())
    val state: StateFlow<SituationGavListUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, SED_SITUATION_GAV_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, SED_SITUATION_GAV_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, SED_SITUATION_GAV_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = situationGavRepository.getSituationGavList()) {
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

    private fun applyFilters(items: List<SituationGav>, query: String): List<SituationGav> {
        if (query.isBlank()) return items
        val q = query.lowercase()
        return items.filter {
            (it.personneNom?.contains(q, ignoreCase = true) ?: false) ||
            (it.personnePrenoms?.contains(q, ignoreCase = true) ?: false) ||
            (it.agentControleNom?.contains(q, ignoreCase = true) ?: false) ||
            (it.agentControleGrade?.contains(q, ignoreCase = true) ?: false) ||
            (it.etatGeneral?.contains(q, ignoreCase = true) ?: false) ||
            (it.observations?.contains(q, ignoreCase = true) ?: false)
        }
    }

    fun requestDelete(item: SituationGav) {
        _state.update { it.copy(deleteTarget = item) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            when (val res = situationGavRepository.deleteSituationGav(target.id)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            deleteTarget = null,
                            userMessage = "Situation GAV supprimée"
                        )
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
