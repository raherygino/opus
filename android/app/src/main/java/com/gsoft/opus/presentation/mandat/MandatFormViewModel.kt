package com.gsoft.opus.presentation.mandat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.MandatFormData
import com.gsoft.opus.domain.repository.MandatRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MandatFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val numero: String = "",
    val type: String = "",
    val autorite: String = "",
    val personneNom: String = "",
    val dateLieuNaissance: String = "",
    val motif: String = "",
    val qualificationInfraction: String = "",
    val opjExecution: String = "",
    val dateHeureExecution: String = "",
    val lieuExecution: String = "",
    val observations: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class MandatFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MandatRepository
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("mandatId") ?: 0
    val editId: Int get() = entryId

    private val _state = MutableStateFlow(MandatFormUiState(isEdit = entryId > 0))
    val state: StateFlow<MandatFormUiState> = _state.asStateFlow()

    init {
        if (entryId > 0) loadEntry() else loadSuggestedNumber()
    }

    fun loadSuggestedNumber() {
        viewModelScope.launch {
            when (val result = repository.peekMandatNumber()) {
                is Resource.Success -> {
                    if (!_state.value.isEdit) {
                        _state.update { it.copy(numero = result.data) }
                    }
                }
                else -> {}
            }
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getMandat(entryId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            numero = e.numero,
                            type = e.type,
                            autorite = e.autorite ?: "",
                            personneNom = e.personneNom,
                            dateLieuNaissance = e.dateLieuNaissance ?: "",
                            motif = e.motif ?: "",
                            qualificationInfraction = e.qualificationInfraction ?: "",
                            opjExecution = e.opjExecution ?: "",
                            dateHeureExecution = e.dateHeureExecution ?: "",
                            lieuExecution = e.lieuExecution ?: "",
                            observations = e.observations ?: ""
                        )
                    }
                    when (val atts = repository.getMandatAttachments(entryId)) {
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
    fun updateType(value: String) { _state.update { it.copy(type = value) } }
    fun updateAutorite(value: String) { _state.update { it.copy(autorite = value) } }
    fun updatePersonneNom(value: String) { _state.update { it.copy(personneNom = value) } }
    fun updateDateLieuNaissance(value: String) { _state.update { it.copy(dateLieuNaissance = value) } }
    fun updateMotif(value: String) { _state.update { it.copy(motif = value) } }
    fun updateQualificationInfraction(value: String) { _state.update { it.copy(qualificationInfraction = value) } }
    fun updateOpjExecution(value: String) { _state.update { it.copy(opjExecution = value) } }
    fun updateDateHeureExecution(value: String) { _state.update { it.copy(dateHeureExecution = value) } }
    fun updateLieuExecution(value: String) { _state.update { it.copy(lieuExecution = value) } }
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
        val errors = validateMandatForm(s.numero, s.type, s.personneNom, s.dateHeureExecution)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = MandatFormData(
                numero = if (s.isEdit) null else s.numero.trim().ifBlank { null },
                type = s.type,
                autorite = s.autorite.trim().ifBlank { null },
                personneNom = s.personneNom.trim(),
                dateLieuNaissance = s.dateLieuNaissance.trim().ifBlank { null },
                motif = s.motif.trim().ifBlank { null },
                qualificationInfraction = s.qualificationInfraction.trim().ifBlank { null },
                opjExecution = s.opjExecution.trim().ifBlank { null },
                dateHeureExecution = s.dateHeureExecution.trim().ifBlank { null },
                lieuExecution = s.lieuExecution.trim().ifBlank { null },
                observations = s.observations.trim().ifBlank { null }
            )
            val result = if (s.isEdit) repository.updateMandat(entryId, data)
            else repository.createMandat(data)
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
            repository.deleteMandatAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                repository.deleteMandatAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    repository.addMandatAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                repository.updateMandatAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                repository.addMandatAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
