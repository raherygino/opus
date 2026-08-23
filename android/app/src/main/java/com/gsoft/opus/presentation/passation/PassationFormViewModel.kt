package com.gsoft.opus.presentation.passation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Passation
import com.gsoft.opus.domain.model.VerifiedIdentity
import com.gsoft.opus.domain.repository.PassationFormData
import com.gsoft.opus.domain.repository.PassationRepository
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

data class PassationFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isVerifying: Boolean = false,
    val datePassation: String = "",
    val heure: String = "",
    val instructionsAutorite: String = "",
    val incidentsSurvenus: String = "",
    val montantIdentity: VerifiedIdentity? = null,
    val montantUsername: String = "",
    val montantPassword: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val chefDescendantDisplay: String = ""
)

@HiltViewModel
class PassationFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val passationRepository: PassationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val passationId: Int = savedStateHandle.get<Int>("passationId") ?: 0
    val editId: Int get() = passationId

    private val _state = MutableStateFlow(
        PassationFormUiState(
            isEdit = passationId > 0,
            datePassation = if (passationId > 0) "" else todayIso(),
            heure = if (passationId > 0) "" else nowHHmm()
        )
    )
    val state: StateFlow<PassationFormUiState> = _state.asStateFlow()

    init {
        loadChefDescendantDisplay()
        if (passationId > 0) {
            loadPassation()
        }
    }

    private fun loadChefDescendantDisplay() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            if (user != null) {
                val display = listOfNotNull(user.grade, user.firstName, user.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { user.username }
                _state.update { it.copy(chefDescendantDisplay = display) }
            }
        }
    }

    private fun loadPassation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = passationRepository.getPassation(passationId)) {
                is Resource.Success -> {
                    val p = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            datePassation = p.datePassation.take(10),
                            heure = p.heureDisplay,
                            instructionsAutorite = p.instructionsAutorite ?: "",
                            incidentsSurvenus = p.incidentsSurvenus ?: "",
                            montantIdentity = VerifiedIdentity(
                                id = p.chefMontantUserId ?: 0,
                                username = p.chefMontantUsername ?: "",
                                grade = p.chefMontantGrade,
                                firstname = null,
                                lastname = p.chefMontantLastname
                            )
                        )
                    }
                    when (val atts = passationRepository.getAttachments(passationId)) {
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

    fun updateDatePassation(value: String) { _state.update { it.copy(datePassation = value) } }
    fun updateHeure(value: String) { _state.update { it.copy(heure = value) } }
    fun updateInstructionsAutorite(value: String) { _state.update { it.copy(instructionsAutorite = value) } }
    fun updateIncidentsSurvenus(value: String) { _state.update { it.copy(incidentsSurvenus = value) } }
    fun updateMontantUsername(value: String) { _state.update { it.copy(montantUsername = value) } }
    fun updateMontantPassword(value: String) { _state.update { it.copy(montantPassword = value) } }

    /** Authenticate the chef de poste montant via /auth/verify. */
    fun verifyMontant() {
        val s = _state.value
        if (s.montantUsername.isBlank() || s.montantPassword.isBlank()) {
            _state.update { it.copy(errorMessage = "Identifiant et mot de passe requis") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isVerifying = true, errorMessage = null) }
            when (val result = passationRepository.verifyIdentity(s.montantUsername.trim(), s.montantPassword)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isVerifying = false,
                        montantIdentity = result.data,
                        montantPassword = "", // Never keep the password in state
                        errorMessage = null
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isVerifying = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetMontant() {
        _state.update {
            it.copy(
                montantIdentity = null,
                montantUsername = "",
                montantPassword = ""
            )
        }
    }

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
        val validationError = PassationFormValidator.validate(
            datePassation = s.datePassation,
            heurePassation = s.heure,
            montantVerified = s.montantIdentity != null
        )
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }
        val identity = s.montantIdentity ?: run {
            _state.update { it.copy(errorMessage = "Le chef de poste montant doit être authentifié") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = PassationFormData(
                datePassation = s.datePassation,
                heurePassation = s.heure,
                chefMontantUserId = identity.id,
                chefMontantGrade = identity.grade ?: "",
                // Store the full name (firstname + lastname) in the lastname
                // field so both chefs display consistently with a single name
                // column.
                chefMontantLastname = listOfNotNull(identity.firstname, identity.lastname)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .trim(),
                instructionsAutorite = s.instructionsAutorite,
                incidentsSurvenus = s.incidentsSurvenus
            )
            if (s.isEdit) {
                when (val res = passationRepository.updatePassation(passationId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = passationRepository.createPassation(data)) {
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
            passationRepository.deleteAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                passationRepository.deleteAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    passationRepository.addAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                passationRepository.updateAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                passationRepository.addAttachment(savedId, a.title, a.uploadFile)
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
