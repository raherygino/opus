package com.gsoft.opus.presentation.materiel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.AffectationMateriel
import com.gsoft.opus.domain.repository.MaterielRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaterielDetailUiState(
    val affectation: AffectationMateriel? = null,
    val isLoading: Boolean = true,
    val canEdit: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MaterielDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val materielRepository: MaterielRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val affectationId: Int = savedStateHandle.get<Int>("affectationId") ?: 0

    private val _state = MutableStateFlow(MaterielDetailUiState())
    val state: StateFlow<MaterielDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        loadAffectation()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(canEdit = hasPermission(user, MATERIEL_MODULE, PermissionAction.EDIT))
            }
        }
    }

    fun loadAffectation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = materielRepository.getAffectationMateriel(affectationId)) {
                is Resource.Success -> _state.update {
                    it.copy(affectation = result.data, isLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }
}
