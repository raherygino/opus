package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.QrAuthRequestInfo
import com.gsoft.opus.domain.model.QrAuthScanResult
import com.gsoft.opus.domain.model.QrAuthStatusResult

interface QrAuthRepository {
    /** Create a pending QR auth request (the device that needs auth calls this). */
    suspend fun createRequest(deviceType: String, deviceName: String): Resource<QrAuthRequestInfo>

    /** Poll the status of a request (the requesting device polls after creating). */
    suspend fun getStatus(code: String): Resource<QrAuthStatusResult>

    /** Phone calls this after scanning to mark as scanned and get device identity. */
    suspend fun scan(code: String): Resource<QrAuthScanResult>

    /**
     * Phone calls this to approve. For the reverse flow (device_type=android),
     * returns an [AuthResult] so the phone can complete its login directly.
     * For the forward flow (device_type=desktop), returns null (the desktop
     * retrieves tokens via polling).
     */
    suspend fun approve(code: String): Resource<AuthResult?>

    /** Phone calls this to reject. */
    suspend fun reject(code: String): Resource<Unit>

    /** Requesting device cancels the request. */
    suspend fun cancel(code: String): Resource<Unit>
}
