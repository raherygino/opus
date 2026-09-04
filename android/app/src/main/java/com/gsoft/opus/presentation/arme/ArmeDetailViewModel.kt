package com.gsoft.opus.presentation.arme

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Arme
import com.gsoft.opus.domain.model.ArmeMunitionsConsommation
import com.gsoft.opus.domain.repository.ArmeRepository
import com.gsoft.opus.domain.repository.ConsommationData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArmeDetailUiState(
    val arme: Arme? = null,
    val consommations: List<ArmeMunitionsConsommation> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val notFound: Boolean = false,
    // Consommation dialog
    val showConsommationDialog: Boolean = false,
    val consoQuantite: String = "1",
    val consoAgentId: String = "",
    val isSavingConso: Boolean = false
)

@HiltViewModel
class ArmeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val armeRepository: ArmeRepository
) : ViewModel() {

    private val armeId: Int = savedStateHandle.get<Int>("armeId") ?: 0

    private val _state = MutableStateFlow(ArmeDetailUiState())
    val state: StateFlow<ArmeDetailUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = armeRepository.getArme(armeId)) {
                is Resource.Success -> {
                    _state.update { it.copy(arme = result.data) }
                    when (val consoResult = armeRepository.getConsommations(armeId)) {
                        is Resource.Success -> _state.update {
                            it.copy(isLoading = false, consommations = consoResult.data)
                        }
                        is Resource.Error -> _state.update {
                            it.copy(isLoading = false, consommations = emptyList())
                        }
                        is Resource.Loading -> {}
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message, notFound = true)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun showConsommationDialog() {
        _state.update { it.copy(showConsommationDialog = true, consoQuantite = "1", consoAgentId = "") }
    }

    fun hideConsommationDialog() {
        _state.update { it.copy(showConsommationDialog = false) }
    }

    fun updateConsoQuantite(v: String) {
        _state.update { it.copy(consoQuantite = v.filter(Char::isDigit)) }
    }

    fun updateConsoAgentId(v: String) {
        _state.update { it.copy(consoAgentId = v.filter(Char::isDigit)) }
    }

    fun submitConsommation() {
        val s = _state.value
        val arme = s.arme ?: return
        val error = ArmeFormValidator.validateConsommation(s.consoQuantite, arme.typeArmeMunitionsStock)
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSavingConso = true, errorMessage = null) }
            val data = ConsommationData(
                agentId = s.consoAgentId.trim().ifBlank { null }?.toIntOrNull(),
                quantite = s.consoQuantite.trim().toInt()
            )
            when (val result = armeRepository.recordConsommation(armeId, data)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isSavingConso = false,
                        showConsommationDialog = false,
                        arme = result.data,
                        userMessage = "Consommation enregistrée avec succès"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isSavingConso = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
            // Reload consommations list
            when (val consoResult = armeRepository.getConsommations(armeId)) {
                is Resource.Success -> _state.update { it.copy(consommations = consoResult.data) }
                else -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null, errorMessage = null) }
    }
}
