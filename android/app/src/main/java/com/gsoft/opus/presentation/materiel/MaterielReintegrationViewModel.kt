package com.gsoft.opus.presentation.materiel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.AffectationMateriel
import com.gsoft.opus.domain.repository.MaterielRepository
import com.gsoft.opus.domain.repository.ReintegrationMaterielData
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

data class MaterielReintegrationUiState(
    val affectation: AffectationMateriel? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val dateReintegration: String = "",
    val heureReintegration: String = "",
    /** Map of ligneId → etat_reintegration. */
    val ligneEtats: Map<Int, String> = emptyMap(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class MaterielReintegrationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val materielRepository: MaterielRepository
) : ViewModel() {

    private val affectationId: Int = savedStateHandle.get<Int>("affectationId") ?: 0

    private val _state = MutableStateFlow(MaterielReintegrationUiState())
    val state: StateFlow<MaterielReintegrationUiState> = _state.asStateFlow()

    init {
        loadAffectation()
    }

    private fun loadAffectation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = materielRepository.getAffectationMateriel(affectationId)) {
                is Resource.Success -> {
                    val now = Date()
                    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
                    _state.update {
                        it.copy(
                            affectation = result.data,
                            isLoading = false,
                            dateReintegration = dateFmt.format(now),
                            heureReintegration = timeFmt.format(now),
                            ligneEtats = result.data.lignes.associate { l -> l.id to "" }
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

    fun setDateReintegration(date: String) = _state.update { it.copy(dateReintegration = date) }
    fun setHeureReintegration(heure: String) = _state.update { it.copy(heureReintegration = heure) }

    fun setLigneEtat(ligneId: Int, etat: String) {
        _state.update { it.copy(ligneEtats = it.ligneEtats + (ligneId to etat)) }
    }

    fun save() {
        val s = _state.value
        val aff = s.affectation ?: return

        if (s.dateReintegration.isBlank()) {
            _state.update { it.copy(errorMessage = "La date de la réintégration est requise") }
            return
        }
        if (s.heureReintegration.isBlank()) {
            _state.update { it.copy(errorMessage = "L'heure de la réintégration est requise") }
            return
        }
        // Business rule 5: reintegration date must not be earlier than perception date
        if (s.dateReintegration < aff.datePerception) {
            _state.update { it.copy(errorMessage = "La date de réintégration ne peut pas être antérieure à la date de perception") }
            return
        }
        // Business rule 6: etat_reintegration required per line item
        for (l in aff.lignes) {
            if ((s.ligneEtats[l.id] ?: "").isBlank()) {
                _state.update { it.copy(errorMessage = "L'état à la réintégration est requis pour chaque matériel") }
                return
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val result = materielRepository.reintegrateAffectationMateriel(
                affectationId,
                ReintegrationMaterielData(
                    dateReintegration = s.dateReintegration,
                    heureReintegration = s.heureReintegration,
                    ligneEtats = s.ligneEtats.mapValues { it.value.trim() }
                )
            )
            when (result) {
                is Resource.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }
}
