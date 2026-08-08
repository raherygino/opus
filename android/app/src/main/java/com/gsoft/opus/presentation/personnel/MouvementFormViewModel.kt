package com.gsoft.opus.presentation.personnel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.repository.MouvementFormData
import com.gsoft.opus.domain.repository.MouvementRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.repository.UploadFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MouvementFormUiState(
    val personnelId: Int = 0,
    val im: String = "",
    val grade: String = "",
    val service: String = "",
    val nom: String = "",
    val prenoms: String = "",
    val typeMouvement: String = "",
    val dateDepart: String = "",
    val days: String = "",
    val dateRetour: String = "",
    val searchName: String = "",
    val autocomplete: List<Personnel> = emptyList(),
    val pendingFiles: List<PendingFile> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class MouvementFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mouvementRepository: MouvementRepository,
    private val personnelRepository: PersonnelRepository
) : ViewModel() {

    private val prefillPersonnelId: Int = savedStateHandle.get<Int>("personnelId") ?: 0

    private val _state = MutableStateFlow(MouvementFormUiState())
    val state: StateFlow<MouvementFormUiState> = _state.asStateFlow()

    private var personnelCache: List<Personnel> = emptyList()

    init {
        loadPersonnelCache()
        if (prefillPersonnelId > 0) prefillPersonnel()
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

    private fun prefillPersonnel() {
        // If cache is already loaded, select immediately
        personnelCache.find { it.id == prefillPersonnelId }?.let { selectPersonnel(it) }
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
        _state.update { it.copy(typeMouvement = type) }
    }

    fun onDateDepartChange(value: String) {
        _state.update {
            it.copy(dateDepart = value, dateRetour = computeDateRetour(value, it.days))
        }
    }

    fun onDaysChange(value: String) {
        val filtered = value.filter { c -> c.isDigit() }
        _state.update {
            it.copy(days = filtered, dateRetour = computeDateRetour(it.dateDepart, filtered))
        }
    }

    fun onDateRetourChange(value: String) {
        _state.update { it.copy(dateRetour = value) }
    }

    fun addPendingFile(title: String, uploadFile: UploadFile) {
        _state.update {
            it.copy(pendingFiles = it.pendingFiles + PendingFile(title, uploadFile.fileName, uploadFile))
        }
    }

    fun removePendingFile(index: Int) {
        _state.update { s ->
            s.copy(pendingFiles = s.pendingFiles.filterIndexed { i, _ -> i != index })
        }
    }

    fun save() {
        val s = _state.value
        if (s.personnelId == 0) {
            _state.update { it.copy(errorMessage = "Recherchez d'abord un personnel") }
            return
        }
        if (s.typeMouvement.isBlank()) {
            _state.update { it.copy(errorMessage = "Sélectionnez un type de mouvement") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val result = mouvementRepository.createMouvement(
                MouvementFormData(
                    personnelId = s.personnelId,
                    im = s.im,
                    grade = s.grade.ifBlank { null },
                    service = s.service.ifBlank { null },
                    nom = s.nom.ifBlank { null },
                    prenoms = s.prenoms.ifBlank { null },
                    typeMouvement = s.typeMouvement,
                    dateDepart = s.dateDepart.ifBlank { null },
                    days = s.days.toIntOrNull(),
                    dateRetour = s.dateRetour.ifBlank { null }
                )
            )
            when (result) {
                is Resource.Success -> {
                    var attachmentFailure: String? = null
                    for (pf in _state.value.pendingFiles) {
                        val upload = mouvementRepository.addAttachment(result.data.id, pf.title, pf.uploadFile)
                        if (upload is Resource.Error) {
                            attachmentFailure = "Impossible d'ajouter la pièce jointe \"${pf.title}\""
                        }
                    }
                    _state.update {
                        it.copy(
                            isSaving = false,
                            saved = true,
                            errorMessage = attachmentFailure
                        )
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
