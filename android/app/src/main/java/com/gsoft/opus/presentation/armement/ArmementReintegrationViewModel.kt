package com.gsoft.opus.presentation.armement

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Armement
import com.gsoft.opus.domain.repository.ArmementRepository
import com.gsoft.opus.domain.repository.ReintegrationData
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ArmementReintegrationUiState(
    val armement: Armement? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val notFound: Boolean = false,
    // Form fields
    val dateReintegration: String = "",
    val heureReintegration: String = "",
    val etatReintegration: String = "",
    val munitionsConsommees: String = "",
    // GPS
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isCapturingLocation: Boolean = false,
    val locationError: String? = null,
    val hasLocationPermission: Boolean = false
)

@HiltViewModel
class ArmementReintegrationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val armementRepository: ArmementRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val armementId: Int = savedStateHandle.get<Int>("armementId") ?: 0

    private val _state = MutableStateFlow(ArmementReintegrationUiState())
    val state: StateFlow<ArmementReintegrationUiState> = _state.asStateFlow()

    init {
        loadArmement()
        checkPermission()
    }

    private fun loadArmement() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = armementRepository.getArmement(armementId)) {
                is Resource.Success -> {
                    val armement = result.data
                    val now = Date()
                    _state.update {
                        it.copy(
                            armement = armement,
                            isLoading = false,
                            dateReintegration = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now),
                            heureReintegration = SimpleDateFormat("HH:mm", Locale.US).format(now)
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

    private fun checkPermission() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _state.update { it.copy(hasLocationPermission = granted) }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasLocationPermission = granted) }
        if (!granted) {
            _state.update { it.copy(locationError = "Autorisation de localisation requise") }
        }
    }

    // ─── Form field updates ────────────────────────────────────────

    fun updateDateReintegration(date: String) {
        _state.update { it.copy(dateReintegration = date) }
    }

    fun updateHeureReintegration(heure: String) {
        _state.update { it.copy(heureReintegration = heure) }
    }

    fun updateEtatReintegration(etat: String) {
        _state.update { it.copy(etatReintegration = etat) }
    }

    fun updateMunitionsConsommees(munitions: String) {
        _state.update { it.copy(munitionsConsommees = munitions.filter(Char::isDigit)) }
    }

    // ─── GPS location capture ──────────────────────────────────────

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    fun captureLocation() {
        val s = _state.value
        if (!s.hasLocationPermission) {
            _state.update { it.copy(locationError = "Autorisation de localisation requise") }
            return
        }
        if (!isLocationEnabled()) {
            _state.update { it.copy(locationError = "Activez les services de localisation (GPS) sur votre appareil") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCapturingLocation = true, locationError = null) }
            try {
                val location = LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .await()
                if (location != null) {
                    _state.update {
                        it.copy(
                            isCapturingLocation = false,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            locationError = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isCapturingLocation = false,
                            locationError = "Impossible d'obtenir la position. Réessayez."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isCapturingLocation = false,
                        locationError = "Erreur de localisation: ${e.message ?: "inconnue"}"
                    )
                }
            }
        }
    }

    fun clearLocation() {
        _state.update { it.copy(latitude = null, longitude = null, locationError = null) }
    }

    // ─── Submit ────────────────────────────────────────────────────

    fun submit() {
        val s = _state.value
        val armement = s.armement ?: return

        // Validate
        val validationError = ArmementFormValidator.validateReintegration(
            heureReintegration = s.heureReintegration,
            etatReintegration = s.etatReintegration,
            munitionsConsommees = s.munitionsConsommees,
            munitionsPercues = armement.munitions
        )
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }
        if (s.dateReintegration.isBlank()) {
            _state.update { it.copy(errorMessage = "La date de la réintégration est requise") }
            return
        }
        if (s.latitude == null || s.longitude == null) {
            _state.update { it.copy(errorMessage = "La capture de la position GPS est requise pour la réintégration") }
            return
        }

        _state.update { it.copy(errorMessage = null, isSaving = true) }
        viewModelScope.launch {
            val data = ReintegrationData(
                heureReintegration = s.heureReintegration,
                dateReintegration = s.dateReintegration,
                etatReintegration = s.etatReintegration,
                munitionsConsommees = s.munitionsConsommees.trim().toInt(),
                reintegrationLatitude = s.latitude,
                reintegrationLongitude = s.longitude
            )
            when (val result = armementRepository.reintegrateArmement(armementId, data)) {
                is Resource.Success -> _state.update {
                    it.copy(isSaving = false, userMessage = "Arme réintégrée avec succès")
                }
                is Resource.Error -> _state.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null, errorMessage = null) }
    }
}
