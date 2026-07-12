package com.gsoft.opus.data.signature

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long,
    val pressure: Float = 1f,
)

@Serializable
data class Stroke(
    val points: List<StrokePoint> = emptyList(),
)

// ─── Incoming messages (from desktop) ───

@Serializable
sealed class IncomingMessage {
    @Serializable
    @SerialName("PAIR_SUCCESS")
    data class PairSuccess(
        val sessionId: String,
        val message: String? = null,
    ) : IncomingMessage()

    @Serializable
    @SerialName("PAIR_FAILED")
    data class PairFailed(
        val message: String? = null,
    ) : IncomingMessage()

    @Serializable
    @SerialName("HEARTBEAT")
    data object Heartbeat : IncomingMessage()

    @Serializable
    @SerialName("SIGNATURE_SAVED")
    data class SignatureSaved(
        val message: String? = null,
    ) : IncomingMessage()

    @Serializable
    @SerialName("DISCONNECT")
    data object Disconnect : IncomingMessage()
}

// ─── Outgoing messages (from Android) ───

@Serializable
sealed class OutgoingMessage {
    @Serializable
    @SerialName("HELLO")
    data class Hello(
        val deviceName: String,
        val batteryLevel: Float? = null,
    ) : OutgoingMessage()

    @Serializable
    @SerialName("HEARTBEAT")
    data object Heartbeat : OutgoingMessage()

    @Serializable
    @SerialName("START_STROKE")
    data class StartStroke(
        val x: Float,
        val y: Float,
        val timestamp: Long,
        val pressure: Float = 1f,
    ) : OutgoingMessage()

    @Serializable
    @SerialName("MOVE_STROKE")
    data class MoveStroke(
        val x: Float,
        val y: Float,
        val timestamp: Long,
        val pressure: Float = 1f,
    ) : OutgoingMessage()

    @Serializable
    @SerialName("END_STROKE")
    data class EndStroke(
        val x: Float,
        val y: Float,
        val timestamp: Long,
        val pressure: Float = 1f,
    ) : OutgoingMessage()

    @Serializable
    @SerialName("CLEAR_SIGNATURE")
    data object ClearSignature : OutgoingMessage()

    @Serializable
    @SerialName("UNDO_STROKE")
    data object UndoStroke : OutgoingMessage()

    @Serializable
    @SerialName("COMPLETE_SIGNATURE")
    data class CompleteSignature(
        val strokes: List<Stroke>,
    ) : OutgoingMessage()

    @Serializable
    @SerialName("CANCEL_SIGNATURE")
    data object CancelSignature : OutgoingMessage()

    @Serializable
    @SerialName("DISCONNECT")
    data object Disconnect : OutgoingMessage()
}

// ─── QR code payload ───

@Serializable
data class QrPayload(
    val ip: String,
    val port: Int,
    val sessionId: String,
    val token: String,
)
