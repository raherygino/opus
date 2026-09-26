package com.gsoft.opus.presentation.registreenquete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.repository.RegistreEnqueteFormData
import com.gsoft.opus.domain.repository.RegistreEnqueteRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegistreEnqueteFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val numero: String = "",
    val dateOuverture: String = "",
    val numeroDossier: String = "",
    val natureInfraction: String = "",
    val dateLieuFaits: String = "",
    val plaignant: String = "",
    val miseEnCause: String = "",
    val enqueteurPersonnelId: Int = 0,
    val opjPersonnelId: Int = 0,
    val statut: String = "EN_COURS",
    val observations: String = "",
    val personnelOptions: List<Personnel> = emptyList(),
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class RegistreEnqueteFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RegistreEnqueteRepository,
    private val personnelRepository: PersonnelRepository
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("registreEnqueteId") ?: 0
    val editId: Int get() = entryId

    private val _state = MutableStateFlow(RegistreEnqueteFormUiState(isEdit = entryId > 0))
    val state: StateFlow<RegistreEnqueteFormUiState> = _state.asStateFlow()

    init {
        loadPersonnel()
        if (entryId > 0) loadEntry() else peekNumero()
    }

    private fun loadPersonnel() {
        viewModelScope.launch {
            when (val result = personnelRepository.getPersonnelList()) {
                is Resource.Success -> _state.update { it.copy(personnelOptions = result.data) }
                else -> {}
            }
        }
    }

    private fun peekNumero() {
        viewModelScope.launch {
            when (val res = repository.peekNextNumber()) {
                is Resource.Success -> _state.update { it.copy(numero = res.data) }
                else -> {}
            }
        }
    }

    fun refreshSuggestedNumber() = peekNumero()

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getEnquete(entryId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            numero = e.numero,
                            dateOuverture = e.dateOuverture,
                            numeroDossier = e.numeroDossier ?: "",
                            natureInfraction = e.natureInfraction,
                            dateLieuFaits = e.dateLieuFaits ?: "",
                            plaignant = e.plaignant ?: "",
                            miseEnCause = e.miseEnCause ?: "",
                            enqueteurPersonnelId = e.enqueteurPersonnelId ?: 0,
                            opjPersonnelId = e.opjPersonnelId ?: 0,
                            statut = e.statut,
                            observations = e.observations ?: ""
                        )
                    }
                    when (val atts = repository.getEnqueteAttachments(entryId)) {
                        is Resource.Success -> _state.update {
                            it.copy(attachments = atts.data.map { a ->
                                AttachmentItem(id = a.id, title = a.title, existingFilename = a.originalFilename)
                            })
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateNumero(value: String) { _state.update { it.copy(numero = value) } }
    fun updateDateOuverture(value: String) { _state.update { it.copy(dateOuverture = value) } }
    fun updateNumeroDossier(value: String) { _state.update { it.copy(numeroDossier = value) } }
    fun updateNatureInfraction(value: String) { _state.update { it.copy(natureInfraction = value) } }
    fun updateDateLieuFaits(value: String) { _state.update { it.copy(dateLieuFaits = value) } }
    fun updatePlaignant(value: String) { _state.update { it.copy(plaignant = value) } }
    fun updateMiseEnCause(value: String) { _state.update { it.copy(miseEnCause = value) } }
    fun updateEnqueteur(personnelId: Int) { _state.update { it.copy(enqueteurPersonnelId = personnelId) } }
    fun updateOpj(personnelId: Int) { _state.update { it.copy(opjPersonnelId = personnelId) } }
    fun updateStatut(value: String) { _state.update { it.copy(statut = value) } }
    fun updateObservations(value: String) { _state.update { it.copy(observations = value) } }

    fun addAttachment() {
        _state.update { it.copy(attachments = it.attachments + AttachmentItem()) }
    }

    fun updateAttachmentTitle(index: Int, title: String) {
        _state.update { s ->
            s.copy(attachments = s.attachments.mapIndexed { i, a -> if (i == index) a.copy(title = title) else a })
        }
    }

    fun setAttachmentFile(index: Int, file: UploadFile) {
        _state.update { s ->
            s.copy(attachments = s.attachments.mapIndexed { i, a -> if (i == index) a.copy(uploadFile = file) else a })
        }
    }

    fun removeAttachment(index: Int) {
        _state.update { s ->
            val list = s.attachments.toMutableList()
            if (list[index].id != null) {
                list[index] = list[index].copy(isDeleted = true)
            } else {
                list.removeAt(index)
            }
            s.copy(attachments = list)
        }
    }

    fun save() {
        val s = _state.value
        val errors = validateEnqueteForm(s.dateOuverture, s.natureInfraction, s.enqueteurPersonnelId)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = RegistreEnqueteFormData(
                numero = s.numero.trim().ifBlank { null },
                dateOuverture = s.dateOuverture.trim(),
                numeroDossier = s.numeroDossier.trim().ifBlank { null },
                natureInfraction = s.natureInfraction.trim(),
                dateLieuFaits = s.dateLieuFaits.trim().ifBlank { null },
                plaignant = s.plaignant.trim().ifBlank { null },
                miseEnCause = s.miseEnCause.trim().ifBlank { null },
                enqueteurPersonnelId = s.enqueteurPersonnelId.takeIf { it > 0 },
                opjPersonnelId = s.opjPersonnelId.takeIf { it > 0 },
                statut = s.statut.ifBlank { "EN_COURS" },
                observations = s.observations.trim().ifBlank { null }
            )
            val result = if (s.isEdit) repository.updateEnquete(entryId, data)
            else repository.createEnquete(data)
            when (result) {
                is Resource.Success -> handleAttachments(result.data.id)
                is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    private suspend fun handleAttachments(savedId: Int) {
        val s = _state.value
        for (a in s.attachments.filter { it.isDeleted && it.id != null }) {
            repository.deleteEnqueteAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                repository.deleteEnqueteAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    repository.addEnqueteAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                repository.updateEnqueteAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                repository.addEnqueteAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
