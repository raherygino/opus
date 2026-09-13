package com.gsoft.opus.presentation.maincourante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.MainCouranteCategorie
import com.gsoft.opus.domain.repository.MainCouranteCategorieFormData
import com.gsoft.opus.domain.repository.MainCouranteCategorieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainCouranteCategorieDialogUiState(
    val categories: List<MainCouranteCategorie> = emptyList(),
    val isLoading: Boolean = false,
    val newLabel: String = "",
    val isCreating: Boolean = false,
    val editingId: Int? = null,
    val editingLabel: String = "",
    val isSavingEdit: Boolean = false,
    val deleteTarget: MainCouranteCategorie? = null,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MainCouranteCategorieDialogViewModel @Inject constructor(
    private val categorieRepository: MainCouranteCategorieRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainCouranteCategorieDialogUiState())
    val state: StateFlow<MainCouranteCategorieDialogUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = categorieRepository.getCategories()) {
                is Resource.Success -> _state.update {
                    it.copy(categories = result.data, isLoading = false)
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

    fun createCategory() {
        val label = _state.value.newLabel.trim()
        if (label.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, errorMessage = null) }
            when (val result = categorieRepository.createCategory(MainCouranteCategorieFormData(label))) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isCreating = false,
                        newLabel = "",
                        categories = (it.categories + result.data).sortedBy { c -> c.label }
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isCreating = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun startEdit(category: MainCouranteCategorie) {
        _state.update { it.copy(editingId = category.id, editingLabel = category.label) }
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
            when (val result = categorieRepository.updateCategory(id, MainCouranteCategorieFormData(label))) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isSavingEdit = false,
                        editingId = null,
                        editingLabel = "",
                        categories = it.categories.map { c -> if (c.id == id) result.data else c }
                            .sortedBy { c -> c.label }
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isSavingEdit = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun requestDelete(category: MainCouranteCategorie) {
        _state.update { it.copy(deleteTarget = category) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, errorMessage = null) }
            when (val result = categorieRepository.deleteCategory(target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isDeleting = false,
                        deleteTarget = null,
                        categories = it.categories.filterNot { c -> c.id == target.id }
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
