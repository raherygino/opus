package com.gsoft.opus.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.DashboardStats
import com.gsoft.opus.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /**
     * Reloads the aggregated KPI counts. The loading indicator is only shown
     * while no data is displayed yet — periodic/foreground refreshes update
     * the figures silently in place.
     */
    fun refresh() {
        viewModelScope.launch {
            val hadData = _state.value.stats != null
            if (!hadData) {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
            }
            when (val result = dashboardRepository.getStats()) {
                is Resource.Success -> _state.update {
                    it.copy(isLoading = false, stats = result.data, errorMessage = null)
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isLoading = false,
                        // Keep showing stale data; surface the error only when
                        // there is nothing to display.
                        errorMessage = if (it.stats == null) result.message else null
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
