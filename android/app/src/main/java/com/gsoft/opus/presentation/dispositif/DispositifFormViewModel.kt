package com.gsoft.opus.presentation.dispositif

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.DispositifEffectifInput
import com.gsoft.opus.domain.repository.DispositifExceptionnelFormData
import com.gsoft.opus.domain.repository.DispositifExceptionnelRepository
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

data class DispositifFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val natureEvenement: String = "",
    val dateDebut: String = "",
    val dateFin: String = "",
    // Effectif engagé — flat row list (secteur, contacts, matériels, missions).
    val effectifs: List<EffectifRow> = emptyList(),
    /** Row keys whose secteur failed validation (partially filled rows). */
    val invalidSecteurKeys: Set<Long> = emptySet(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class DispositifFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DispositifExceptionnelRepository
) : ViewModel() {

    private val dispositifId: Int = savedStateHandle.get<Int>("dispositifId") ?: 0

    private val _state = MutableStateFlow(
        DispositifFormUiState(
            isEdit = dispositifId > 0,
            dateDebut = if (dispositifId > 0) "" else todayIso(),
            dateFin = if (dispositifId > 0) "" else todayIso()
        )
    )
    val state: StateFlow<DispositifFormUiState> = _state.asStateFlow()

    /** Local keys for newly added rows (negative to avoid clashing with server ids). */
    private var nextKey = 0L
    private fun newKey(): Long = nextKey--

    init {
        if (dispositifId > 0) {
            loadEntry()
        } else {
            // Start with one blank effectif line.
            _state.update { it.copy(effectifs = listOf(blankRow())) }
        }
    }

    private fun blankRow() = EffectifRow(key = newKey())

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getDispositif(dispositifId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            natureEvenement = e.natureEvenement,
                            dateDebut = e.dateDebut.take(10),
                            dateFin = e.dateFin.take(10),
                            effectifs = e.effectifs.map { r -> r.toRow() }
                        )
                    }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateNatureEvenement(value: String) { _state.update { it.copy(natureEvenement = value) } }
    fun updatePeriode(start: String, end: String) {
        _state.update { it.copy(dateDebut = start, dateFin = end) }
    }

    fun addEffectif() {
        _state.update { it.copy(effectifs = it.effectifs + blankRow()) }
    }

    fun updateEffectif(key: Long, update: (EffectifRow) -> EffectifRow) {
        _state.update { s ->
            s.copy(
                effectifs = s.effectifs.map { if (it.key == key) update(it) else it },
                invalidSecteurKeys = s.invalidSecteurKeys - key
            )
        }
    }

    fun removeEffectif(key: Long) {
        _state.update { s ->
            s.copy(
                effectifs = s.effectifs.filterNot { it.key == key },
                invalidSecteurKeys = s.invalidSecteurKeys - key
            )
        }
    }

    fun save() {
        val s = _state.value
        val errors = validateDispositifForm(
            natureEvenement = s.natureEvenement,
            dateDebut = s.dateDebut,
            dateFin = s.dateFin
        )
        // Table entries: a partially filled row must have a secteur.
        val invalidKeys = s.effectifs
            .filter { !it.isBlank && it.secteur.isBlank() }
            .map { it.key }
            .toSet()
        if (errors.isNotEmpty() || invalidKeys.isNotEmpty()) {
            _state.update {
                it.copy(
                    errorMessage = errors.values.firstOrNull()
                        ?: "Chaque ligne d'effectif renseignée doit avoir un secteur",
                    invalidSecteurKeys = invalidKeys
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            // Skip rows the user left entirely blank.
            val effectifs = s.effectifs
                .filterNot { it.isBlank }
                .map {
                    DispositifEffectifInput(
                        secteur = it.secteur.trim(),
                        chefElementContact = it.chefElementContact.trim().ifBlank { null },
                        controleContact = it.controleContact.trim().ifBlank { null },
                        materielsArmements = it.materielsArmements.trim().ifBlank { null },
                        missions = it.missions.trim().ifBlank { null }
                    )
                }
            val data = DispositifExceptionnelFormData(
                natureEvenement = s.natureEvenement.trim(),
                dateDebut = s.dateDebut,
                dateFin = s.dateFin,
                effectifs = effectifs
            )
            val result = if (s.isEdit) {
                repository.updateDispositif(dispositifId, data)
            } else {
                repository.createDispositif(data)
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
}
