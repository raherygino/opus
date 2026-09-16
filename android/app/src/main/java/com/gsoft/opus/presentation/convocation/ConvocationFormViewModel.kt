package com.gsoft.opus.presentation.convocation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.ConvocationFormData
import com.gsoft.opus.domain.repository.ConvocationRepository
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

data class ConvocationFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val type: String = "ST_PARQUET",
    val dateConvocation: String = "",
    val numero: String = "",
    val nom: String = "",
    val adresse: String = "",
    val infraction: String = "",
    val personneAccuseRecu: String = "",
    val numeroDossier: String = "",
    val observation: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ConvocationFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val convocationRepository: ConvocationRepository
) : ViewModel() {

    private val convocationId: Int = savedStateHandle.get<Int>("convocationId") ?: 0
    val editId: Int get() = convocationId

    private val _state = MutableStateFlow(
        ConvocationFormUiState(
            isEdit = convocationId > 0,
            dateConvocation = if (convocationId > 0) "" else todayIso()
        )
    )
    val state: StateFlow<ConvocationFormUiState> = _state.asStateFlow()

    init {
        if (convocationId > 0) loadConvocation()
        else loadSuggestedNumber()
    }

    private fun loadSuggestedNumber() {
        viewModelScope.launch {
            when (val result = convocationRepository.peekConvocationNumber(_state.value.type)) {
                is Resource.Success -> _state.update { it.copy(numero = result.data) }
                else -> {}
            }
        }
    }

    fun refreshSuggestedNumber() {
        if (!_state.value.isEdit) loadSuggestedNumber()
    }

    private fun loadConvocation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = convocationRepository.getConvocation(convocationId)) {
                is Resource.Success -> {
                    val c = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            type = c.type,
                            dateConvocation = c.dateConvocation.take(10),
                            numero = c.numero,
                            nom = c.nom,
                            adresse = c.adresse ?: "",
                            infraction = c.infraction ?: "",
                            personneAccuseRecu = c.personneAccuseRecu ?: "",
                            numeroDossier = c.numeroDossier ?: "",
                            observation = c.observation ?: ""
                        )
                    }
                    when (val atts = convocationRepository.getConvocationAttachments(convocationId)) {
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

    fun updateType(value: String) {
        _state.update { it.copy(type = value) }
        // Refresh the suggested number when the type changes (only on create).
        if (!_state.value.isEdit) loadSuggestedNumber()
    }
    fun updateDateConvocation(value: String) { _state.update { it.copy(dateConvocation = value) } }
    fun updateNumero(value: String) { _state.update { it.copy(numero = value) } }
    fun updateNom(value: String) { _state.update { it.copy(nom = value) } }
    fun updateAdresse(value: String) { _state.update { it.copy(adresse = value) } }
    fun updateInfraction(value: String) { _state.update { it.copy(infraction = value) } }
    fun updatePersonneAccuseRecu(value: String) { _state.update { it.copy(personneAccuseRecu = value) } }
    fun updateNumeroDossier(value: String) { _state.update { it.copy(numeroDossier = value) } }
    fun updateObservation(value: String) { _state.update { it.copy(observation = value) } }

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
        val errors = validateConvocationForm(
            type = s.type,
            dateConvocation = s.dateConvocation,
            nom = s.nom,
            infraction = s.infraction
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = ConvocationFormData(
                type = s.type,
                dateConvocation = s.dateConvocation,
                numero = s.numero.takeIf { it.isNotBlank() },
                nom = s.nom.trim(),
                adresse = s.adresse.trim().ifBlank { null },
                infraction = s.infraction.trim().ifBlank { null },
                personneAccuseRecu = s.personneAccuseRecu.trim().ifBlank { null },
                numeroDossier = s.numeroDossier.trim().ifBlank { null },
                observation = s.observation.trim().ifBlank { null }
            )
            if (s.isEdit) {
                when (val res = convocationRepository.updateConvocation(convocationId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = convocationRepository.createConvocation(data)) {
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
            convocationRepository.deleteConvocationAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                convocationRepository.deleteConvocationAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    convocationRepository.addConvocationAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                convocationRepository.updateConvocationAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                convocationRepository.addConvocationAttachment(savedId, a.title, a.uploadFile)
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
