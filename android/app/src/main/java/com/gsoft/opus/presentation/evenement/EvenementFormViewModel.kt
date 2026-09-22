package com.gsoft.opus.presentation.evenement

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.LocationCapture
import com.gsoft.opus.core.LocationResult
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.EvenementSurvenuFormData
import com.gsoft.opus.domain.repository.EvenementSurvenuRepository
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

data class EvenementFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val dateEvenement: String = "",
    val heureEvenement: String = "",
    val typeEvenement: String = "",
    val lieuExact: String = "",
    val auteursPresumes: String = "",
    val victimes: String = "",
    val temoins: String = "",
    val mesuresPrises: String = "",
    // GPS location — required on Android to create an évènement survenu.
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isCapturingLocation: Boolean = false,
    val locationError: String? = null,
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class EvenementFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EvenementSurvenuRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val evenementId: Int = savedStateHandle.get<Int>("evenementId") ?: 0

    private val _state = MutableStateFlow(
        EvenementFormUiState(
            isEdit = evenementId > 0,
            dateEvenement = if (evenementId > 0) "" else todayIso(),
            heureEvenement = if (evenementId > 0) "" else nowHHmm()
        )
    )
    val state: StateFlow<EvenementFormUiState> = _state.asStateFlow()

    init {
        if (evenementId > 0) {
            loadEntry()
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getEvenement(evenementId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            dateEvenement = e.dateEvenement.take(10),
                            heureEvenement = e.heureDisplay,
                            typeEvenement = e.typeEvenement,
                            lieuExact = e.lieuExact,
                            auteursPresumes = e.auteursPresumes.orEmpty(),
                            victimes = e.victimes.orEmpty(),
                            temoins = e.temoins.orEmpty(),
                            mesuresPrises = e.mesuresPrises.orEmpty(),
                            latitude = e.latitude,
                            longitude = e.longitude
                        )
                    }
                    when (val atts = repository.getAttachments(evenementId)) {
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

    fun updateDateEvenement(value: String) { _state.update { it.copy(dateEvenement = value) } }
    fun updateHeureEvenement(value: String) { _state.update { it.copy(heureEvenement = value) } }
    fun updateTypeEvenement(value: String) { _state.update { it.copy(typeEvenement = value) } }
    fun updateLieuExact(value: String) { _state.update { it.copy(lieuExact = value) } }
    fun updateAuteursPresumes(value: String) { _state.update { it.copy(auteursPresumes = value) } }
    fun updateVictimes(value: String) { _state.update { it.copy(victimes = value) } }
    fun updateTemoins(value: String) { _state.update { it.copy(temoins = value) } }
    fun updateMesuresPrises(value: String) { _state.update { it.copy(mesuresPrises = value) } }

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

    // ─── GPS location capture ───────────────────────────────────────
    // On Android, the agent must enable location services and the app
    // captures the device's latitude/longitude before an évènement can
    // be created — same requirement as the armement feature.

    /** Whether the app has been granted location permissions. */
    fun hasLocationPermission(): Boolean = LocationCapture.hasPermission(context)

    /** Whether location services (GPS/network provider) are enabled on the device. */
    fun isLocationEnabled(): Boolean = LocationCapture.isLocationEnabled(context)

    /**
     * Capture the current device location. On success, the
     * latitude/longitude are stored in the UI state. On failure, a
     * location error message is shown. This must succeed before a new
     * évènement can be saved.
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

    fun save() {
        val s = _state.value
        val errors = validateEvenementForm(
            dateEvenement = s.dateEvenement,
            heureEvenement = s.heureEvenement,
            typeEvenement = s.typeEvenement,
            lieuExact = s.lieuExact
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
            val data = EvenementSurvenuFormData(
                dateEvenement = s.dateEvenement,
                heureEvenement = s.heureEvenement,
                typeEvenement = s.typeEvenement,
                lieuExact = s.lieuExact.trim(),
                auteursPresumes = s.auteursPresumes.trim().ifBlank { null },
                victimes = s.victimes.trim().ifBlank { null },
                temoins = s.temoins.trim().ifBlank { null },
                mesuresPrises = s.mesuresPrises.trim().ifBlank { null },
                latitude = s.latitude,
                longitude = s.longitude
            )
            val result = if (s.isEdit) {
                repository.updateEvenement(evenementId, data)
            } else {
                repository.createEvenement(data)
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
