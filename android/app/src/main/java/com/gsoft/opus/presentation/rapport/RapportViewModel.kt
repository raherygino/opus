package com.gsoft.opus.presentation.rapport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Rapport
import com.gsoft.opus.domain.repository.RapportRepository
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class RapportType(val apiValue: String, val label: String) {
    DAILY("daily", "Journalier"),
    WEEKLY("weekly", "Hebdomadaire"),
    MONTHLY("monthly", "Mensuel")
}

data class RapportUiState(
    val isLoading: Boolean = true,
    val type: RapportType = RapportType.DAILY,
    /** Anchor date the server builds the period around. */
    val date: LocalDate = LocalDate.now(),
    val rapport: Rapport? = null,
    val errorMessage: String? = null
) {
    /**
     * Human-readable period label derived from the server-computed bounds
     * (periodStart / periodEnd), so it always matches the counted data.
     */
    val periodLabel: String
        get() {
            val r = rapport ?: return ""
            return when (r.type) {
                "daily" -> formatDateDisplay(r.periodStart)
                "weekly" -> "Semaine du ${formatDateDisplay(r.periodStart)} au ${formatDateDisplay(r.periodEnd)}"
                else -> runCatching {
                    val month = LocalDate.parse(r.periodStart.take(10))
                        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))
                    month.replaceFirstChar { it.uppercase(Locale.FRENCH) }
                }.getOrDefault(r.periodStart)
            }
        }
}

@HiltViewModel
class RapportViewModel @Inject constructor(
    private val rapportRepository: RapportRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RapportUiState())
    val state: StateFlow<RapportUiState> = _state.asStateFlow()

    init {
        loadRapport()
    }

    fun refresh() = loadRapport()

    fun setType(type: RapportType) {
        if (type == _state.value.type) return
        _state.update { it.copy(type = type) }
        loadRapport()
    }

    fun setDate(date: LocalDate) {
        if (date == _state.value.date) return
        _state.update { it.copy(date = date) }
        loadRapport()
    }

    /** Move the anchor date one period backward/forward (day / ISO week / month). */
    fun shiftPeriod(delta: Long) {
        val s = _state.value
        val newDate = when (s.type) {
            RapportType.DAILY -> s.date.plusDays(delta)
            RapportType.WEEKLY -> s.date.plusWeeks(delta)
            RapportType.MONTHLY -> s.date.plusMonths(delta)
        }
        setDate(newDate)
    }

    fun goToToday() = setDate(LocalDate.now())

    private fun loadRapport() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val s = _state.value
            when (val result = rapportRepository.getRapport(s.type.apiValue, s.date.toString())) {
                is Resource.Success -> _state.update {
                    it.copy(isLoading = false, rapport = result.data)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
