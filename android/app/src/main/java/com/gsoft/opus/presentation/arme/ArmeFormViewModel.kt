package com.gsoft.opus.presentation.arme

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Arme
import com.gsoft.opus.domain.model.TypeArme
import com.gsoft.opus.domain.repository.ArmeFormData
import com.gsoft.opus.domain.repository.ArmeRepository
import com.gsoft.opus.domain.repository.TypeArmeFormData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArmeFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val typeArmeId: Int = 0,
    val typesArmes: List<TypeArme> = emptyList(),
    val matricule: String = "",
    val errorMessage: String? = null,
    val saved: Boolean = false,
    // TypeArme quick-create dialog
    val showTypeArmeDialog: Boolean = false,
    val newTypeArmeNom: String = "",
    val newTypeArmeDescription: String = "",
    val isSavingTypeArme: Boolean = false
)

@HiltViewModel
class ArmeFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val armeRepository: ArmeRepository
) : ViewModel() {

    private val armeId: Int = savedStateHandle.get<Int>("armeId") ?: 0

    private val _state = MutableStateFlow(ArmeFormUiState(isEdit = armeId > 0))
    val state: StateFlow<ArmeFormUiState> = _state.asStateFlow()

    init {
        loadTypes()
        if (armeId > 0) loadArme()
    }

    private fun loadTypes() {
        viewModelScope.launch {
            when (val result = armeRepository.getTypeArmeList()) {
                is Resource.Success -> _state.update { it.copy(typesArmes = result.data) }
                else -> {}
            }
        }
    }

    private fun loadArme() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = armeRepository.getArme(armeId)) {
                is Resource.Success -> {
                    val a = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            typeArmeId = a.typeArmeId,
                            matricule = a.matricule
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

    fun updateTypeArmeId(id: Int) { _state.update { it.copy(typeArmeId = id) } }
    fun updateMatricule(v: String) { _state.update { it.copy(matricule = v) } }

    fun showTypeArmeDialog() { _state.update { it.copy(showTypeArmeDialog = true) } }
    fun hideTypeArmeDialog() {
        _state.update { it.copy(showTypeArmeDialog = false, newTypeArmeNom = "", newTypeArmeDescription = "") }
    }
    fun updateNewTypeArmeNom(v: String) { _state.update { it.copy(newTypeArmeNom = v) } }
    fun updateNewTypeArmeDescription(v: String) { _state.update { it.copy(newTypeArmeDescription = v) } }

    fun createTypeArme() {
        val s = _state.value
        val error = ArmeFormValidator.validateTypeArme(s.newTypeArmeNom)
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSavingTypeArme = true, errorMessage = null) }
            when (val result = armeRepository.createTypeArme(
                TypeArmeFormData(nom = s.newTypeArmeNom.trim(), description = s.newTypeArmeDescription.trim().ifBlank { null })
            )) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isSavingTypeArme = false,
                            showTypeArmeDialog = false,
                            typesArmes = it.typesArmes + result.data,
                            typeArmeId = result.data.id,
                            newTypeArmeNom = "",
                            newTypeArmeDescription = ""
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isSavingTypeArme = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun save() {
        val s = _state.value
        val error = ArmeFormValidator.validateArme(s.typeArmeId, s.matricule)
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = ArmeFormData(
                typeArmeId = s.typeArmeId,
                matricule = s.matricule.trim(),
                munitionsStock = 0
            )
            val result = if (s.isEdit) armeRepository.updateArme(armeId, data) else armeRepository.createArme(data)
            when (result) {
                is Resource.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissError() { _state.update { it.copy(errorMessage = null) } }
}
