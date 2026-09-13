package com.gsoft.opus.presentation.materielroulant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.MaterielRoulant
import com.gsoft.opus.domain.repository.MaterielRoulantRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaterielRoulantDetailUiState(
    val item: MaterielRoulant? = null,
    val isLoading: Boolean = true,
    val canEdit: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MaterielRoulantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val materielRoulantRepository: MaterielRoulantRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val materielId: Int = savedStateHandle.get<Int>("affectationId") ?: 0

    private val _state = MutableStateFlow(MaterielRoulantDetailUiState())
    val state: StateFlow<MaterielRoulantDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        loadItem()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(canEdit = hasPermission(user, MATERIEL_ROULANT_MODULE, PermissionAction.EDIT))
            }
        }
    }

    fun loadItem() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = materielRoulantRepository.getMaterielRoulant(materielId)) {
                is Resource.Success -> _state.update {
                    it.copy(item = result.data, isLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }
}
