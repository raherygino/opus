package com.gsoft.opus.presentation.requisition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.RequisitionFormData
import com.gsoft.opus.domain.repository.RequisitionRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequisitionFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val type: String = "TPH",
    val dateRequisition: String = "",
    val numero: String = "",
    val numeroTtr: String = "",
    val nomSubstitut: String = "",
    val affaire: String = "",
    val numeroDossier: String = "",
    val opj: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class RequisitionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val requisitionRepository: RequisitionRepository
) : ViewModel() {

    private val requisitionId: Int = savedStateHandle.get<Int>("requisitionId") ?: 0
    val editId: Int get() = requisitionId

    private val _state = MutableStateFlow(
        RequisitionFormUiState(isEdit = requisitionId > 0)
    )
    val state: StateFlow<RequisitionFormUiState> = _state.asStateFlow()

    init {
        if (requisitionId > 0) {
            loadRequisition()
        } else {
            initCreateDefaults()
        }
    }

    private fun initCreateDefaults() {
        _state.update {
            it.copy(dateRequisition = com.gsoft.opus.presentation.personnel.millisToIsoDate(System.currentTimeMillis()))
        }
        refreshSuggestedNumber()
    }

    fun refreshSuggestedNumber() {
        viewModelScope.launch {
            when (val res = requisitionRepository.peekRequisitionNumber()) {
                is Resource.Success -> _state.update { it.copy(numero = res.data) }
                else -> {}
            }
        }
    }

    private fun loadRequisition() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = requisitionRepository.getRequisition(requisitionId)) {
                is Resource.Success -> {
                    val r = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            type = r.type,
                            dateRequisition = r.dateRequisition.take(10),
                            numero = r.numero,
                            numeroTtr = r.numeroTtr ?: "",
                            nomSubstitut = r.nomSubstitut ?: "",
                            affaire = r.affaire,
                            numeroDossier = r.numeroDossier ?: "",
                            opj = r.opj ?: ""
                        )
                    }
                    when (val atts = requisitionRepository.getRequisitionAttachments(requisitionId)) {
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

    fun updateType(value: String) { _state.update { it.copy(type = value) } }
    fun updateDateRequisition(value: String) { _state.update { it.copy(dateRequisition = value) } }
    fun updateNumero(value: String) { _state.update { it.copy(numero = value) } }
    fun updateNumeroTtr(value: String) { _state.update { it.copy(numeroTtr = value) } }
    fun updateNomSubstitut(value: String) { _state.update { it.copy(nomSubstitut = value) } }
    fun updateAffaire(value: String) { _state.update { it.copy(affaire = value) } }
    fun updateNumeroDossier(value: String) { _state.update { it.copy(numeroDossier = value) } }
    fun updateOpj(value: String) { _state.update { it.copy(opj = value) } }

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
        val errors = validateRequisitionForm(s.type, s.dateRequisition, s.affaire)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = RequisitionFormData(
                type = s.type,
                dateRequisition = s.dateRequisition.trim(),
                numero = if (s.isEdit) null else s.numero.trim().ifBlank { null },
                numeroTtr = s.numeroTtr.trim().ifBlank { null },
                nomSubstitut = s.nomSubstitut.trim().ifBlank { null },
                affaire = s.affaire.trim(),
                numeroDossier = s.numeroDossier.trim().ifBlank { null },
                opj = s.opj.trim().ifBlank { null }
            )
            if (s.isEdit) {
                when (val res = requisitionRepository.updateRequisition(requisitionId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = requisitionRepository.createRequisition(data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private suspend fun handleAttachments(savedId: Int) {
        val s = _state.value
        for (a in s.attachments.filter { it.isDeleted && it.id != null }) {
            requisitionRepository.deleteRequisitionAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                requisitionRepository.deleteRequisitionAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    requisitionRepository.addRequisitionAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                requisitionRepository.updateRequisitionAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                requisitionRepository.addRequisitionAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
