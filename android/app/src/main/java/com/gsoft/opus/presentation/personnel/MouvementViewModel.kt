package com.gsoft.opus.presentation.personnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Mouvement
import com.gsoft.opus.domain.model.MouvementAttachment
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

/** A file selected for upload, held until the mouvement is created. */
data class PendingFile(
    val title: String,
    val fileName: String,
    val uploadFile: UploadFile
)

data class MouvementFormState(
    val personnelId: Int = 0,
    val im: String = "",
    val grade: String = "",
    val service: String = "",
    val nom: String = "",
    val prenoms: String = "",
    val typeMouvement: String = "",
    val dateDepart: String = "",
    val days: String = "",
    val dateRetour: String = ""
)

data class MouvementUiState(
    val mouvements: List<Mouvement> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",

    // New mouvement form dialog
    val formOpen: Boolean = false,
    val form: MouvementFormState = MouvementFormState(),
    val formSearchName: String = "",
    val formAutocomplete: List<Personnel> = emptyList(),
    val pendingFiles: List<PendingFile> = emptyList(),
    val isSaving: Boolean = false,

    // Retour dialog
    val retourTarget: Mouvement? = null,
    val retourDate: String = "",
    val isSavingRetour: Boolean = false,

    // Detail dialog
    val detailTarget: Mouvement? = null,
    val detailAttachments: List<MouvementAttachment> = emptyList(),
    val isLoadingAttachments: Boolean = false,

    // Delete
    val deleteTarget: Mouvement? = null,
    val isDeleting: Boolean = false,

    val userMessage: String? = null
) {
    val filtered: List<Mouvement>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return mouvements
            return mouvements.filter { m ->
                m.im.lowercase().contains(query) ||
                    (m.nom?.lowercase()?.contains(query) == true) ||
                    (m.prenoms?.lowercase()?.contains(query) == true) ||
                    m.typeMouvement.lowercase().contains(query)
            }
        }
}

