package com.gsoft.opus.presentation.objet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.ObjetSaisiFormData
import com.gsoft.opus.domain.repository.ObjetSaisiRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObjetSaisiFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val numeroDossier: String = "",
    val motif: String = "",
    val typeObjet: String = "TELEPHONE",
    val proprietaire: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ObjetSaisiFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ObjetSaisiRepository
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("objetId") ?: 0
    val editId: Int get() = entryId

    private val _state = MutableStateFlow(ObjetSaisiFormUiState(isEdit = entryId > 0))
    val state: StateFlow<ObjetSaisiFormUiState> = _state.asStateFlow()

    init {
        if (entryId > 0) loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getObjetSaisi(entryId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            numeroDossier = e.numeroDossier ?: "",
                            motif = e.motif,
                            typeObjet = e.typeObjet,
                            proprietaire = e.proprietaire ?: ""
                        )
                    }
                    when (val atts = repository.getObjetSaisiAttachments(entryId)) {
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

    fun updateNumeroDossier(value: String) { _state.update { it.copy(numeroDossier = value) } }
    fun updateMotif(value: String) { _state.update { it.copy(motif = value) } }
    fun updateTypeObjet(value: String) { _state.update { it.copy(typeObjet = value) } }
    fun updateProprietaire(value: String) { _state.update { it.copy(proprietaire = value) } }

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
        val errors = validateObjetSaisiForm(s.typeObjet, s.motif)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = ObjetSaisiFormData(
                numeroDossier = s.numeroDossier.trim().ifBlank { null },
                motif = s.motif.trim(),
                typeObjet = s.typeObjet,
                proprietaire = s.proprietaire.trim().ifBlank { null }
            )
            val result = if (s.isEdit) repository.updateObjetSaisi(entryId, data)
            else repository.createObjetSaisi(data)
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
            repository.deleteObjetSaisiAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                repository.deleteObjetSaisiAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    repository.addObjetSaisiAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                repository.updateObjetSaisiAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                repository.addObjetSaisiAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
