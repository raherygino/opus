package com.gsoft.opus.presentation.rassemblement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.RassemblementJournalierFormData
import com.gsoft.opus.domain.repository.RassemblementJournalierRepository
import com.gsoft.opus.domain.repository.RepartitionSecteurInput
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

data class RassemblementFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val dateRassemblement: String = "",
    val heureRassemblement: String = "",
    val brigadeService: String = "",
    val officierPermanence: String = "",
    val inspecteurPermanence: String = "",
    val chefPoste: String = "",
    val instructionsAutorite: String = "",
    // Situation de prise d'arme — stored directly on the rassemblement record.
    val effectifTheorique: String = "0",
    val present: String = "0",
    val absent: String = "0",
    val motifAbsence: String = "",
    // Répartition par secteur — one row list; Diurne/Nocturne differ only by type.
    val repartitions: List<RepartitionRow> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class RassemblementFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RassemblementJournalierRepository
) : ViewModel() {

    private val rassemblementId: Int = savedStateHandle.get<Int>("rassemblementId") ?: 0

    private val _state = MutableStateFlow(
        RassemblementFormUiState(
            isEdit = rassemblementId > 0,
            dateRassemblement = if (rassemblementId > 0) "" else todayIso(),
            heureRassemblement = if (rassemblementId > 0) "" else nowHHmm()
        )
    )
    val state: StateFlow<RassemblementFormUiState> = _state.asStateFlow()

    /** Local keys for newly added rows (negative to avoid clashing with server ids). */
    private var nextKey = 0L
    private fun newKey(): Long = nextKey--

    init {
        if (rassemblementId > 0) {
            loadEntry()
        } else {
            // Start with one blank line per section.
            _state.update {
                it.copy(repartitions = REPARTITION_SECTIONS.map { (type) -> blankRow(type) })
            }
        }
    }

    private fun blankRow(type: String) = RepartitionRow(key = newKey(), type = type)

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getRassemblement(rassemblementId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            dateRassemblement = e.dateRassemblement.take(10),
                            heureRassemblement = e.heureDisplay,
                            brigadeService = e.brigadeService,
                            officierPermanence = e.officierPermanence.orEmpty(),
                            inspecteurPermanence = e.inspecteurPermanence.orEmpty(),
                            chefPoste = e.chefPoste.orEmpty(),
                            instructionsAutorite = e.instructionsAutorite.orEmpty(),
                            effectifTheorique = e.effectifTheorique.toString(),
                            present = e.present.toString(),
                            absent = e.absent.toString(),
                            motifAbsence = e.motifAbsence.orEmpty(),
                            repartitions = e.repartitions.map { it.toRow() }
                        )
                    }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateDateRassemblement(value: String) { _state.update { it.copy(dateRassemblement = value) } }
    fun updateHeureRassemblement(value: String) { _state.update { it.copy(heureRassemblement = value) } }
    fun updateBrigadeService(value: String) { _state.update { it.copy(brigadeService = value) } }
    fun updateOfficierPermanence(value: String) { _state.update { it.copy(officierPermanence = value) } }
    fun updateInspecteurPermanence(value: String) { _state.update { it.copy(inspecteurPermanence = value) } }
    fun updateChefPoste(value: String) { _state.update { it.copy(chefPoste = value) } }
    fun updateInstructionsAutorite(value: String) { _state.update { it.copy(instructionsAutorite = value) } }
    fun updateEffectifTheorique(value: String) { _state.update { it.copy(effectifTheorique = value) } }
    fun updatePresent(value: String) { _state.update { it.copy(present = value) } }
    fun updateAbsent(value: String) { _state.update { it.copy(absent = value) } }
    fun updateMotifAbsence(value: String) { _state.update { it.copy(motifAbsence = value) } }

    fun addRepartition(type: String) {
        _state.update { it.copy(repartitions = it.repartitions + blankRow(type)) }
    }

    fun updateRepartition(key: Long, update: (RepartitionRow) -> RepartitionRow) {
        _state.update { s ->
            s.copy(repartitions = s.repartitions.map { if (it.key == key) update(it) else it })
        }
    }

    fun removeRepartition(key: Long) {
        _state.update { s -> s.copy(repartitions = s.repartitions.filterNot { it.key == key }) }
    }

    fun save() {
        val s = _state.value
        val errors = validateRassemblementForm(
            dateRassemblement = s.dateRassemblement,
            heureRassemblement = s.heureRassemblement,
            brigadeService = s.brigadeService,
            effectifTheorique = s.effectifTheorique,
            present = s.present,
            absent = s.absent
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            // Skip rows the user left entirely blank.
            val repartitions = s.repartitions
                .filterNot { it.isBlank }
                .map {
                    RepartitionSecteurInput(
                        type = it.type,
                        secteur = it.secteur.trim(),
                        effectifEngage = it.effectifEngage.trim().ifBlank { null },
                        chefElementContact = it.chefElementContact.trim().ifBlank { null },
                        controleContact = it.controleContact.trim().ifBlank { null },
                        materielsArmements = it.materielsArmements.trim().ifBlank { null },
                        missions = it.missions.trim().ifBlank { null }
                    )
                }
            val data = RassemblementJournalierFormData(
                dateRassemblement = s.dateRassemblement,
                heureRassemblement = s.heureRassemblement,
                brigadeService = s.brigadeService.trim(),
                officierPermanence = s.officierPermanence.trim().ifBlank { null },
                inspecteurPermanence = s.inspecteurPermanence.trim().ifBlank { null },
                chefPoste = s.chefPoste.trim().ifBlank { null },
                instructionsAutorite = s.instructionsAutorite.trim().ifBlank { null },
                effectifTheorique = s.effectifTheorique.trim().toIntOrNull() ?: 0,
                present = s.present.trim().toIntOrNull() ?: 0,
                absent = s.absent.trim().toIntOrNull() ?: 0,
                motifAbsence = s.motifAbsence.trim().ifBlank { null },
                repartitions = repartitions
            )
            val result = if (s.isEdit) {
                repository.updateRassemblement(rassemblementId, data)
            } else {
                repository.createRassemblement(data)
            }
            when (result) {
                is Resource.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun nowHHmm(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