@HiltViewModel
class MouvementViewModel @Inject constructor(
    private val mouvementRepository: MouvementRepository,
    private val personnelRepository: PersonnelRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MouvementUiState())
    val state: StateFlow<MouvementUiState> = _state.asStateFlow()

    // Cached personnel list used by the autocomplete in the mouvement form.
    private var personnelCache: List<Personnel> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = mouvementRepository.getMouvementList()) {
                is Resource.Success -> _state.update {
                    it.copy(mouvements = result.data, isLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
            // Refresh the personnel cache too, so statuses stay in sync.
            when (val p = personnelRepository.getPersonnelList()) {
                is Resource.Success -> personnelCache = p.data
                else -> {}
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    // ─── New mouvement form ─────────────────────────────────────────

    fun openForm() {
        _state.update {
            it.copy(
                formOpen = true,
                form = MouvementFormState(),
                formSearchName = "",
                formAutocomplete = emptyList(),
                pendingFiles = emptyList()
            )
        }
    }

    fun closeForm() {
        _state.update { it.copy(formOpen = false) }
    }

    fun onFormSearchNameChange(value: String) {
        val query = value.trim().lowercase()
        val results = if (query.isEmpty()) emptyList() else personnelCache.filter { p ->
            p.lastname.lowercase().contains(query) ||
                p.firstname.lowercase().contains(query) ||
                "${p.firstname} ${p.lastname}".lowercase().contains(query) ||
                p.im.lowercase().contains(query)
        }
        _state.update { it.copy(formSearchName = value, formAutocomplete = results) }
    }

    fun selectPersonnel(person: Personnel) {
        _state.update { s ->
            s.copy(
                form = s.form.copy(
                    personnelId = person.id,
                    im = person.im,
                    grade = person.grade,
                    service = person.affectation ?: "",
                    nom = person.lastname,
                    prenoms = person.firstname,
                    dateDepart = "",
                    days = "",
                    dateRetour = ""
                ),
                formSearchName = "${person.firstname} ${person.lastname}",
                formAutocomplete = emptyList()
            )
        }
    }

    fun onTypeChange(type: String) {
        _state.update { s -> s.copy(form = s.form.copy(typeMouvement = type)) }
    }

    fun onDateDepartChange(value: String) {
        _state.update { s ->
            val updated = s.form.copy(dateDepart = value)
            s.copy(form = updated.copy(dateRetour = computeDateRetour(value, updated.days)))
        }
    }

    fun onDaysChange(value: String) {
        _state.update { s ->
            val updated = s.form.copy(days = value.filter { c -> c.isDigit() })
            s.copy(form = updated.copy(dateRetour = computeDateRetour(updated.dateDepart, updated.days)))
        }
    }

    fun onDateRetourChange(value: String) {
        _state.update { s -> s.copy(form = s.form.copy(dateRetour = value)) }
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

    fun saveMouvement() {
        val form = _state.value.form
        if (form.personnelId == 0) {
            _state.update { it.copy(userMessage = "Recherchez d'abord un personnel") }
            return
        }
        if (form.typeMouvement.isBlank()) {
            _state.update { it.copy(userMessage = "Sélectionnez un type de mouvement") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = mouvementRepository.createMouvement(
                MouvementFormData(
                    personnelId = form.personnelId,
                    im = form.im,
                    grade = form.grade.ifBlank { null },
                    service = form.service.ifBlank { null },
                    nom = form.nom.ifBlank { null },
                    prenoms = form.prenoms.ifBlank { null },
                    typeMouvement = form.typeMouvement,
                    dateDepart = form.dateDepart.ifBlank { null },
                    days = form.days.toIntOrNull(),
                    dateRetour = form.dateRetour.ifBlank { null }
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
                            formOpen = false,
                            userMessage = attachmentFailure ?: "Mouvement ajouté avec succès"
                        )
                    }
                    refresh()
                }
                is Resource.Error -> _state.update {
                    it.copy(isSaving = false, userMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Retour ─────────────────────────────────────────────────────

    fun openRetourDialog(mouvement: Mouvement) {
        _state.update {
            it.copy(retourTarget = mouvement, retourDate = mouvement.dateRetour ?: "")
        }
    }

    fun onRetourDateChange(value: String) {
        _state.update { it.copy(retourDate = value) }
    }

    fun closeRetourDialog() {
        if (_state.value.isSavingRetour) return
        _state.update { it.copy(retourTarget = null) }
    }

    fun confirmRetour() {
        val target = _state.value.retourTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSavingRetour = true) }
            when (mouvementRepository.confirmRetour(target.id, _state.value.retourDate.ifBlank { null })) {
                is Resource.Success -> _state.update {
                    it.copy(isSavingRetour = false, retourTarget = null, userMessage = "Retour enregistré avec succès")
                }
                is Resource.Error -> _state.update {
                    it.copy(isSavingRetour = false, userMessage = "Impossible d'enregistrer le retour")
                }
                is Resource.Loading -> {}
            }
            refresh()
        }
    }

    // ─── Detail dialog ──────────────────────────────────────────────

    fun openDetail(mouvement: Mouvement) {
        _state.update { it.copy(detailTarget = mouvement, detailAttachments = emptyList()) }
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAttachments = true) }
            when (val result = mouvementRepository.getAttachments(mouvement.id)) {
                is Resource.Success -> _state.update {
                    it.copy(detailAttachments = result.data, isLoadingAttachments = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoadingAttachments = false, userMessage = "Impossible de charger les pièces jointes")
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun closeDetail() {
        _state.update { it.copy(detailTarget = null, detailAttachments = emptyList()) }
    }

    fun deleteDetailAttachment(attachId: Int) {
        val target = _state.value.detailTarget ?: return
        viewModelScope.launch {
            when (mouvementRepository.deleteAttachment(target.id, attachId)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        detailAttachments = it.detailAttachments.filterNot { a -> a.id == attachId },
                        userMessage = "Pièce jointe supprimée"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(userMessage = "Impossible de supprimer la pièce jointe")
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Delete mouvement ───────────────────────────────────────────

    fun requestDelete(mouvement: Mouvement) {
        _state.update { it.copy(deleteTarget = mouvement) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (mouvementRepository.deleteMouvement(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        detailTarget = null,
                        mouvements = it.mouvements.filterNot { m -> m.id == target.id },
                        userMessage = "Mouvement supprimé avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        userMessage = "Impossible de supprimer ce mouvement"
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
