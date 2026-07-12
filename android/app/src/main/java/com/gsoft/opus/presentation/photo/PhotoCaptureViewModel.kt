package com.gsoft.opus.presentation.photo

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.data.signature.ConnectionStatus
import com.gsoft.opus.data.signature.IncomingMessage
import com.gsoft.opus.data.signature.OutgoingMessage
import com.gsoft.opus.data.signature.QrPayload
import com.gsoft.opus.data.signature.SignatureWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoCaptureUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val connectionMessage: String? = null,
    val desktopIp: String? = null,
    val isFinished: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class PhotoCaptureViewModel @Inject constructor(
    private val webSocketClient: SignatureWebSocketClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoCaptureUiState())
    val uiState: StateFlow<PhotoCaptureUiState> = _uiState.asStateFlow()

    init {
        observeConnection()
        observeEvents()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            webSocketClient.connectionState.collect { info ->
                _uiState.value = _uiState.value.copy(
                    isConnected = info.status == ConnectionStatus.CONNECTED,
                    isConnecting = info.status == ConnectionStatus.CONNECTING,
                    desktopIp = info.desktopIp,
                    connectionMessage = info.message,
                    errorMessage = if (info.status == ConnectionStatus.FAILED) info.message else null,
                )
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            webSocketClient.events.collect { event ->
                when (event) {
                    is IncomingMessage.PairSuccess -> {
                        sendHello()
                    }
                    is IncomingMessage.PairFailed -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = event.message ?: "Couplage échoué",
                        )
                    }
                    is IncomingMessage.Heartbeat -> {
                        webSocketClient.send(OutgoingMessage.Heartbeat)
                    }
                    is IncomingMessage.PhotoReceived -> {
                        _uiState.value = _uiState.value.copy(isFinished = true)
                    }
                    is IncomingMessage.SignatureSaved -> {
                        // Not relevant for photo, ignore
                    }
                    is IncomingMessage.Disconnect -> {
                        webSocketClient.disconnect()
                    }
                }
            }
        }
    }

    fun connectByQrPayload(payload: QrPayload) {
        webSocketClient.connectByQr(payload.ip, payload.port, payload.sessionId, payload.token)
    }

    fun connectByPairingCode(ip: String, port: Int, code: String) {
        webSocketClient.connectByCode(ip, port, code)
    }

    private fun sendHello() {
        webSocketClient.send(
            OutgoingMessage.Hello(
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                batteryLevel = null,
            ),
        )
    }

    fun sendPhoto(base64Data: String) {
        webSocketClient.send(OutgoingMessage.PhotoData(photoData = base64Data))
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    fun resetState() {
        _uiState.value = PhotoCaptureUiState()
    }

    override fun onCleared() {
        webSocketClient.disconnect()
        super.onCleared()
    }
}
