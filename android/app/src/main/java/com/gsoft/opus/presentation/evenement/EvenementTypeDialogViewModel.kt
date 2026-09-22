package com.gsoft.opus.presentation.evenement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.EvenementSurvenuType
import com.gsoft.opus.domain.repository.EvenementSurvenuTypeFormData
import com.gsoft.opus.domain.repository.EvenementSurvenuTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EvenementTypeDialogUiState(
    val types: List<EvenementSurvenuType> = emptyList(),
    val isLoading: Boolean = false,
    val newLabel: String = "",
    val isCreating: Boolean = false,
    val editingId: Int? = null,
    val editingLabel: String = "",
    val isSavingEdit: Boolean = false,
    val deleteTarget: EvenementSurvenuType? = null,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class EvenementTypeDialogViewModel @Inject constructor(
    private val typeRepository: EvenementSurvenuTypeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EvenementTypeDialogUiState())
    val state: StateFlow<EvenementTypeDialogUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = typeRepository.getTypes()) {
                is Resource.Success -> _state.update {
                    it.copy(types = result.data, isLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setNewLabel(value: String) {
        _state.update { it.copy(newLabel = value) }
    }

    fun createType() {
        val label = _state.value.newLabel.trim()
        if (label.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, errorMessage = null) }
            when (val result = typeRepository.createType(EvenementSurvenuTypeFormData(label))) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isCreating = false,
                        newLabel = "",
                        types = (it.types + result.data).sortedBy { t -> t.label }
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isCreating = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun startEdit(type: EvenementSurvenuType) {
        _state.update { it.copy(editingId = type.id, editingLabel = type.label) }
    }

    fun cancelEdit() {
        _state.update { it.copy(editingId = null, editingLabel = "") }
    }

    fun setEditingLabel(value: String) {
        _state.update { it.copy(editingLabel = value) }
    }

    fun saveEdit() {
        val id = _state.value.editingId ?: return
        val label = _state.value.editingLabel.trim()
        if (label.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSavingEdit = true, errorMessage = null) }
            when (val result = typeRepository.updateType(id, EvenementSurvenuTypeFormData(label))) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isSavingEdit = false,
                        editingId = null,
                        editingLabel = "",
                        types = it.types.map { t -> if (t.id == id) result.data else t }
                            .sortedBy { t -> t.label }
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isSavingEdit = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun requestDelete(type: EvenementSurvenuType) {
        _state.update { it.copy(deleteTarget = type) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, errorMessage = null) }
            when (val result = typeRepository.deleteType(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        types = it.types.filterNot { t -> t.id == target.id }
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isDeleting = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
