package com.gsoft.opus.notifications

import android.content.Context
import android.os.Build
import android.util.Log
import com.gsoft.opus.data.local.UserPreferences
import com.gsoft.opus.domain.repository.DeviceTokenRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the FCM registration token lifecycle:
 *  - Retrieves the current FCM token from Firebase.
 *  - Registers it with the backend (only when the user is logged in).
 *  - Retries registration on failure with exponential backoff.
 *  - Unregisters all device tokens on logout.
 *
 * The token can change over time, so [OpusMessagingService.onNewToken] delegates
 * here to keep the backend in sync.
 */
@Singleton
class FcmTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val TAG = "FcmTokenManager"
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 2000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fetch the FCM token from Firebase and register it with the backend
     * if the user is currently logged in. Safe to call on app startup.
     *
     * Registration is retried with exponential backoff if it fails.
     */
    fun fetchAndRegisterToken() {
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM token retrieved: ${token.take(12)}…")
                registerIfLoggedIn(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch FCM token", e)
            }
        }
    }

    /**
     * Register a known FCM token with the backend if the user is logged in.
     * Called from [OpusMessagingService.onNewToken] and internally by
     * [fetchAndRegisterToken].
     *
     * If registration fails, retries with exponential backoff up to [MAX_RETRIES]
     * times. This ensures transient network errors don't permanently prevent the
     * token from being registered.
     */
    fun registerIfLoggedIn(token: String) {
        scope.launch {
            val loggedIn = userPreferences.isLoggedIn.first()
            if (!loggedIn) {
                Log.d(TAG, "User not logged in — skipping token registration")
                return@launch
            }

            val deviceName = getDeviceName()
            var backoff = INITIAL_BACKOFF_MS

            for (attempt in 1..MAX_RETRIES) {
                val success = deviceTokenRepository.registerToken(token, deviceName)
                if (success) {
                    Log.d(TAG, "FCM token registered with backend (attempt $attempt)")
                    return@launch
                }
                Log.w(TAG, "Failed to register FCM token (attempt $attempt/$MAX_RETRIES)")
                if (attempt < MAX_RETRIES) {
                    delay(backoff)
                    backoff *= 2
                }
            }
            Log.e(TAG, "Failed to register FCM token after $MAX_RETRIES attempts")
        }
    }

    /**
     * Unregister all device tokens for the current user on the backend.
     * Called on logout.
     */
    fun unregisterAll() {
        scope.launch {
            val success = deviceTokenRepository.unregisterAll()
            Log.d(TAG, "Unregister all devices: success=$success")
        }
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER ?: ""
        val model = Build.MODEL ?: ""
        return "$manufacturer $model".trim().ifEmpty { "Android Device" }
    }
}
