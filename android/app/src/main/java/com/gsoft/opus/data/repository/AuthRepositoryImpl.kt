package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.ChangePasswordRequest
import com.gsoft.opus.data.api.dto.LoginRequestDto
import com.gsoft.opus.data.api.dto.RefreshTokenRequestDto
import com.gsoft.opus.data.api.dto.UpdateProfileRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.data.local.UserPreferences
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.User
import com.gsoft.opus.domain.repository.AuthRepository
import com.gsoft.opus.domain.repository.DeviceTokenRepository
import com.gsoft.opus.domain.repository.UploadFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences,
    private val deviceTokenRepository: DeviceTokenRepository
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
    }

    override suspend fun login(username: String, password: String, rememberMe: Boolean): Resource<AuthResult> {
        return try {
            val response = apiService.login(LoginRequestDto(username, password))
            if (response.isSuccessful) {
                val body = response.body()
                val data = body?.data
                if (body?.success == true && data != null) {
                    userPreferences.saveAuthData(
                        accessToken = data.accessToken,
                        refreshToken = data.refreshToken,
                        username = username,
                        rememberMe = rememberMe
                    )
                    Resource.success(data.toDomain())
                } else {
                    Resource.error(body?.message ?: "Login failed")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val message = parseErrorMessage(errorBody, response.code())
                Resource.error(message, response.code())
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

    override suspend fun refreshToken(): Resource<String> {
        return try {
            val refreshToken = userPreferences.getRefreshToken()
                ?: return Resource.error("No refresh token available", 401)
            val response = apiService.refreshToken(RefreshTokenRequestDto(refreshToken))
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    userPreferences.updateAccessToken(data.accessToken)
                    // Save the rotated refresh token so the session stays alive.
                    data.refreshToken?.let { userPreferences.updateRefreshToken(it) }
                    Resource.success(data.accessToken)
                } else {
                    Resource.error("Failed to refresh token")
                }
            } else {
                userPreferences.clear()
                Resource.error("Session expired", response.code())
            }
        } catch (e: Exception) {
            userPreferences.clear()
            Resource.error("Session expired")
        }
    }

    override suspend fun getCurrentUser(): Resource<User> {
        return try {
            // The header is added by AuthInterceptor — do NOT also pass it via
            // @Header here: two Authorization headers make nginx reject the
            // request with HTTP 400 before it ever reaches the API.
            userPreferences.getAccessToken()
                ?: return Resource.error("Not authenticated", 401)
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    Resource.success(data.toDomain())
                } else {
                    Resource.error("Failed to get user data")
                }
            } else if (response.code() == 401) {
                val refreshResult = refreshToken()
                if (refreshResult is Resource.Success) {
                    getCurrentUser()
                } else {
                    userPreferences.clear()
                    Resource.error("Session expired", 401)
                }
            } else if (response.code() == 404) {
                // The account referenced by the token no longer exists
                // server-side — clear local auth so the app logs out.
                logout()
                Resource.error("User not found", 404)
            } else {
                Resource.error("Failed to get user data", response.code())
            }
        } catch (e: Exception) {
            Resource.error("Network error. Check your connection.")
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return userPreferences.isLoggedIn.first()
    }

    override suspend fun logout() {
        // Unregister all device tokens for this user before clearing local auth,
        // so the backend stops sending push notifications to this device.
        runCatching { deviceTokenRepository.unregisterAll() }
        userPreferences.clear()
    }

    override fun getSavedUsername(): Flow<String> {
        return userPreferences.savedUsername
    }

    override fun getRememberMe(): Flow<Boolean> {
        return userPreferences.rememberMe
    }

    // ─── Self-service profile ────────────────────────────────────────

    override suspend fun updateProfile(
        lastname: String,
        firstname: String,
        phone: String?,
        email: String?,
        address: String?
    ): Resource<User> {
        return try {
            apiService.updateProfile(
                UpdateProfileRequest(
                    lastname = lastname,
                    firstname = firstname,
                    phone = phone,
                    email = email,
                    address = address
                )
            ).extract("Impossible de mettre à jour le profil").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateProfile"))
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Resource<Unit> {
        return try {
            val response = apiService.changePassword(ChangePasswordRequest(currentPassword, newPassword))
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.success(Unit)
            } else {
                val errors = response.body()?.errors?.entries
                    ?.joinToString("\n") { it.value }
                Resource.error(
                    errors ?: response.body()?.message ?: "Impossible de changer le mot de passe",
                    response.code()
                )
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "changePassword"))
        }
    }

    override suspend fun uploadProfilePhoto(photo: UploadFile): Resource<User> {
        return try {
            val photoPart = MultipartBody.Part.createFormData(
                "photo", photo.fileName,
                photo.bytes.toRequestBody(photo.mimeType?.toMediaTypeOrNull())
            )
            apiService.uploadProfilePhoto(photoPart)
                .extract("Impossible d'enregistrer la photo").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "uploadProfilePhoto"))
        }
    }

    override suspend fun deleteProfilePhoto(): Resource<User> {
        return try {
            apiService.deleteProfilePhoto()
                .extract("Impossible de supprimer la photo").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteProfilePhoto"))
        }
    }

    private fun <T> retrofit2.Response<com.gsoft.opus.data.api.dto.ApiResponse<T>>.extract(defaultError: String): Resource<T> {
        return if (isSuccessful && body()?.success == true) {
            val data = body()!!.data
            if (data != null) Resource.success(data)
            else Resource.error(body()?.message ?: defaultError, code())
        } else {
            val errors = body()?.errors?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }
            Resource.error(errors ?: body()?.message ?: defaultError, code())
        }
    }

    private fun failureMessage(e: Exception, what: String): String {
        return if (e is IOException || e is SocketTimeoutException) {
            "Erreur réseau. Vérifiez votre connexion."
        } else {
            Log.e(TAG, "$what failed", e)
            "Une erreur inattendue s'est produite."
        }
    }

    private fun parseErrorMessage(errorBody: String?, code: Int): String {
        return when (code) {
            401 -> "Invalid username or password"
            403 -> "Account is deactivated"
            422 -> "Please check your input"
            in 500..599 -> "Server error. Please try again later."
            else -> "Login failed. Please try again."
        }
    }
}
