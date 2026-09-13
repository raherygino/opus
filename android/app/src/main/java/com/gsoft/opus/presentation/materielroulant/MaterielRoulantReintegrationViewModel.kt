package com.gsoft.opus.presentation.materielroulant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.MaterielRoulant
import com.gsoft.opus.domain.repository.MaterielRoulantRepository
import com.gsoft.opus.domain.repository.ReintegrationMaterielRoulantData
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

data class MaterielRoulantReintegrationUiState(
    val item: MaterielRoulant? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val dateReintegration: String = "",
    val heureReintegration: String = "",
    val kilometrageRetour: String = "",
    val niveauCarburantRetour: String = "",
    val observationsTechniques: String = "",
    val defaillances: String = "",
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class MaterielRoulantReintegrationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val materielRoulantRepository: MaterielRoulantRepository
) : ViewModel() {

    private val materielId: Int = savedStateHandle.get<Int>("affectationId") ?: 0

    private val _state = MutableStateFlow(MaterielRoulantReintegrationUiState())
    val state: StateFlow<MaterielRoulantReintegrationUiState> = _state.asStateFlow()

    init {
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = materielRoulantRepository.getMaterielRoulant(materielId)) {
                is Resource.Success -> {
                    val m = result.data
                    val now = Date()
                    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                    _state.update {
                        it.copy(
                            item = m,
                            isLoading = false,
                            dateReintegration = dateFmt.format(now),
                            heureReintegration = timeFmt.format(now),
                            kilometrageRetour = m.kilometrageDepart ?: ""
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

    fun setDateReintegration(v: String) { _state.update { it.copy(dateReintegration = v) } }
    fun setHeureReintegration(v: String) { _state.update { it.copy(heureReintegration = v) } }
    fun setKilometrageRetour(v: String) { _state.update { it.copy(kilometrageRetour = v) } }
    fun setNiveauCarburantRetour(v: String) { _state.update { it.copy(niveauCarburantRetour = v) } }
    fun setObservationsTechniques(v: String) { _state.update { it.copy(observationsTechniques = v) } }
    fun setDefaillances(v: String) { _state.update { it.copy(defaillances = v) } }

    fun save() {
        val s = _state.value
        val item = s.item ?: return

        if (s.dateReintegration.isBlank()) {
            _state.update { it.copy(errorMessage = "La date de la réintégration est requise") }
            return
        }
        if (s.heureReintegration.isBlank()) {
            _state.update { it.copy(errorMessage = "L'heure de la réintégration est requise") }
            return
        }
        if (s.dateReintegration < item.datePerception) {
            _state.update { it.copy(errorMessage = "La date de réintégration ne peut pas être antérieure à la date de perception") }
            return
        }
        if (s.kilometrageRetour.isBlank()) {
            _state.update { it.copy(errorMessage = "Le kilométrage de retour est requis") }
            return
        }
        if (!item.kilometrageDepart.isNullOrBlank() &&
            s.kilometrageRetour.toDoubleOrNull() != null &&
            item.kilometrageDepart.toDoubleOrNull() != null &&
            s.kilometrageRetour.toDoubleOrNull()!! < item.kilometrageDepart.toDoubleOrNull()!!
        ) {
            _state.update { it.copy(errorMessage = "Le kilométrage de retour ne peut pas être inférieur au kilométrage de départ") }
            return
        }
        val obs = s.observationsTechniques.trim()
        val def = s.defaillances.trim()
        if (obs.isNotEmpty() && obs == def) {
            _state.update { it.copy(errorMessage = "Les observations techniques et les défaillances doivent être distinctes") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = ReintegrationMaterielRoulantData(
                dateReintegration = s.dateReintegration,
                heureReintegration = s.heureReintegration,
                kilometrageRetour = s.kilometrageRetour.trim(),
                niveauCarburantRetour = s.niveauCarburantRetour.trim().ifBlank { null },
                observationsTechniques = obs.ifBlank { null },
                defaillances = def.ifBlank { null }
            )
            when (val result = materielRoulantRepository.reintegrateMaterielRoulant(materielId, data)) {
                is Resource.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }
}
