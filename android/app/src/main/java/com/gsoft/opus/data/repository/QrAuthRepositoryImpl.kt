package com.gsoft.opus.data.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.QrAuthRequestDto
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.data.local.UserPreferences
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.QrAuthRequestInfo
import com.gsoft.opus.domain.model.QrAuthScanResult
import com.gsoft.opus.domain.model.QrAuthStatusResult
import com.gsoft.opus.domain.repository.QrAuthRepository
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrAuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) : QrAuthRepository {

    override suspend fun createRequest(deviceType: String, deviceName: String): Resource<QrAuthRequestInfo> {
        return try {
            val response = apiService.createQrAuthRequest(
                QrAuthRequestDto(deviceType, deviceName)
            )
            if (response.isSuccessful) {
                val body = response.body()
                val data = body?.data
                if (body?.success == true && data != null) {
                    Resource.success(data.toDomain())
                } else {
                    Resource.error(body?.message ?: "Failed to create QR auth request")
                }
            } else {
                Resource.error(parseErrorMessage(response.errorBody()?.string(), response.code()))
            }
        } catch (e: SocketTimeoutException) {
            Resource.error("Connection timed out. Please try again.")
        } catch (e: IOException) {
            Resource.error("Network error. Check your connection.")
        } catch (e: HttpException) {
            Resource.error("Server error: ${e.code()}", e.code())
        } catch (e: Exception) {
            Resource.error("An unexpected error occurred.")
        }
    }

    override suspend fun getStatus(code: String): Resource<QrAuthStatusResult> {
        return try {
            val response = apiService.getQrAuthStatus(code)
            if (response.isSuccessful) {
                val body = response.body()
                val data = body?.data
                if (body?.success == true && data != null) {
                    Resource.success(data.toDomain())
                } else {
                    Resource.error(body?.message ?: "Failed to get QR auth status")
                }
            } else {
                Resource.error(parseErrorMessage(response.errorBody()?.string(), response.code()))
            }
        } catch (e: SocketTimeoutException) {
            Resource.error("Connection timed out.")
        } catch (e: IOException) {
            Resource.error("Network error. Check your connection.")
        } catch (e: HttpException) {
            Resource.error("Server error: ${e.code()}", e.code())
        } catch (e: Exception) {
            Resource.error("An unexpected error occurred.")
        }
    }

    override suspend fun scan(code: String): Resource<QrAuthScanResult> {
        return try {
            val response = apiService.scanQrAuth(code)
            if (response.isSuccessful) {
                val body = response.body()
                val data = body?.data
                if (body?.success == true && data != null) {
                    Resource.success(data.toDomain())
                } else {
                    Resource.error(body?.message ?: "Failed to scan QR code")
                }
            } else {
                Resource.error(parseErrorMessage(response.errorBody()?.string(), response.code()))
            }
        } catch (e: SocketTimeoutException) {
            Resource.error("Connection timed out.")
        } catch (e: IOException) {
            Resource.error("Network error. Check your connection.")
        } catch (e: HttpException) {
            Resource.error("Server error: ${e.code()}", e.code())
        } catch (e: Exception) {
            Resource.error("An unexpected error occurred.")
        }
    }

    override suspend fun approve(code: String): Resource<AuthResult?> {
        return try {
            val response = apiService.approveQrAuth(code)
            if (response.isSuccessful) {
                val body = response.body()
                val data = body?.data
                if (body?.success == true && data != null) {
                    // Reverse flow (device_type=android): tokens returned directly
                    if (data.accessToken != null && data.refreshToken != null && data.user != null) {
                        val authResult = AuthResult(
                            accessToken = data.accessToken,
                            refreshToken = data.refreshToken,
                            user = data.user.toDomain()
                        )
                        // Persist the new session for the phone
                        userPreferences.saveAuthData(
                            accessToken = authResult.accessToken,
                            refreshToken = authResult.refreshToken,
                            username = authResult.user.username,
                            rememberMe = false
                        )
                        Resource.success(authResult)
                    } else {
                        // Forward flow (device_type=desktop): no tokens for the phone
                        Resource.success(null)
                    }
                } else {
                    Resource.error(body?.message ?: "Failed to approve QR auth")
                }
            } else {
                Resource.error(parseErrorMessage(response.errorBody()?.string(), response.code()))
            }
        } catch (e: SocketTimeoutException) {
            Resource.error("Connection timed out.")
        } catch (e: IOException) {
            Resource.error("Network error. Check your connection.")
        } catch (e: HttpException) {
            Resource.error("Server error: ${e.code()}", e.code())
        } catch (e: Exception) {
            Resource.error("An unexpected error occurred.")
        }
    }

    override suspend fun reject(code: String): Resource<Unit> {
        return try {
            val response = apiService.rejectQrAuth(code)
            if (response.isSuccessful) {
                Resource.success(Unit)
            } else {
                Resource.error(parseErrorMessage(response.errorBody()?.string(), response.code()))
            }
        } catch (e: SocketTimeoutException) {
            Resource.error("Connection timed out.")
        } catch (e: IOException) {
            Resource.error("Network error. Check your connection.")
        } catch (e: HttpException) {
            Resource.error("Server error: ${e.code()}", e.code())
        } catch (e: Exception) {
            Resource.error("An unexpected error occurred.")
        }
    }

    override suspend fun cancel(code: String): Resource<Unit> {
        return try {
            val response = apiService.cancelQrAuth(code)
            if (response.isSuccessful) {
                Resource.success(Unit)
            } else {
                Resource.error(parseErrorMessage(response.errorBody()?.string(), response.code()))
            }
        } catch (e: SocketTimeoutException) {
            Resource.error("Connection timed out.")
        } catch (e: IOException) {
            Resource.error("Network error. Check your connection.")
        } catch (e: HttpException) {
            Resource.error("Server error: ${e.code()}", e.code())
        } catch (e: Exception) {
            Resource.error("An unexpected error occurred.")
        }
    }

    private fun parseErrorMessage(errorBody: String?, code: Int): String {
        return when (code) {
            401 -> "Authentication required"
            403 -> "Account is deactivated"
            404 -> "QR code not found"
            409 -> "This QR code is no longer valid"
            410 -> "This QR code has expired"
            in 500..599 -> "Server error. Please try again later."
            else -> "Request failed. Please try again."
        }
    }
}
