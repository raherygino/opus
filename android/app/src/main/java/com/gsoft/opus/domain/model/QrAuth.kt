package com.gsoft.opus.domain.model

/**
 * QR-code-based authentication domain models.
 *
 * The QR code only ever carries a short-lived, one-time [requestCode] —
 * never credentials or tokens.
 */

enum class QrAuthDeviceType(val value: String) {
    DESKTOP("desktop"),
    ANDROID("android");

    companion object {
        fun fromValue(value: String): QrAuthDeviceType =
            entries.firstOrNull { it.value == value } ?: DESKTOP
    }
}

enum class QrAuthStatus(val value: String) {
    PENDING("pending"),
    SCANNED("scanned"),
    APPROVED("approved"),
    REJECTED("rejected"),
    EXPIRED("expired"),
    CANCELLED("cancelled"),
    CONSUMED("consumed");

    companion object {
        fun fromValue(value: String): QrAuthStatus =
            entries.firstOrNull { it.value == value } ?: PENDING
    }
}

data class QrAuthRequester(
    val username: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val roleCode: String? = null,
    val roleName: String? = null,
)

data class QrAuthRequestInfo(
    val requestCode: String,
    val deviceType: QrAuthDeviceType,
    val deviceName: String,
    val expiresAt: String? = null,
    val ttlSeconds: Int? = null,
)

data class QrAuthScanResult(
    val requestCode: String,
    val deviceType: QrAuthDeviceType,
    val deviceName: String,
    val requester: QrAuthRequester? = null,
    val expiresAt: String? = null,
)

data class QrAuthStatusResult(
    val requestCode: String,
    val deviceType: QrAuthDeviceType,
    val deviceName: String,
    val status: QrAuthStatus,
    val expiresAt: String? = null,
    val scannedAt: String? = null,
    val resolvedAt: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: User? = null,
)
