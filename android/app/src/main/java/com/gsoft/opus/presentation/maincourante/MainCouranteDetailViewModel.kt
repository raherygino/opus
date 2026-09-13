package com.gsoft.opus.presentation.maincourante

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.MainCourante
import com.gsoft.opus.domain.model.MainCouranteAttachment
import com.gsoft.opus.domain.repository.MainCouranteRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainCouranteDetailUiState(
    val entry: MainCourante? = null,
    val attachments: List<MainCouranteAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class MainCouranteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mainCouranteRepository: MainCouranteRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("mainCouranteId") ?: 0
    private val origine: String = savedStateHandle.get<String>("origine") ?: "Secretariat"
    private val module = moduleForOrigine(origine)

    private val _state = MutableStateFlow(MainCouranteDetailUiState())
    val state: StateFlow<MainCouranteDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            _state.update {
                it.copy(
                    canEdit = hasPermission(user, module, PermissionAction.EDIT)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = mainCouranteRepository.getMainCourante(entryId)) {
                is Resource.Success -> {
                    val entry = result.data
                    _state.update {
                        it.copy(entry = entry)
                    }
                    val attachmentsDeferred = async { mainCouranteRepository.getAttachments(entryId) }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            attachments = attachmentsDeferred.await().getOrNull() ?: emptyList()
                        )
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
        _state.update { it.copy(userMessage = null) }
    }
}
