package com.gsoft.opus.presentation.objet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.ObjetTrouveFormData
import com.gsoft.opus.domain.repository.ObjetTrouveRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObjetTrouveFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val affaire: String = "",
    val motifDecouverte: String = "REQUISITION",
    val restitution: Boolean = false,
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ObjetTrouveFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ObjetTrouveRepository
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("objetId") ?: 0
    val editId: Int get() = entryId

    private val _state = MutableStateFlow(ObjetTrouveFormUiState(isEdit = entryId > 0))
    val state: StateFlow<ObjetTrouveFormUiState> = _state.asStateFlow()

    init {
        if (entryId > 0) loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getObjetTrouve(entryId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            affaire = e.affaire,
                            motifDecouverte = e.motifDecouverte,
                            restitution = e.restitution
                        )
                    }
                    when (val atts = repository.getObjetTrouveAttachments(entryId)) {
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

    fun updateAffaire(value: String) { _state.update { it.copy(affaire = value) } }
    fun updateMotifDecouverte(value: String) { _state.update { it.copy(motifDecouverte = value) } }
    fun updateRestitution(value: Boolean) { _state.update { it.copy(restitution = value) } }

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
        val errors = validateObjetTrouveForm(s.affaire, s.motifDecouverte)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = ObjetTrouveFormData(
                affaire = s.affaire.trim(),
                motifDecouverte = s.motifDecouverte,
                restitution = s.restitution
            )
            val result = if (s.isEdit) repository.updateObjetTrouve(entryId, data)
            else repository.createObjetTrouve(data)
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
            repository.deleteObjetTrouveAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                repository.deleteObjetTrouveAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    repository.addObjetTrouveAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                repository.updateObjetTrouveAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                repository.addObjetTrouveAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
