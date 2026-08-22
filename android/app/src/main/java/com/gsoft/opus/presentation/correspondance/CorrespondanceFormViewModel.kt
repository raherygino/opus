package com.gsoft.opus.presentation.correspondance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.CorrespondanceFormData
import com.gsoft.opus.domain.repository.CorrespondanceRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CorrespondanceFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val dateCorrespondance: String = "",
    val heure: String = "",
    val sens: String = "",
    val reference: String = "",
    val emetteurDestinataire: String = "",
    val objet: String = "",
    val statut: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class CorrespondanceFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val correspondanceRepository: CorrespondanceRepository
) : ViewModel() {

    private val correspondanceId: Int = savedStateHandle.get<Int>("correspondanceId") ?: 0
    val editId: Int get() = correspondanceId

    private val _state = MutableStateFlow(
        CorrespondanceFormUiState(
            isEdit = correspondanceId > 0,
            dateCorrespondance = if (correspondanceId > 0) "" else todayIso(),
            heure = if (correspondanceId > 0) "" else nowHHmm(),
            sens = if (correspondanceId > 0) "" else CORRESPONDANCE_SENS.first(),
            statut = if (correspondanceId > 0) "" else CORRESPONDANCE_STATUTS.first()
        )
    )
    val state: StateFlow<CorrespondanceFormUiState> = _state.asStateFlow()

    init {
        if (correspondanceId > 0) loadCorrespondance()
    }

    private fun loadCorrespondance() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = correspondanceRepository.getCorrespondance(correspondanceId)) {
                is Resource.Success -> {
                    val c = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            dateCorrespondance = c.dateCorrespondance.take(10),
                            heure = c.heureDisplay,
                            sens = c.sens,
                            reference = c.reference,
                            emetteurDestinataire = c.emetteurDestinataire,
                            objet = c.objet,
                            statut = c.statut
                        )
                    }
                    when (val atts = correspondanceRepository.getAttachments(correspondanceId)) {
                        is Resource.Success -> _state.update {
                            it.copy(attachments = atts.data.map { a -> AttachmentItem(id = a.id, title = a.title, existingFilename = a.originalFilename) })
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateDateCorrespondance(value: String) { _state.update { it.copy(dateCorrespondance = value) } }
    fun updateHeure(value: String) { _state.update { it.copy(heure = value) } }
    fun updateSens(value: String) { _state.update { it.copy(sens = value) } }
    fun updateReference(value: String) { _state.update { it.copy(reference = value) } }
    fun updateEmetteurDestinataire(value: String) { _state.update { it.copy(emetteurDestinataire = value) } }
    fun updateObjet(value: String) { _state.update { it.copy(objet = value) } }
    fun updateStatut(value: String) { _state.update { it.copy(statut = value) } }

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
        val validationError = CorrespondanceFormValidator.validate(
            dateCorrespondance = s.dateCorrespondance,
            heure = s.heure,
            sens = s.sens,
            reference = s.reference,
            emetteurDestinataire = s.emetteurDestinataire,
            objet = s.objet
        )
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = CorrespondanceFormData(
                dateCorrespondance = s.dateCorrespondance,
                heureEnregistrement = s.heure,
                sens = s.sens,
                reference = s.reference,
                emetteurDestinataire = s.emetteurDestinataire,
                objet = s.objet,
                statut = s.statut.ifBlank { null }
            )
            if (s.isEdit) {
                when (val res = correspondanceRepository.updateCorrespondance(correspondanceId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = correspondanceRepository.createCorrespondance(data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private suspend fun handleAttachments(savedId: Int) {
        val s = _state.value
        // Delete marked attachments
        for (a in s.attachments.filter { it.isDeleted && it.id != null }) {
            correspondanceRepository.deleteAttachment(savedId, a.id!!)
        }
        // Add/update non-deleted attachments
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                // Replace: delete then re-create
                correspondanceRepository.deleteAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    correspondanceRepository.addAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                correspondanceRepository.updateAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                correspondanceRepository.addAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

private fun todayIso(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun nowHHmm(): String =
    SimpleDateFormat("HH:mm", Locale.US).format(Date())
