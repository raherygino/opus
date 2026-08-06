package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.DeviceTokenRequestDto
import com.gsoft.opus.domain.repository.DeviceTokenRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceTokenRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : DeviceTokenRepository {

    companion object {
        private const val TAG = "DeviceTokenRepo"
    }

    override suspend fun registerToken(token: String, deviceName: String?): Boolean {
        return try {
            Log.d(TAG, "Registering FCM token with backend: ${token.take(12)}…")
            val response = apiService.registerDeviceToken(
                DeviceTokenRequestDto(token = token, deviceName = deviceName)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Token registered successfully (id=${response.body()?.data?.id})")
                true
            } else {
                val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                Log.e(TAG, "Token registration failed: HTTP ${response.code()} — $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token registration exception: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }

    override suspend fun unregisterToken(token: String): Boolean {
        return try {
            val response = apiService.unregisterDeviceToken(
                DeviceTokenRequestDto(token = token)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                true
            } else {
                val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                Log.w(TAG, "Token unregister failed: HTTP ${response.code()} — $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Token unregister exception: ${e.message}")
            false
        }
    }

    override suspend fun unregisterAll(): Boolean {
        return try {
            val response = apiService.unregisterAllDevices()
            if (response.isSuccessful && response.body()?.success == true) {
                true
            } else {
                val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                Log.w(TAG, "Unregister all failed: HTTP ${response.code()} — $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unregister all exception: ${e.message}")
            false
        }
    }
}
