package com.gsoft.opus.presentation.armement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.repository.ArmementFormData
import com.gsoft.opus.domain.repository.ArmementRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
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

data class ArmementFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val datePerception: String = "",
    val heure: String = "",
    val agentPreneurPersonnelId: Int = 0,
    val personnelOptions: List<Personnel> = emptyList(),
    val typeArme: String = "",
    val matriculeArme: String = "",
    val munitions: String = "",
    val secteurMission: String = "",
    val etatPerception: String = "",
    // Agent verification (create only — set at perception time, one-way).
    val codeSecret: String = "",
    val verifying: Boolean = false,
    val verified: Boolean = false,
    val verifyError: String? = null,
    // Signature SVG (captured after verification).
    val signatureSvg: String? = null,
    // Whether the current signature came from the personnel's data (vs drawn).
    val signatureFromPersonnel: Boolean = false,
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ArmementFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val armementRepository: ArmementRepository,
    private val personnelRepository: PersonnelRepository
) : ViewModel() {

    private val armementId: Int = savedStateHandle.get<Int>("armementId") ?: 0
    val editId: Int get() = armementId

    private val _state = MutableStateFlow(
        ArmementFormUiState(
            isEdit = armementId > 0,
            datePerception = if (armementId > 0) "" else todayIso(),
            heure = if (armementId > 0) "" else nowHHmm()
        )
    )
    val state: StateFlow<ArmementFormUiState> = _state.asStateFlow()

    init {
        loadPersonnel()
        if (armementId > 0) {
            loadArmement()
        }
    }

    private fun loadPersonnel() {
        viewModelScope.launch {
            when (val result = personnelRepository.getPersonnelList()) {
                is Resource.Success -> _state.update { it.copy(personnelOptions = result.data) }
                else -> {}
            }
        }
    }

    private fun loadArmement() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = armementRepository.getArmement(armementId)) {
                is Resource.Success -> {
                    val a = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            datePerception = a.datePerception.take(10),
                            heure = a.heurePerceptionDisplay,
                            agentPreneurPersonnelId = a.agentPreneurPersonnelId ?: 0,
                            typeArme = a.typeArme,
                            matriculeArme = a.matriculeArme,
                            munitions = a.munitions?.toString() ?: "",
                            secteurMission = a.secteurMission ?: "",
                            etatPerception = a.etatPerception ?: "",
                            verified = a.agentVerifie,
                            signatureSvg = a.signatureSvg
                        )
                    }
                    when (val atts = armementRepository.getAttachments(armementId)) {
                        is Resource.Success -> _state.update {
                            it.copy(attachments = atts.data.map { x -> AttachmentItem(id = x.id, title = x.title, existingFilename = x.originalFilename) })
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateDatePerception(value: String) { _state.update { it.copy(datePerception = value) } }
    fun updateHeure(value: String) { _state.update { it.copy(heure = value) } }
    fun updateAgentPreneur(personnelId: Int) {
        // Reset verification state when the agent changes.
        _state.update {
            it.copy(
                agentPreneurPersonnelId = personnelId,
                verified = false,
                verifyError = null,
                codeSecret = "",
                signatureSvg = null
            )
        }
    }
    fun updateTypeArme(value: String) { _state.update { it.copy(typeArme = value) } }
    fun updateMatriculeArme(value: String) { _state.update { it.copy(matriculeArme = value) } }
    fun updateMunitions(value: String) { _state.update { it.copy(munitions = value) } }
    fun updateSecteurMission(value: String) { _state.update { it.copy(secteurMission = value) } }
    fun updateEtatPerception(value: String) { _state.update { it.copy(etatPerception = value) } }
    fun updateCodeSecret(value: String) { _state.update { it.copy(codeSecret = value) } }
    fun setSignatureSvg(svg: String?) {
        _state.update { it.copy(signatureSvg = svg, signatureFromPersonnel = false) }
    }

    /**
     * Verify the agent preneur's code secret against the server. On success,
     * the perception can be created. On failure, the verification error is
     * shown and the perception cannot be submitted.
     */
    fun verifyCode() {
        val s = _state.value
        if (s.agentPreneurPersonnelId <= 0) {
            _state.update { it.copy(verifyError = "Sélectionnez d'abord un agent") }
            return
        }
        if (s.codeSecret.isBlank()) {
            _state.update { it.copy(verifyError = "Saisissez le code secret de l'agent") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(verifying = true, verifyError = null) }
            when (val result = personnelRepository.verifyCodeSecret(s.agentPreneurPersonnelId, s.codeSecret.trim())) {
                is Resource.Success -> {
                    if (result.data) {
                        // After successful verification, pull the signature
                        // directly from the personnel's existing data. If
                        // they have one, it becomes the default signature
                        // for this perception. The user can still choose to
                        // draw a new one to override.
                        val personnel = s.personnelOptions.firstOrNull { it.id == s.agentPreneurPersonnelId }
                        val existingSvg = personnel?.signatureSvg
                        _state.update {
                            it.copy(
                                verifying = false,
                                verified = true,
                                verifyError = null,
                                signatureSvg = existingSvg,
                                signatureFromPersonnel = existingSvg != null
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                verifying = false,
                                verified = false,
                                verifyError = "Code secret incorrect. L'identité de l'agent n'a pas pu être vérifiée."
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            verifying = false,
                            verified = false,
                            verifyError = result.message ?: "Erreur lors de la vérification"
                        )
                    }
                }
                is Resource.Loading -> {}
            }
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
        val validationError = ArmementFormValidator.validate(
            datePerception = s.datePerception,
            heurePerception = s.heure,
            agentSelected = s.agentPreneurPersonnelId > 0,
            typeArme = s.typeArme,
            matriculeArme = s.matriculeArme,
            munitions = s.munitions
        )
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }
        // On create, the agent must be verified via code secret before the
        // perception can be created. On edit, verification was done at
        // perception time and cannot be modified.
        if (!s.isEdit && !s.verified) {
            _state.update {
                it.copy(errorMessage = "L'identité de l'agent doit être vérifiée via le code secret avant d'enregistrer la perception")
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = ArmementFormData(
                datePerception = s.datePerception,
                heurePerception = s.heure,
                agentPreneurPersonnelId = s.agentPreneurPersonnelId,
                typeArme = s.typeArme,
                matriculeArme = s.matriculeArme,
                munitions = s.munitions.trim().takeIf { it.isNotEmpty() }?.toIntOrNull(),
                secteurMission = s.secteurMission,
                etatPerception = s.etatPerception,
                // On create, include the code secret (verified server-side)
                // and the signature SVG (captured after verification).
                codeSecret = if (s.isEdit) null else s.codeSecret.trim().takeIf { it.isNotEmpty() },
                signatureSvg = if (s.isEdit) null else s.signatureSvg
            )
            val result = if (s.isEdit) {
                armementRepository.updateArmement(armementId, data)
            } else {
                armementRepository.createArmement(data)
            }
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
            armementRepository.deleteAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                armementRepository.deleteAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    armementRepository.addAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                armementRepository.updateAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                armementRepository.addAttachment(savedId, a.title, a.uploadFile)
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
