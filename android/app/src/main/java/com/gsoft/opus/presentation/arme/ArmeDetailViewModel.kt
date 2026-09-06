package com.gsoft.opus.presentation.arme

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Arme
import com.gsoft.opus.domain.model.ArmeMunitionsConsommation
import com.gsoft.opus.domain.repository.ArmeRepository
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
    val notFound: Boolean = false
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

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null, errorMessage = null) }
    }
}
