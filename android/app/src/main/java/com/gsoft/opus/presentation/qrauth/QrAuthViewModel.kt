package com.gsoft.opus.presentation.qrauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.QrAuthScanResult
import com.gsoft.opus.domain.usecase.ApproveQrAuthUseCase
import com.gsoft.opus.domain.usecase.RejectQrAuthUseCase
import com.gsoft.opus.domain.usecase.ScanQrAuthUseCase
import com.gsoft.opus.notifications.FcmTokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phases of the QR scan-to-login flow on the phone:
 *   scanning → validating → confirming → approving → success / rejected / error
 */
enum class QrAuthPhase {
    SCANNING,
    VALIDATING,
    CONFIRMING,
    APPROVING,
    SUCCESS,
    REJECTED,
    ERROR,
}

data class QrAuthUiState(
    val phase: QrAuthPhase = QrAuthPhase.SCANNING,
    val scanResult: QrAuthScanResult? = null,
    val errorMessage: String? = null,
    val authResult: AuthResult? = null,
)

@HiltViewModel
class QrAuthViewModel @Inject constructor(
    private val scanQrAuthUseCase: ScanQrAuthUseCase,
    private val approveQrAuthUseCase: ApproveQrAuthUseCase,
    private val rejectQrAuthUseCase: RejectQrAuthUseCase,
    private val fcmTokenManager: FcmTokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(QrAuthUiState())
    val state: StateFlow<QrAuthUiState> = _state.asStateFlow()

    /** Called when the camera detects a QR code. Parses the payload and scans. */
    fun onQrCodeScanned(rawValue: String) {
        val current = _state.value
        if (current.phase != QrAuthPhase.SCANNING) return

        val code = parseQrPayload(rawValue) ?: rawValue.trim()
        if (code.isBlank()) return

        _state.update { it.copy(phase = QrAuthPhase.VALIDATING, errorMessage = null) }

        viewModelScope.launch {
            when (val result = scanQrAuthUseCase(code)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(phase = QrAuthPhase.CONFIRMING, scanResult = result.data)
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            phase = QrAuthPhase.ERROR,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun approve() {
        val current = _state.value
        val scanResult = current.scanResult ?: return
        if (current.phase != QrAuthPhase.CONFIRMING) return

        _state.update { it.copy(phase = QrAuthPhase.APPROVING, errorMessage = null) }

        viewModelScope.launch {
            when (val result = approveQrAuthUseCase(scanResult.requestCode)) {
                is Resource.Success -> {
                    // If tokens were returned (reverse flow), register FCM and complete.
                    result.data?.let { authResult ->
                        fcmTokenManager.fetchAndRegisterToken()
                    }
                    _state.update {
                        it.copy(
                            phase = QrAuthPhase.SUCCESS,
                            authResult = result.data
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            phase = QrAuthPhase.ERROR,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun reject() {
        val current = _state.value
        val scanResult = current.scanResult ?: return
        if (current.phase != QrAuthPhase.CONFIRMING) return

        viewModelScope.launch {
            rejectQrAuthUseCase(scanResult.requestCode)
            _state.update { it.copy(phase = QrAuthPhase.REJECTED) }
        }
    }

    fun resetToScanning() {
        _state.update {
            QrAuthUiState(phase = QrAuthPhase.SCANNING)
        }
    }

    fun dismissError() {
        resetToScanning()
    }

    companion object {
        /**
         * The desktop QR payload is JSON: {"opus":"qr_auth","code":"...","v":1}.
         * Extract the code. If parsing fails, return the raw value (the API
         * will reject it gracefully).
         */
        private fun parseQrPayload(rawValue: String): String? {
            return try {
                val json = org.json.JSONObject(rawValue)
                json.optString("code").takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
        }
    }
}
