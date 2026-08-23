package com.gsoft.opus.presentation.declarationperte

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.DeclarationPerteFormData
import com.gsoft.opus.domain.repository.DeclarationPerteRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
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

data class DeclarationPerteFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val dateDeclaration: String = "",
    val heure: String = "",
    val identiteDeclarant: String = "",
    val natureObjet: String = "",
    val descriptionObjet: String = "",
    val datePerte: String = "",
    val lieuPerte: String = "",
    val numeroAttestation: String = "",
    val nomAgent: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class DeclarationPerteFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val declarationPerteRepository: DeclarationPerteRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val declarationId: Int = savedStateHandle.get<Int>("declarationId") ?: 0
    val editId: Int get() = declarationId

    private val _state = MutableStateFlow(
        DeclarationPerteFormUiState(
            isEdit = declarationId > 0,
            dateDeclaration = if (declarationId > 0) "" else todayIso(),
            heure = if (declarationId > 0) "" else nowHHmm()
        )
    )
    val state: StateFlow<DeclarationPerteFormUiState> = _state.asStateFlow()

    init {
        if (declarationId > 0) {
            loadDeclaration()
        } else {
            prefillNomAgent()
        }
    }

    /** Prefill the agent name with the current user's name (falling back to username). */
    private fun prefillNomAgent() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull() ?: return@launch
            val displayName = listOfNotNull(user.firstName, user.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { user.username }
            _state.update { it.copy(nomAgent = displayName) }
        }
    }

    private fun loadDeclaration() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = declarationPerteRepository.getDeclarationPerte(declarationId)) {
                is Resource.Success -> {
                    val d = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            dateDeclaration = d.dateDeclaration.take(10),
                            heure = d.heureDisplay,
                            identiteDeclarant = d.identiteDeclarant,
                            natureObjet = d.natureObjet,
                            descriptionObjet = d.descriptionObjet,
                            datePerte = d.datePerte.take(10),
                            lieuPerte = d.lieuPerte,
                            numeroAttestation = d.numeroAttestation,
                            nomAgent = d.nomAgent
                        )
                    }
                    when (val atts = declarationPerteRepository.getAttachments(declarationId)) {
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

    fun updateDateDeclaration(value: String) { _state.update { it.copy(dateDeclaration = value) } }
    fun updateHeure(value: String) { _state.update { it.copy(heure = value) } }
    fun updateIdentiteDeclarant(value: String) { _state.update { it.copy(identiteDeclarant = value) } }
    fun updateNatureObjet(value: String) { _state.update { it.copy(natureObjet = value) } }
    fun updateDescriptionObjet(value: String) { _state.update { it.copy(descriptionObjet = value) } }
    fun updateDatePerte(value: String) { _state.update { it.copy(datePerte = value) } }
    fun updateLieuPerte(value: String) { _state.update { it.copy(lieuPerte = value) } }
    fun updateNumeroAttestation(value: String) { _state.update { it.copy(numeroAttestation = value) } }
    fun updateNomAgent(value: String) { _state.update { it.copy(nomAgent = value) } }

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
        val validationError = DeclarationPerteFormValidator.validate(
            dateDeclaration = s.dateDeclaration,
            heure = s.heure,
            identiteDeclarant = s.identiteDeclarant,
            natureObjet = s.natureObjet,
            descriptionObjet = s.descriptionObjet,
            datePerte = s.datePerte,
            lieuPerte = s.lieuPerte,
            numeroAttestation = s.numeroAttestation,
            nomAgent = s.nomAgent
        )
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = DeclarationPerteFormData(
                dateDeclaration = s.dateDeclaration,
                heureDeclaration = s.heure,
                identiteDeclarant = s.identiteDeclarant,
                natureObjet = s.natureObjet,
                descriptionObjet = s.descriptionObjet,
                datePerte = s.datePerte,
                lieuPerte = s.lieuPerte,
                numeroAttestation = s.numeroAttestation,
                nomAgent = s.nomAgent
            )
            if (s.isEdit) {
                when (val res = declarationPerteRepository.updateDeclarationPerte(declarationId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = declarationPerteRepository.createDeclarationPerte(data)) {
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
            declarationPerteRepository.deleteAttachment(savedId, a.id!!)
        }
        // Add/update non-deleted attachments
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                // Replace: delete then re-create
                declarationPerteRepository.deleteAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    declarationPerteRepository.addAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                declarationPerteRepository.updateAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                declarationPerteRepository.addAttachment(savedId, a.title, a.uploadFile)
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
