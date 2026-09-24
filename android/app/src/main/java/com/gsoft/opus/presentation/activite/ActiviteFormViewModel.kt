package com.gsoft.opus.presentation.activite

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.LocationCapture
import com.gsoft.opus.core.LocationResult
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Activite
import com.gsoft.opus.domain.repository.ActiviteFormData
import com.gsoft.opus.domain.repository.ActiviteRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** One selectable patrol mode row (e.g. diurne × motorisée) + its itinerary. */
data class PatrouilleRow(
    val type: String,
    val mode: String,
    val selected: Boolean = false,
    val itineraire: String = ""
)

data class ActiviteFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val dateActivite: String = "",
    val heureActivite: String = "",
    /** All six patrol mode rows — selection toggles the itinerary field. */
    val patrouilles: List<PatrouilleRow> = emptyList(),
    val operationCiblee: String = "",
    val faitsConstates: String = "",
    val compteRenduHierarchie: String = "",
    val conduiteATenir: String = "",
    val natureIntervention: String = "",
    val suitesDonnees: String = "",
    // GPS location — required on Android to create an activité.
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isCapturingLocation: Boolean = false,
    val locationError: String? = null,
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ActiviteFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ActiviteRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val activiteId: Int = savedStateHandle.get<Int>("activiteId") ?: 0

    private val _state = MutableStateFlow(
        ActiviteFormUiState(
            isEdit = activiteId > 0,
            dateActivite = if (activiteId > 0) "" else todayIso(),
            heureActivite = if (activiteId > 0) "" else nowHHmm(),
            patrouilles = Activite.PATROUILLE_TYPES.flatMap { (type, _) ->
                Activite.PATROUILLE_MODES.map { (mode, _) -> PatrouilleRow(type, mode) }
            }
        )
    )
    val state: StateFlow<ActiviteFormUiState> = _state.asStateFlow()

    init {
        if (activiteId > 0) {
            loadEntry()
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getActivite(activiteId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            dateActivite = e.dateActivite.take(10),
                            heureActivite = e.heureDisplay,
                            patrouilles = it.patrouilles.map { row ->
                                row.copy(
                                    itineraire = e.itineraire(row.type, row.mode).orEmpty(),
                                    selected = e.itineraire(row.type, row.mode) != null
                                )
                            },
                            operationCiblee = e.operationCiblee.orEmpty(),
                            faitsConstates = e.faitsConstates.orEmpty(),
                            compteRenduHierarchie = e.compteRenduHierarchie.orEmpty(),
                            conduiteATenir = e.conduiteATenir.orEmpty(),
                            natureIntervention = e.natureIntervention.orEmpty(),
                            suitesDonnees = e.suitesDonnees.orEmpty(),
                            latitude = e.latitude,
                            longitude = e.longitude
                        )
                    }
                    when (val atts = repository.getAttachments(activiteId)) {
                        is Resource.Success -> _state.update {
                            it.copy(attachments = atts.data.map { a -> AttachmentItem(id = a.id, title = a.title, existingFilename = a.originalFilename) })
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateDateActivite(value: String) { _state.update { it.copy(dateActivite = value) } }
    fun updateHeureActivite(value: String) { _state.update { it.copy(heureActivite = value) } }
    fun updateOperationCiblee(value: String) { _state.update { it.copy(operationCiblee = value) } }
    fun updateFaitsConstates(value: String) { _state.update { it.copy(faitsConstates = value) } }
    fun updateCompteRenduHierarchie(value: String) { _state.update { it.copy(compteRenduHierarchie = value) } }
    fun updateConduiteATenir(value: String) { _state.update { it.copy(conduiteATenir = value) } }
    fun updateNatureIntervention(value: String) { _state.update { it.copy(natureIntervention = value) } }
    fun updateSuitesDonnees(value: String) { _state.update { it.copy(suitesDonnees = value) } }

    // ─── Patrol selection ───────────────────────────────────────────

    fun togglePatrouille(type: String, mode: String) {
        _state.update { s ->
            s.copy(patrouilles = s.patrouilles.map {
                if (it.type == type && it.mode == mode) it.copy(selected = !it.selected) else it
            })
        }
    }

    fun updatePatrouilleItineraire(type: String, mode: String, value: String) {
        _state.update { s ->
            s.copy(patrouilles = s.patrouilles.map {
                if (it.type == type && it.mode == mode) it.copy(itineraire = value) else it
            })
        }
    }

    private fun patrouilleItineraire(type: String, mode: String): String? =
        _state.value.patrouilles
            .firstOrNull { it.type == type && it.mode == mode }
            ?.let { if (it.selected) it.itineraire else null }

    // ─── Attachments ────────────────────────────────────────────────

    fun addAttachment() {
        _state.update { it.copy(attachments = it.attachments + AttachmentItem()) }
    }

    fun updateAttachmentTitle(index: Int, title: String) {
        _state.update { s ->
            s.copy(attachments = s.attachments.mapIndexed { i, a -> if (i == index) a.copy(title = title) else a })
        }
    }

    fun setAttachmentFile(index: Int, file: UploadFile) {
        _state.update { s ->
            s.copy(attachments = s.attachments.mapIndexed { i, a -> if (i == index) a.copy(uploadFile = file) else a })
        }
    }

    fun removeAttachment(index: Int) {
        _state.update { s ->
            val list = s.attachments.toMutableList()
            if (list[index].id != null) {
                list[index] = list[index].copy(isDeleted = true)
            } else {
                list.removeAt(index)
            }
            s.copy(attachments = list)
        }
    }

    // ─── GPS location capture ───────────────────────────────────────
    // On Android, the agent must enable location services and the app
    // captures the device's latitude/longitude before an activité can
    // be created — same requirement as the évènement survenu feature.

    /** Whether the app has been granted location permissions. */
    fun hasLocationPermission(): Boolean = LocationCapture.hasPermission(context)

    /** Whether location services (GPS/network provider) are enabled on the device. */
    fun isLocationEnabled(): Boolean = LocationCapture.isLocationEnabled(context)

    /**
     * Capture the current device location. On success, the
     * latitude/longitude are stored in the UI state. On failure, a
     * location error message is shown. This must succeed before a new
     * activité can be saved.
     */
    fun captureLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isCapturingLocation = true, locationError = null) }
            when (val result = LocationCapture.capture(context)) {
                is LocationResult.Success -> _state.update {
                    it.copy(
                        isCapturingLocation = false,
                        latitude = result.latitude,
                        longitude = result.longitude,
                        locationError = null
                    )
                }
                is LocationResult.Error -> _state.update {
                    it.copy(
                        isCapturingLocation = false,
                        locationError = result.message
                    )
                }
            }
        }
    }

    fun save() {
        val s = _state.value
        val errors = validateActiviteForm(
            dateActivite = s.dateActivite,
            heureActivite = s.heureActivite
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        // On create, GPS location is required (mobile-only requirement).
        if (!s.isEdit && (s.latitude == null || s.longitude == null)) {
            _state.update {
                it.copy(errorMessage = "La localisation GPS est requise. Activez les services de localisation et capturez votre position.")
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = ActiviteFormData(
                dateActivite = s.dateActivite,
                heureActivite = s.heureActivite,
                patrouilleDiurneMotoriseeItineraire = patrouilleItineraire("diurne", "motorisee"),
                patrouilleDiurnePedestreItineraire = patrouilleItineraire("diurne", "pedestre"),
                patrouilleDiurnePorteeItineraire = patrouilleItineraire("diurne", "portee"),
                patrouilleNocturneMotoriseeItineraire = patrouilleItineraire("nocturne", "motorisee"),
                patrouilleNocturnePedestreItineraire = patrouilleItineraire("nocturne", "pedestre"),
                patrouilleNocturnePorteeItineraire = patrouilleItineraire("nocturne", "portee"),
                operationCiblee = s.operationCiblee.trim().ifBlank { null },
                faitsConstates = s.faitsConstates.trim().ifBlank { null },
                compteRenduHierarchie = s.compteRenduHierarchie.trim().ifBlank { null },
                conduiteATenir = s.conduiteATenir.trim().ifBlank { null },
                natureIntervention = s.natureIntervention.trim().ifBlank { null },
                suitesDonnees = s.suitesDonnees.trim().ifBlank { null },
                latitude = s.latitude,
                longitude = s.longitude
            )
            val result = if (s.isEdit) {
                repository.updateActivite(activiteId, data)
            } else {
                repository.createActivite(data)
            }
            when (result) {
                is Resource.Success -> handleAttachments(result.data.id)
                is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    private suspend fun handleAttachments(savedId: Int) {
        val s = _state.value
        // Delete marked attachments
        for (a in s.attachments.filter { it.isDeleted && it.id != null }) {
            repository.deleteAttachment(savedId, a.id!!)
        }
        // Add/update non-deleted attachments
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                // Replace: delete then re-create
                repository.deleteAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    repository.addAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                repository.updateAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                repository.addAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun nowHHmm(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
