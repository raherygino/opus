package com.gsoft.opus.presentation.signature

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.data.signature.ConnectionStatus
import com.gsoft.opus.data.signature.IncomingMessage
import com.gsoft.opus.data.signature.OutgoingMessage
import com.gsoft.opus.data.signature.QrPayload
import com.gsoft.opus.data.signature.SignatureWebSocketClient
import com.gsoft.opus.data.signature.Stroke
import com.gsoft.opus.data.signature.StrokePoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class SignaturePadUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val connectionMessage: String? = null,
    val desktopIp: String? = null,
    val strokes: List<Stroke> = emptyList(),
    val currentStroke: MutableList<StrokePoint> = mutableListOf(),
    val isFinished: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class SignaturePadViewModel @Inject constructor(
    private val webSocketClient: SignatureWebSocketClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignaturePadUiState())
    val uiState: StateFlow<SignaturePadUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

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
                    is IncomingMessage.SignatureSaved -> {
                        _uiState.value = _uiState.value.copy(isFinished = true)
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

    fun startStroke(x: Float, y: Float) {
        val point = StrokePoint(x = x, y = y, timestamp = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(
            currentStroke = mutableListOf(point),
        )
    }

    fun moveStroke(x: Float, y: Float) {
        val point = StrokePoint(x = x, y = y, timestamp = System.currentTimeMillis())
        _uiState.value.currentStroke.add(point)
    }

    fun endStroke(x: Float, y: Float) {
        val point = StrokePoint(x = x, y = y, timestamp = System.currentTimeMillis())
        _uiState.value.currentStroke.add(point)
        val completedStroke = Stroke(points = _uiState.value.currentStroke.toList())
        if (completedStroke.points.size > 1) {
            _uiState.value = _uiState.value.copy(
                strokes = _uiState.value.strokes + completedStroke,
                currentStroke = mutableListOf(),
            )
        } else {
            _uiState.value = _uiState.value.copy(currentStroke = mutableListOf())
        }
    }

    fun clearSignature() {
        _uiState.value = _uiState.value.copy(
            strokes = emptyList(),
            currentStroke = mutableListOf(),
        )
    }

    fun undoStroke() {
        _uiState.value = _uiState.value.copy(
            strokes = _uiState.value.strokes.dropLast(1),
            currentStroke = mutableListOf(),
        )
    }

    fun finishSignature() {
        val strokes = _uiState.value.strokes
        if (strokes.isEmpty()) return
        webSocketClient.send(OutgoingMessage.CompleteSignature(strokes))
        _uiState.value = _uiState.value.copy(isFinished = true)
        // Don't disconnect — keep connection alive until user navigates back
    }

    fun cancelSignature() {
        webSocketClient.send(OutgoingMessage.CancelSignature)
        webSocketClient.disconnect()
        _uiState.value = _uiState.value.copy(isFinished = true)
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    fun resetState() {
        _uiState.value = SignaturePadUiState()
    }

    override fun onCleared() {
        webSocketClient.disconnect()
        super.onCleared()
    }
}
