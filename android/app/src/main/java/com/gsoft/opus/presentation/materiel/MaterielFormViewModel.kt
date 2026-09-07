package com.gsoft.opus.presentation.materiel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.TypeMateriel
import com.gsoft.opus.domain.repository.AffectationMaterielFormData
import com.gsoft.opus.domain.repository.AffectationMaterielLigneFormData
import com.gsoft.opus.domain.repository.MaterielRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
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

data class LigneFormState(
    val typeMaterielId: Int = 0,
    val numeroMateriel: String = "",
    val etatEmport: String = ""
)

data class MaterielFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val agentPersonnelId: Int = 0,
    val personnelOptions: List<Personnel> = emptyList(),
    val typeOptions: List<TypeMateriel> = emptyList(),
    val datePerception: String = "",
    val heurePerception: String = "",
    val observations: String = "",
    val lignes: List<LigneFormState> = listOf(LigneFormState()),
    // Agent verification (create only — set at perception time, one-way).
    val codeSecret: String = "",
    val verifying: Boolean = false,
    val verified: Boolean = false,
    val verifyError: String? = null,
    // Signature SVG (captured after verification, optional).
    val signatureSvg: String? = null,
    // Whether the current signature came from the personnel's data (vs drawn).
    val signatureFromPersonnel: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class MaterielFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val materielRepository: MaterielRepository,
    private val personnelRepository: PersonnelRepository
) : ViewModel() {

    private val affectationId: Int = savedStateHandle.get<Int>("affectationId") ?: 0
    val editId: Int get() = affectationId

    private val _state = MutableStateFlow(MaterielFormUiState())
    val state: StateFlow<MaterielFormUiState> = _state.asStateFlow()

    init {
        loadOptions()
        if (affectationId > 0) loadAffectation()
        else initDefaults()
    }

    private fun initDefaults() {
        val now = Date()
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
        _state.update {
            it.copy(
                isEdit = false,
                datePerception = dateFmt.format(now),
                heurePerception = timeFmt.format(now)
            )
        }
    }

    private fun loadOptions() {
        viewModelScope.launch {
            val personnel = personnelRepository.getPersonnelList().getOrNull() ?: emptyList()
            val types = materielRepository.getTypeMaterielList().getOrNull() ?: emptyList()
            _state.update {
                it.copy(personnelOptions = personnel, typeOptions = types)
            }
        }
    }

    private fun loadAffectation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = materielRepository.getAffectationMateriel(affectationId)) {
                is Resource.Success -> {
                    val aff = result.data
                    _state.update {
                        it.copy(
                            isEdit = true,
                            isLoading = false,
                            agentPersonnelId = aff.agentPersonnelId,
                            datePerception = aff.datePerception,
                            heurePerception = aff.heurePerception.take(5),
                            observations = aff.observations ?: "",
                            verified = aff.agentVerifie,
                            signatureSvg = aff.signatureSvg,
                            lignes = aff.lignes.map { l ->
                                LigneFormState(
                                    typeMaterielId = l.typeMaterielId,
                                    numeroMateriel = l.numeroMateriel,
                                    etatEmport = l.etatEmport ?: ""
                                )
                            }
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setAgentPersonnelId(id: Int) {
        // Reset verification state when the agent changes.
        _state.update {
            it.copy(
                agentPersonnelId = id,
                verified = false,
                verifyError = null,
                codeSecret = "",
                signatureSvg = null,
                signatureFromPersonnel = false
            )
        }
    }
    fun setDatePerception(date: String) = _state.update { it.copy(datePerception = date) }
    fun setHeurePerception(heure: String) = _state.update { it.copy(heurePerception = heure) }
    fun setObservations(obs: String) = _state.update { it.copy(observations = obs) }
    fun setCodeSecret(value: String) = _state.update { it.copy(codeSecret = value) }
    fun setSignatureSvg(svg: String?) {
        _state.update { it.copy(signatureSvg = svg, signatureFromPersonnel = false) }
    }

    /**
     * Verify the agent's code secret against the server. On success, pulls
     * the signature from the personnel's existing data (if any).
     */
    fun verifyCode() {
        val s = _state.value
        if (s.agentPersonnelId <= 0) {
            _state.update { it.copy(verifyError = "Sélectionnez d'abord un agent") }
            return
        }
        if (s.codeSecret.isBlank()) {
            _state.update { it.copy(verifyError = "Saisissez le code secret de l'agent") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(verifying = true, verifyError = null) }
            when (val result = personnelRepository.verifyCodeSecret(s.agentPersonnelId, s.codeSecret.trim())) {
                is Resource.Success -> {
                    if (result.data) {
                        val personnel = s.personnelOptions.firstOrNull { it.id == s.agentPersonnelId }
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
                is Resource.Error -> _state.update {
                    it.copy(
                        verifying = false,
                        verified = false,
                        verifyError = result.message ?: "Erreur lors de la vérification"
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateLigne(index: Int, ligne: LigneFormState) {
        _state.update { state ->
            state.copy(lignes = state.lignes.mapIndexed { i, l -> if (i == index) ligne else l })
        }
    }

    fun addLigne() {
        _state.update { it.copy(lignes = it.lignes + LigneFormState()) }
    }

    fun removeLigne(index: Int) {
        _state.update {
            it.copy(lignes = it.lignes.filterIndexed { i, _ -> i != index })
        }
    }

    fun save() {
        val s = _state.value
        if (s.agentPersonnelId <= 0) {
            _state.update { it.copy(errorMessage = "L'agent est requis") }
            return
        }
        if (s.datePerception.isBlank() || s.heurePerception.isBlank()) {
            _state.update { it.copy(errorMessage = "La date et l'heure de perception sont requises") }
            return
        }
        if (s.lignes.isEmpty()) {
            _state.update { it.copy(errorMessage = "Au moins un type de matériel est requis") }
            return
        }
        for ((i, l) in s.lignes.withIndex()) {
            if (l.typeMaterielId <= 0) {
                _state.update { it.copy(errorMessage = "Ligne ${i + 1} : le type de matériel est requis") }
                return
            }
            if (l.numeroMateriel.isBlank()) {
                _state.update { it.copy(errorMessage = "Ligne ${i + 1} : l'ID Matériel est requis") }
                return
            }
        }
        // Check for duplicate numero_materiel within the same assignment
        val nums = s.lignes.map { it.numeroMateriel.trim() }
        val dupIndex = nums.indexOfFirst { n -> nums.indexOf(n) != nums.lastIndexOf(n) }
        if (dupIndex >= 0) {
            _state.update { it.copy(errorMessage = "Ligne ${dupIndex + 1} : l'ID Matériel « ${nums[dupIndex]} » est en double") }
            return
        }
        // On create, the agent must be verified via code secret before the
        // affectation can be created. On edit, verification was done at
        // perception time and cannot be modified.
        if (!s.isEdit && !s.verified) {
            _state.update {
                it.copy(errorMessage = "L'identité de l'agent doit être vérifiée via le code secret avant d'enregistrer l'affectation")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val formData = AffectationMaterielFormData(
                agentPersonnelId = s.agentPersonnelId,
                datePerception = s.datePerception,
                heurePerception = s.heurePerception,
                observations = s.observations.trim().ifBlank { null },
                lignes = s.lignes.map {
                    AffectationMaterielLigneFormData(
                        typeMaterielId = it.typeMaterielId,
                        numeroMateriel = it.numeroMateriel.trim(),
                        etatEmport = it.etatEmport.trim().ifBlank { null }
                    )
                },
                // On create, include the code secret (verified server-side)
                // and the optional signature SVG.
                codeSecret = if (s.isEdit) null else s.codeSecret.trim().takeIf { it.isNotEmpty() },
                signatureSvg = if (s.isEdit) null else s.signatureSvg
            )
            val result = if (s.isEdit) {
                materielRepository.updateAffectationMateriel(affectationId, formData)
            } else {
                materielRepository.createAffectationMateriel(formData)
            }
            when (result) {
                is Resource.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }
}
