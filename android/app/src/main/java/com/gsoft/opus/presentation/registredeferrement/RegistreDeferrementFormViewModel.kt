package com.gsoft.opus.presentation.registredeferrement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.RegistreDeferrementFormData
import com.gsoft.opus.domain.repository.RegistreDeferrementRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RegistreDeferrementFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val numero: String = "",
    val dateHeureDeferrement: String = "",
    val personneNom: String = "",
    val dateLieuNaissance: String = "",
    val infraction: String = "",
    val numeroDossier: String = "",
    val autorite: String = "",
    val destination: String = "",
    val escorte: String = "",
    val suiteDonnee: String = "",
    val observations: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class RegistreDeferrementFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RegistreDeferrementRepository
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("registreDeferrementId") ?: 0
    val editId: Int get() = entryId

    private val _state = MutableStateFlow(RegistreDeferrementFormUiState(isEdit = entryId > 0))
    val state: StateFlow<RegistreDeferrementFormUiState> = _state.asStateFlow()

    init {
        if (entryId > 0) loadEntry() else {
            peekNumero()
            // Pre-fill with the current date/time like the desktop form does.
            _state.update {
                it.copy(
                    dateHeureDeferrement = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
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
            when (val result = repository.getDeferrement(entryId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            numero = e.numero,
                            dateHeureDeferrement = e.dateHeureDeferrement,
                            personneNom = e.personneNom,
                            dateLieuNaissance = e.dateLieuNaissance ?: "",
                            infraction = e.infraction ?: "",
                            numeroDossier = e.numeroDossier ?: "",
                            autorite = e.autorite ?: "",
                            destination = e.destination ?: "",
                            escorte = e.escorte ?: "",
                            suiteDonnee = e.suiteDonnee ?: "",
                            observations = e.observations ?: ""
                        )
                    }
                    when (val atts = repository.getDeferrementAttachments(entryId)) {
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
    fun updateDateHeureDeferrement(value: String) { _state.update { it.copy(dateHeureDeferrement = value) } }
    fun updatePersonneNom(value: String) { _state.update { it.copy(personneNom = value) } }
    fun updateDateLieuNaissance(value: String) { _state.update { it.copy(dateLieuNaissance = value) } }
    fun updateInfraction(value: String) { _state.update { it.copy(infraction = value) } }
    fun updateNumeroDossier(value: String) { _state.update { it.copy(numeroDossier = value) } }
    fun updateAutorite(value: String) { _state.update { it.copy(autorite = value) } }
    fun updateDestination(value: String) { _state.update { it.copy(destination = value) } }
    fun updateEscorte(value: String) { _state.update { it.copy(escorte = value) } }
    fun updateSuiteDonnee(value: String) { _state.update { it.copy(suiteDonnee = value) } }
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
        val errors = validateDeferrementForm(s.personneNom, s.dateHeureDeferrement)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = RegistreDeferrementFormData(
                numero = s.numero.trim().ifBlank { null },
                dateHeureDeferrement = s.dateHeureDeferrement.trim(),
                personneNom = s.personneNom.trim(),
                dateLieuNaissance = s.dateLieuNaissance.trim().ifBlank { null },
                infraction = s.infraction.trim().ifBlank { null },
                numeroDossier = s.numeroDossier.trim().ifBlank { null },
                autorite = s.autorite.trim().ifBlank { null },
                destination = s.destination.trim().ifBlank { null },
                escorte = s.escorte.trim().ifBlank { null },
                suiteDonnee = s.suiteDonnee.trim().ifBlank { null },
                observations = s.observations.trim().ifBlank { null }
            )
            val result = if (s.isEdit) repository.updateDeferrement(entryId, data)
            else repository.createDeferrement(data)
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
            repository.deleteDeferrementAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                repository.deleteDeferrementAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    repository.addDeferrementAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                repository.updateDeferrementAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                repository.addDeferrementAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
