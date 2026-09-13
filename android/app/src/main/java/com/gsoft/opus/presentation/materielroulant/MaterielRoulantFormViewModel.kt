package com.gsoft.opus.presentation.materielroulant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.repository.MaterielRoulantFormData
import com.gsoft.opus.domain.repository.MaterielRoulantRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
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

data class MaterielRoulantFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val canEdit: Boolean = false,
    val personnelOptions: List<Personnel> = emptyList(),
    val datePerception: String = "",
    val heurePerception: String = "",
    val typeMateriel: String = "VHL",
    val numeroImmatriculation: String = "",
    val descriptionVehicule: String = "",
    val agentConducteurPersonnelId: Int = 0,
    val chefDeBordPersonnelId: Int = 0,
    val kilometrageDepart: String = "",
    val niveauCarburantDepart: String = "",
    // Conducteur verification (create only — set at perception time, one-way).
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
class MaterielRoulantFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val materielRoulantRepository: MaterielRoulantRepository,
    private val personnelRepository: PersonnelRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val materielId: Int = savedStateHandle.get<Int>("affectationId") ?: 0

    private val _state = MutableStateFlow(MaterielRoulantFormUiState())
    val state: StateFlow<MaterielRoulantFormUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        loadOptions()
        if (materielId > 0) loadItem() else setDefaults()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(canEdit = hasPermission(user, MATERIEL_ROULANT_MODULE, PermissionAction.EDIT))
            }
        }
    }

    private fun loadOptions() {
        viewModelScope.launch {
            when (val result = personnelRepository.getPersonnelList()) {
                is Resource.Success -> _state.update { it.copy(personnelOptions = result.data) }
                is Resource.Error -> _state.update { it.copy(errorMessage = "Impossible de charger le personnel") }
                is Resource.Loading -> {}
            }
        }
    }

    private fun setDefaults() {
        val now = Date()
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        _state.update {
            it.copy(
                isEdit = false,
                datePerception = dateFmt.format(now),
                heurePerception = timeFmt.format(now)
            )
        }
    }

    private fun loadItem() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = materielRoulantRepository.getMaterielRoulant(materielId)) {
                is Resource.Success -> {
                    val m = result.data
                    _state.update {
                        it.copy(
                            isEdit = true,
                            isLoading = false,
                            datePerception = m.datePerception,
                            heurePerception = m.heurePerception.take(5),
                            typeMateriel = m.typeMateriel,
                            numeroImmatriculation = m.numeroImmatriculation ?: "",
                            descriptionVehicule = m.descriptionVehicule ?: "",
                            agentConducteurPersonnelId = m.agentConducteurPersonnelId ?: 0,
                            chefDeBordPersonnelId = m.chefDeBordPersonnelId ?: 0,
                            kilometrageDepart = m.kilometrageDepart ?: "",
                            niveauCarburantDepart = m.niveauCarburantDepart ?: "",
                            verified = m.agentVerifie,
                            signatureSvg = m.signatureSvg
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

    fun setDatePerception(v: String) { _state.update { it.copy(datePerception = v) } }
    fun setHeurePerception(v: String) { _state.update { it.copy(heurePerception = v) } }
    fun setTypeMateriel(v: String) { _state.update { it.copy(typeMateriel = v) } }
    fun setNumeroImmatriculation(v: String) { _state.update { it.copy(numeroImmatriculation = v) } }
    fun setDescriptionVehicule(v: String) { _state.update { it.copy(descriptionVehicule = v) } }
    fun setAgentConducteur(v: Int) {
        // Reset verification state when the conducteur changes.
        _state.update {
            it.copy(
                agentConducteurPersonnelId = v,
                verified = false,
                verifyError = null,
                codeSecret = "",
                signatureSvg = null,
                signatureFromPersonnel = false
            )
        }
    }
    fun setChefDeBord(v: Int) { _state.update { it.copy(chefDeBordPersonnelId = v) } }
    fun setKilometrageDepart(v: String) { _state.update { it.copy(kilometrageDepart = v) } }
    fun setNiveauCarburantDepart(v: String) { _state.update { it.copy(niveauCarburantDepart = v) } }
    fun setCodeSecret(v: String) { _state.update { it.copy(codeSecret = v) } }
    fun setSignatureSvg(svg: String?) {
        _state.update { it.copy(signatureSvg = svg, signatureFromPersonnel = false) }
    }

    /**
     * Verify the conducteur's code secret against the server. On success,
     * pulls the signature from the personnel's existing data (if any).
     */
    fun verifyCode() {
        val s = _state.value
        if (s.agentConducteurPersonnelId <= 0) {
            _state.update { it.copy(verifyError = "Sélectionnez d'abord un agent conducteur") }
            return
        }
        if (s.codeSecret.isBlank()) {
            _state.update { it.copy(verifyError = "Saisissez le code secret du conducteur") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(verifying = true, verifyError = null) }
            when (val result = personnelRepository.verifyCodeSecret(s.agentConducteurPersonnelId, s.codeSecret.trim())) {
                is Resource.Success -> {
                    if (result.data) {
                        val personnel = s.personnelOptions.firstOrNull { it.id == s.agentConducteurPersonnelId }
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
                                verifyError = "Code secret incorrect. L'identité du conducteur n'a pas pu être vérifiée."
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

    fun save() {
        val s = _state.value
        if (s.agentConducteurPersonnelId <= 0) {
            _state.update { it.copy(errorMessage = "L'agent conducteur est requis") }
            return
        }
        if (s.datePerception.isBlank() || s.heurePerception.isBlank()) {
            _state.update { it.copy(errorMessage = "La date et l'heure de perception sont requises") }
            return
        }
        // On create, the conducteur must be verified via code secret before the
        // perception can be created. On edit, verification was done at
        // perception time and cannot be modified.
        if (!s.isEdit && !s.verified) {
            _state.update {
                it.copy(errorMessage = "L'identité du conducteur doit être vérifiée via le code secret avant d'enregistrer la perception")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = MaterielRoulantFormData(
                datePerception = s.datePerception,
                heurePerception = s.heurePerception,
                typeMateriel = s.typeMateriel,
                numeroImmatriculation = s.numeroImmatriculation.trim().ifBlank { null },
                descriptionVehicule = s.descriptionVehicule.trim().ifBlank { null },
                agentConducteurPersonnelId = s.agentConducteurPersonnelId,
                chefDeBordPersonnelId = s.chefDeBordPersonnelId.takeIf { it > 0 },
                kilometrageDepart = s.kilometrageDepart.trim().ifBlank { null },
                niveauCarburantDepart = s.niveauCarburantDepart.trim().ifBlank { null },
                // On create, include the code secret (verified server-side)
                // and the optional signature SVG.
                codeSecret = if (s.isEdit) null else s.codeSecret.trim().takeIf { it.isNotEmpty() },
                signatureSvg = if (s.isEdit) null else s.signatureSvg
            )
            val result = if (s.isEdit) {
                materielRoulantRepository.updateMaterielRoulant(materielId, data)
            } else {
                materielRoulantRepository.createMaterielRoulant(data)
            }
            when (result) {
                is Resource.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }
}
