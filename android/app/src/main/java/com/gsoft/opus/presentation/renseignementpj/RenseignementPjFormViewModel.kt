package com.gsoft.opus.presentation.renseignementpj

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.RenseignementPjFormData
import com.gsoft.opus.domain.repository.RenseignementPjRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RenseignementPjFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val natureInfraction: String = "",
    val dateLieuFaits: String = "",
    val circonstances: String = "",
    val prejudices: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class RenseignementPjFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RenseignementPjRepository
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("renseignementId") ?: 0
    val editId: Int get() = entryId

    private val _state = MutableStateFlow(RenseignementPjFormUiState(isEdit = entryId > 0))
    val state: StateFlow<RenseignementPjFormUiState> = _state.asStateFlow()

    init {
        if (entryId > 0) loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getRenseignementPj(entryId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            natureInfraction = e.natureInfraction,
                            dateLieuFaits = e.dateLieuFaits ?: "",
                            circonstances = e.circonstances ?: "",
                            prejudices = e.prejudices ?: ""
                        )
                    }
                    when (val atts = repository.getRenseignementPjAttachments(entryId)) {
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

    fun updateNatureInfraction(value: String) { _state.update { it.copy(natureInfraction = value) } }
    fun updateDateLieuFaits(value: String) { _state.update { it.copy(dateLieuFaits = value) } }
    fun updateCirconstances(value: String) { _state.update { it.copy(circonstances = value) } }
    fun updatePrejudices(value: String) { _state.update { it.copy(prejudices = value) } }

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
        val errors = validateRenseignementPjForm(s.natureInfraction)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = RenseignementPjFormData(
                natureInfraction = s.natureInfraction.trim(),
                dateLieuFaits = s.dateLieuFaits.trim().ifBlank { null },
                circonstances = s.circonstances.trim().ifBlank { null },
                prejudices = s.prejudices.trim().ifBlank { null }
            )
            val result = if (s.isEdit) repository.updateRenseignementPj(entryId, data)
            else repository.createRenseignementPj(data)
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
            repository.deleteRenseignementPjAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                repository.deleteRenseignementPjAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    repository.addRenseignementPjAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                repository.updateRenseignementPjAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                repository.addRenseignementPjAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
