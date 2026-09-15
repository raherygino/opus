package com.gsoft.opus.presentation.objet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.ObjetSaisi
import com.gsoft.opus.domain.model.ObjetTrouve
import com.gsoft.opus.domain.repository.ObjetSaisiRepository
import com.gsoft.opus.domain.repository.ObjetTrouveRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObjetListUiState(
    val selectedTab: Int = 0,
    val saisiItems: List<ObjetSaisi> = emptyList(),
    val trouveItems: List<ObjetTrouve> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteSaisiTarget: ObjetSaisi? = null,
    val deleteTrouveTarget: ObjetTrouve? = null,
    val userMessage: String? = null
)

@HiltViewModel
class ObjetViewModel @Inject constructor(
    private val saisiRepository: ObjetSaisiRepository,
    private val trouveRepository: ObjetTrouveRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ObjetListUiState())
    val state: StateFlow<ObjetListUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, PJ_OBJETS_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, PJ_OBJETS_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, PJ_OBJETS_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun setTab(index: Int) {
        _state.update { it.copy(selectedTab = index) }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val saisiDeferred = async { saisiRepository.getObjetSaisiList() }
            val trouveDeferred = async { trouveRepository.getObjetTrouveList() }
            val saisi = saisiDeferred.await()
            val trouve = trouveDeferred.await()

            _state.update {
                it.copy(
                    isLoading = false,
                    saisiItems = saisi.getOrNull() ?: emptyList(),
                    trouveItems = trouve.getOrNull() ?: emptyList(),
                    errorMessage = when {
                        saisi is Resource.Error && trouve is Resource.Error ->
                            saisi.message ?: trouve.message
                        else -> null
                    }
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun filteredSaisi(): List<ObjetSaisi> {
        val s = _state.value
        if (s.searchQuery.isBlank()) return s.saisiItems
        val q = s.searchQuery.lowercase()
        return s.saisiItems.filter {
            (it.numeroDossier?.contains(q, true) ?: false) ||
            it.motif.contains(q, true) ||
            (it.proprietaire?.contains(q, true) ?: false)
        }
    }

    fun filteredTrouve(): List<ObjetTrouve> {
        val s = _state.value
        if (s.searchQuery.isBlank()) return s.trouveItems
        val q = s.searchQuery.lowercase()
        return s.trouveItems.filter {
            it.affaire.contains(q, true)
        }
    }

    fun requestDeleteSaisi(item: ObjetSaisi) {
        _state.update { it.copy(deleteSaisiTarget = item) }
    }

    fun requestDeleteTrouve(item: ObjetTrouve) {
        _state.update { it.copy(deleteTrouveTarget = item) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteSaisiTarget = null, deleteTrouveTarget = null) }
    }

    fun confirmDelete() {
        val saisi = _state.value.deleteSaisiTarget
        val trouve = _state.value.deleteTrouveTarget
        viewModelScope.launch {
            when {
                saisi != null -> {
                    when (val res = saisiRepository.deleteObjetSaisi(saisi.id)) {
                        is Resource.Success -> {
                            _state.update { it.copy(deleteSaisiTarget = null, userMessage = "Objet saisi supprimé") }
                            refresh()
                        }
                        is Resource.Error -> _state.update {
                            it.copy(deleteSaisiTarget = null, userMessage = res.message)
                        }
                        is Resource.Loading -> {}
                    }
                }
                trouve != null -> {
                    when (val res = trouveRepository.deleteObjetTrouve(trouve.id)) {
                        is Resource.Success -> {
                            _state.update { it.copy(deleteTrouveTarget = null, userMessage = "Objet trouvé supprimé") }
                            refresh()
                        }
                        is Resource.Error -> _state.update {
                            it.copy(deleteTrouveTarget = null, userMessage = res.message)
                        }
                        is Resource.Loading -> {}
                    }
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
