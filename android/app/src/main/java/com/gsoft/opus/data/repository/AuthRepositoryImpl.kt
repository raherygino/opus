package com.gsoft.opus.data.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.LoginRequestDto
import com.gsoft.opus.data.api.dto.RefreshTokenRequestDto
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.data.local.UserPreferences
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.User
import com.gsoft.opus.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) : AuthRepository {

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
            val token = userPreferences.getAccessToken()
                ?: return Resource.error("Not authenticated", 401)
            val response = apiService.getCurrentUser("Bearer $token")
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
        userPreferences.clear()
    }

    override fun getSavedUsername(): Flow<String> {
        return userPreferences.savedUsername
    }

    override fun getRememberMe(): Flow<Boolean> {
        return userPreferences.rememberMe
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
