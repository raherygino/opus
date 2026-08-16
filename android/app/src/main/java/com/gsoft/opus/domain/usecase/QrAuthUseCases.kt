package com.gsoft.opus.domain.usecase

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.QrAuthRequestInfo
import com.gsoft.opus.domain.model.QrAuthScanResult
import com.gsoft.opus.domain.model.QrAuthStatusResult
import com.gsoft.opus.domain.repository.QrAuthRepository
import javax.inject.Inject

class CreateQrAuthRequestUseCase @Inject constructor(
    private val repository: QrAuthRepository
) {
    suspend operator fun invoke(deviceType: String, deviceName: String): Resource<QrAuthRequestInfo> {
        if (deviceType.isBlank()) return Resource.error("Device type is required")
        if (deviceName.isBlank()) return Resource.error("Device name is required")
        return repository.createRequest(deviceType, deviceName)
    }
}

class GetQrAuthStatusUseCase @Inject constructor(
    private val repository: QrAuthRepository
) {
    suspend operator fun invoke(code: String): Resource<QrAuthStatusResult> {
        if (code.isBlank()) return Resource.error("Request code is required")
        return repository.getStatus(code)
    }
}

class ScanQrAuthUseCase @Inject constructor(
    private val repository: QrAuthRepository
) {
    suspend operator fun invoke(code: String): Resource<QrAuthScanResult> {
        if (code.isBlank()) return Resource.error("Request code is required")
        return repository.scan(code)
    }
}

class ApproveQrAuthUseCase @Inject constructor(
    private val repository: QrAuthRepository
) {
    suspend operator fun invoke(code: String): Resource<AuthResult?> {
        if (code.isBlank()) return Resource.error("Request code is required")
        return repository.approve(code)
    }
}

class RejectQrAuthUseCase @Inject constructor(
    private val repository: QrAuthRepository
) {
    suspend operator fun invoke(code: String): Resource<Unit> {
        if (code.isBlank()) return Resource.error("Request code is required")
        return repository.reject(code)
    }
}

class CancelQrAuthUseCase @Inject constructor(
    private val repository: QrAuthRepository
) {
    suspend operator fun invoke(code: String): Resource<Unit> {
        if (code.isBlank()) return Resource.error("Request code is required")
        return repository.cancel(code)
    }
}
