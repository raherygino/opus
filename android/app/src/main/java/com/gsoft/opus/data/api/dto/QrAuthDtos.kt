package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

data class QrAuthRequestDto(
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_name") val deviceName: String
)

data class QrAuthRequestResponseDto(
    @SerializedName("request_code") val requestCode: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("ttl_seconds") val ttlSeconds: Int? = null
)

data class QrAuthRequesterDto(
    @SerializedName("username") val username: String? = null,
    @SerializedName("firstname") val firstname: String? = null,
    @SerializedName("lastname") val lastname: String? = null,
    @SerializedName("role_code") val roleCode: String? = null,
    @SerializedName("role_name") val roleName: String? = null
)

data class QrAuthScanResponseDto(
    @SerializedName("request_code") val requestCode: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("requester") val requester: QrAuthRequesterDto? = null,
    @SerializedName("expires_at") val expiresAt: String? = null
)

data class QrAuthStatusResponseDto(
    @SerializedName("request_code") val requestCode: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("status") val status: String,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("scanned_at") val scannedAt: String? = null,
    @SerializedName("resolved_at") val resolvedAt: String? = null,
    // Present only once when status == "approved" (one-time retrieval)
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("user") val user: UserDto? = null
)

/**
 * Response from the approve endpoint. For the reverse flow (device_type=android),
 * the server returns tokens directly so the phone can complete its login.
 * For the forward flow (device_type=desktop), only device info is returned.
 */
data class QrAuthApproveResponseDto(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("user") val user: UserDto? = null,
    @SerializedName("device_type") val deviceType: String? = null,
    @SerializedName("device_name") val deviceName: String? = null
)
