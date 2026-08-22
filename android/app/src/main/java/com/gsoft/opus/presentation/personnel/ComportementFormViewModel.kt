package com.gsoft.opus.presentation.personnel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.repository.ComportementFormData
import com.gsoft.opus.domain.repository.ComportementRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComportementFormUiState(
    val personnelId: Int = 0,
    val im: String = "",
    val grade: String = "",
    val service: String = "",
    val nom: String = "",
    val prenoms: String = "",
    val type: String = "",
    val dateComportement: String = "",
    val motif: String = "",
    val decision: String = "",
    val searchName: String = "",
    val autocomplete: List<Personnel> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class ComportementFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val comportementRepository: ComportementRepository,
    private val personnelRepository: PersonnelRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val prefillPersonnelId: Int = savedStateHandle.get<Int>("personnelId") ?: 0

    private val _state = MutableStateFlow(ComportementFormUiState())
    val state: StateFlow<ComportementFormUiState> = _state.asStateFlow()

    private var personnelCache: List<Personnel> = emptyList()
    private var isAdmin: Boolean = false

    init {
        loadCurrentUser()
        loadPersonnelCache()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            isAdmin = user?.roleCode == "SUPER_ADMIN" || user?.roleCode == "STATION_ADMIN"
        }
    }

    private fun loadPersonnelCache() {
        viewModelScope.launch {
            when (val result = personnelRepository.getPersonnelList()) {
                is Resource.Success -> {
                    personnelCache = result.data
                    if (prefillPersonnelId > 0) {
                        personnelCache.find { it.id == prefillPersonnelId }?.let { selectPersonnel(it) }
                    }
                }
                else -> {}
            }
        }
    }

    fun onSearchNameChange(value: String) {
        val query = value.trim().lowercase()
        val results = if (query.isEmpty()) emptyList() else personnelCache.filter { p ->
            p.lastname.lowercase().contains(query) ||
                p.firstname.lowercase().contains(query) ||
                "${p.firstname} ${p.lastname}".lowercase().contains(query) ||
                p.im.lowercase().contains(query)
        }
        _state.update { it.copy(searchName = value, autocomplete = results) }
    }

    fun selectPersonnel(person: Personnel) {
        _state.update {
            it.copy(
                personnelId = person.id,
                im = person.im,
                grade = person.grade,
                service = person.affectation ?: "",
                nom = person.lastname,
                prenoms = person.firstname,
                searchName = "${person.firstname} ${person.lastname}",
                autocomplete = emptyList()
            )
        }
    }

    fun onTypeChange(type: String) {
        _state.update { it.copy(type = type) }
    }

    fun onDateChange(value: String) {
        _state.update { it.copy(dateComportement = value) }
    }

    fun onMotifChange(value: String) {
        _state.update { it.copy(motif = value) }
    }

    fun onDecisionChange(value: String) {
        _state.update { it.copy(decision = value) }
    }

    fun save() {
        val s = _state.value
        if (s.personnelId == 0) {
            _state.update { it.copy(errorMessage = "Recherchez d'abord un personnel") }
            return
        }
        if (s.type.isBlank()) {
            _state.update { it.copy(errorMessage = "Sélectionnez un type") }
            return
        }
        if (s.dateComportement.isBlank()) {
            _state.update { it.copy(errorMessage = "La date est requise") }
            return
        }
        if (s.motif.isBlank()) {
            _state.update { it.copy(errorMessage = "Le motif est requis") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val result = comportementRepository.createComportement(
                ComportementFormData(
                    personnelId = s.personnelId,
                    im = s.im,
                    grade = s.grade.ifBlank { null },
                    service = s.service.ifBlank { null },
                    nom = s.nom.ifBlank { null },
                    prenoms = s.prenoms.ifBlank { null },
                    type = s.type,
                    dateComportement = s.dateComportement,
                    motif = s.motif,
                    decision = s.decision.ifBlank { null }
                )
            )
            when (result) {
                is Resource.Success -> {
                    val message = if (isAdmin) {
                        "Comportement enregistré et confirmé avec succès"
                    } else {
                        "Comportement enregistré. En attente de confirmation par un administrateur."
                    }
                    _state.update {
                        it.copy(isSaving = false, saved = true, successMessage = message)
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
