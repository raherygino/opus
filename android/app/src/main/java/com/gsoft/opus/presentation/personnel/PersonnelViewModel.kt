package com.gsoft.opus.presentation.personnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonnelUiState(
    val personnel: List<Personnel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val serviceFilter: String = "",
    val canCreate: Boolean = false,
    val canDelete: Boolean = false,
    val deleteTarget: Personnel? = null,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
) {
    /** Distinct affectations present in the data, for the service tabs. */
    val services: List<String>
        get() = personnel.mapNotNull { it.affectation }
            .filter { it.isNotBlank() && !it.equals("Unité Spéciale", ignoreCase = true) }
            .distinct()
            .sorted()

    val filtered: List<Personnel>
        get() {
            val query = searchQuery.trim().lowercase()
            return personnel.filter { p ->
                val matchesService = serviceFilter.isEmpty() || p.affectation == serviceFilter
                val matchesQuery = query.isEmpty() ||
                    p.lastname.lowercase().contains(query) ||
                    p.firstname.lowercase().contains(query) ||
                    "${p.firstname} ${p.lastname}".lowercase().contains(query) ||
                    p.im.lowercase().contains(query) ||
                    p.grade.lowercase().contains(query) ||
                    (p.affectation?.lowercase()?.contains(query) == true)
                matchesService && matchesQuery
            }
        }
}

@HiltViewModel
class PersonnelViewModel @Inject constructor(
    private val personnelRepository: PersonnelRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PersonnelUiState())
    val state: StateFlow<PersonnelUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canCreate = hasPermission(user, "personnel", PermissionAction.CREATE),
                    canDelete = hasPermission(user, "personnel", PermissionAction.DELETE)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = personnelRepository.getPersonnelList()) {
                is Resource.Success -> _state.update {
                    it.copy(personnel = result.data, isLoading = false)
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

    fun setServiceFilter(service: String) {
        _state.update { it.copy(serviceFilter = service) }
    }

    fun requestDelete(personnel: Personnel) {
        _state.update { it.copy(deleteTarget = personnel) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (personnelRepository.deletePersonnel(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        personnel = it.personnel.filterNot { p -> p.id == target.id },
                        userMessage = "Personnel supprimé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer ce personnel"
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
