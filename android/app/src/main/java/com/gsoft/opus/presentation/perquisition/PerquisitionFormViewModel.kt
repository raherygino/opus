package com.gsoft.opus.presentation.perquisition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.PerquisitionFormData
import com.gsoft.opus.domain.repository.PerquisitionRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerquisitionFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val numero: String = "",
    val numeroTtr: String = "",
    val substitut: String = "",
    val affaire: String = "",
    val motif: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class PerquisitionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PerquisitionRepository
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("perquisitionId") ?: 0
    val editId: Int get() = entryId

    private val _state = MutableStateFlow(PerquisitionFormUiState(isEdit = entryId > 0))
    val state: StateFlow<PerquisitionFormUiState> = _state.asStateFlow()

    init {
        if (entryId > 0) loadEntry() else peekNumero()
    }

    private fun peekNumero() {
        viewModelScope.launch {
            when (val res = repository.peekNextNumber()) {
                is Resource.Success -> _state.update { it.copy(numero = res.data) }
                else -> {}
            }
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getPerquisition(entryId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            numero = e.numero,
                            numeroTtr = e.numeroTtr ?: "",
                            substitut = e.substitut ?: "",
                            affaire = e.affaire,
                            motif = e.motif ?: ""
                        )
                    }
                    when (val atts = repository.getPerquisitionAttachments(entryId)) {
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
    fun updateNumeroTtr(value: String) { _state.update { it.copy(numeroTtr = value) } }
    fun updateSubstitut(value: String) { _state.update { it.copy(substitut = value) } }
    fun updateAffaire(value: String) { _state.update { it.copy(affaire = value) } }
    fun updateMotif(value: String) { _state.update { it.copy(motif = value) } }

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
        val errors = validatePerquisitionForm(s.affaire)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = PerquisitionFormData(
                numero = s.numero.trim().ifBlank { null },
                numeroTtr = s.numeroTtr.trim().ifBlank { null },
                substitut = s.substitut.trim().ifBlank { null },
                affaire = s.affaire.trim(),
                motif = s.motif.trim().ifBlank { null }
            )
            val result = if (s.isEdit) repository.updatePerquisition(entryId, data)
            else repository.createPerquisition(data)
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
            repository.deletePerquisitionAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                repository.deletePerquisitionAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    repository.addPerquisitionAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                repository.updatePerquisitionAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                repository.addPerquisitionAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
