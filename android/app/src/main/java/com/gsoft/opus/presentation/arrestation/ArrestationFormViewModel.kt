package com.gsoft.opus.presentation.arrestation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.ArrestationFormData
import com.gsoft.opus.domain.repository.ArrestationRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArrestationFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val numero: String = "",
    val dateHeureArrestation: String = "",
    val personneNom: String = "",
    val lieuArrestation: String = "",
    val motif: String = "",
    val policiers: String = "",
    val numeroDossier: String = "",
    val observations: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ArrestationFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ArrestationRepository
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("arrestationId") ?: 0
    val editId: Int get() = entryId

    private val _state = MutableStateFlow(ArrestationFormUiState(isEdit = entryId > 0))
    val state: StateFlow<ArrestationFormUiState> = _state.asStateFlow()

    init {
        if (entryId > 0) loadEntry() else loadSuggestedNumber()
    }

    fun loadSuggestedNumber() {
        viewModelScope.launch {
            when (val result = repository.peekArrestationNumber()) {
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
            when (val result = repository.getArrestation(entryId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            numero = e.numero,
                            dateHeureArrestation = e.dateHeureArrestation ?: "",
                            personneNom = e.personneNom,
                            lieuArrestation = e.lieuArrestation ?: "",
                            motif = e.motif ?: "",
                            policiers = e.policiers ?: "",
                            numeroDossier = e.numeroDossier ?: "",
                            observations = e.observations ?: ""
                        )
                    }
                    when (val atts = repository.getArrestationAttachments(entryId)) {
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
    fun updateDateHeureArrestation(value: String) { _state.update { it.copy(dateHeureArrestation = value) } }
    fun updatePersonneNom(value: String) { _state.update { it.copy(personneNom = value) } }
    fun updateLieuArrestation(value: String) { _state.update { it.copy(lieuArrestation = value) } }
    fun updateMotif(value: String) { _state.update { it.copy(motif = value) } }
    fun updatePoliciers(value: String) { _state.update { it.copy(policiers = value) } }
    fun updateNumeroDossier(value: String) { _state.update { it.copy(numeroDossier = value) } }
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
        val errors = validateArrestationForm(s.personneNom, s.dateHeureArrestation)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = ArrestationFormData(
                numero = if (s.isEdit) null else s.numero.trim().ifBlank { null },
                dateHeureArrestation = s.dateHeureArrestation.trim(),
                personneNom = s.personneNom.trim(),
                lieuArrestation = s.lieuArrestation.trim().ifBlank { null },
                motif = s.motif.trim().ifBlank { null },
                policiers = s.policiers.trim().ifBlank { null },
                numeroDossier = s.numeroDossier.trim().ifBlank { null },
                observations = s.observations.trim().ifBlank { null }
            )
            val result = if (s.isEdit) repository.updateArrestation(entryId, data)
            else repository.createArrestation(data)
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
            repository.deleteArrestationAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                repository.deleteArrestationAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    repository.addArrestationAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                repository.updateArrestationAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                repository.addArrestationAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
