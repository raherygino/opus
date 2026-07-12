package com.gsoft.opus.data.signature

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED,
}

data class ConnectionInfo(
    val status: ConnectionStatus,
    val desktopIp: String? = null,
    val desktopPort: Int? = null,
    val message: String? = null,
)

class SignatureWebSocketClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build(),
) {
    companion object {
        private const val TAG = "SigWebSocketClient"
        private const val RECONNECT_DELAY_MS = 2000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    private val _connectionState = MutableStateFlow(ConnectionInfo(ConnectionStatus.DISCONNECTED))
    val connectionState: StateFlow<ConnectionInfo> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 64)
    val events: SharedFlow<IncomingMessage> = _events.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectAttempts = 0
    private var shouldReconnect = false
    private var lastIp: String? = null
    private var lastPort: Int? = null
    private var lastSessionId: String? = null
    private var lastToken: String? = null
    private var lastCode: String? = null

    fun connectByQr(ip: String, port: Int, sessionId: String, token: String) {
        lastIp = ip
        lastPort = port
        lastSessionId = sessionId
        lastToken = token
        lastCode = null
        shouldReconnect = true
        reconnectAttempts = 0
        doConnect()
    }

    fun connectByCode(ip: String, port: Int, code: String) {
        lastIp = ip
        lastPort = port
        lastSessionId = null
        lastToken = null
        lastCode = code
        shouldReconnect = true
        reconnectAttempts = 0
        doConnect()
    }

    private fun doConnect() {
        val ip = lastIp ?: return
        val port = lastPort ?: return

        _connectionState.value = ConnectionInfo(
            status = ConnectionStatus.CONNECTING,
            desktopIp = ip,
            desktopPort = port,
        )

        val urlBuilder = StringBuilder("ws://$ip:$port?")
        lastSessionId?.let { urlBuilder.append("sessionId=$it&") }
        lastToken?.let { urlBuilder.append("token=$it&") }
        lastCode?.let { urlBuilder.append("code=$it&") }
        val url = urlBuilder.toString().trimEnd('&', '?')

        Log.i(TAG, "Connecting to $url")

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected")
                reconnectAttempts = 0
                _connectionState.value = ConnectionInfo(
                    status = ConnectionStatus.CONNECTED,
                    desktopIp = ip,
                    desktopPort = port,
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = json.decodeFromString<IncomingMessage>(text)
                    scope.launch { _events.emit(msg) }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse message: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code $reason")
                handleDisconnect(code)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                handleDisconnect(0)
            }
        })
    }

    private fun handleDisconnect(code: Int) {
        _connectionState.value = ConnectionInfo(
            status = if (code in 4000..4100) ConnectionStatus.FAILED else ConnectionStatus.DISCONNECTED,
            desktopIp = lastIp,
            desktopPort = lastPort,
            message = if (code == 4002) "Code de couplage invalide" else null,
        )

        if (shouldReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS && code !in 4001..4003) {
            reconnectAttempts++
            scope.launch {
                delay(RECONNECT_DELAY_MS)
                if (shouldReconnect) doConnect()
            }
        }
    }

    fun send(message: OutgoingMessage) {
        val ws = webSocket ?: return
        val jsonStr = json.encodeToString(message)
        ws.send(jsonStr)
    }

    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "client_disconnect")
        webSocket = null
        _connectionState.value = ConnectionInfo(status = ConnectionStatus.DISCONNECTED)
    }
}
