package com.gsoft.opus.presentation.convocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Convocation
import com.gsoft.opus.domain.repository.ConvocationRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConvocationListUiState(
    val items: List<Convocation> = emptyList(),
    val filtered: List<Convocation> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val typeFilter: String? = null,
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Convocation? = null,
    val userMessage: String? = null
)

@HiltViewModel
class ConvocationViewModel @Inject constructor(
    private val convocationRepository: ConvocationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ConvocationListUiState())
    val state: StateFlow<ConvocationListUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, PJ_CONVOCATION_MODULE, PermissionAction.CREATE),
                    canEdit = hasPermission(user, PJ_CONVOCATION_MODULE, PermissionAction.EDIT),
                    canDelete = hasPermission(user, PJ_CONVOCATION_MODULE, PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = convocationRepository.getConvocationList()) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = result.data,
                            filtered = applyFilters(result.data, it.searchQuery, it.typeFilter)
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
                filtered = applyFilters(it.items, query, it.typeFilter)
            )
        }
    }

    fun setTypeFilter(type: String?) {
        _state.update {
            it.copy(
                typeFilter = type,
                filtered = applyFilters(it.items, it.searchQuery, type)
            )
        }
    }

    private fun applyFilters(items: List<Convocation>, query: String, type: String?): List<Convocation> {
        var result = items
        if (!type.isNullOrBlank()) {
            result = result.filter { it.type == type }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter {
                it.numero.contains(q, ignoreCase = true) ||
                it.nom.contains(q, ignoreCase = true) ||
                (it.infraction?.contains(q, ignoreCase = true) ?: false) ||
                (it.numeroDossier?.contains(q, ignoreCase = true) ?: false)
            }
        }
        return result
    }

    fun requestDelete(item: Convocation) {
        _state.update { it.copy(deleteTarget = item) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            when (val res = convocationRepository.deleteConvocation(target.id)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            deleteTarget = null,
                            userMessage = "Convocation supprimée"
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
