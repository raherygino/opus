package com.gsoft.opus.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gsoft.opus.core.Constants
import com.gsoft.opus.data.local.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp [Authenticator] that transparently refreshes the JWT access token
 * when the API answers 401 Unauthorized, then retries the original request
 * with the new token.
 *
 * This is critical for background work such as FCM device-token registration:
 * access tokens expire after 15 minutes, so any request made outside a fresh
 * login (app cold start, FCM token rotation via onNewToken) would otherwise
 * fail with 401 and silently leave the device unregistered — meaning the user
 * stops receiving push notifications until their next manual login.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val userPreferences: UserPreferences
) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
        private const val MAX_ATTEMPTS = 3
    }

    // Standalone client for the refresh call — using the main client would
    // recurse into this authenticator on a 401 from the refresh endpoint.
    private val refreshClient = OkHttpClient()
    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath

        // Never attempt a refresh for the auth endpoints themselves.
        if (path.contains("/api/auth/login") || path.contains("/api/auth/refresh")) {
            return null
        }

        // Give up if this request chain already failed repeatedly.
        if (responseCount(response) >= MAX_ATTEMPTS) {
            Log.w(TAG, "Too many 401 retries for $path — giving up")
            return null
        }

        synchronized(this) {
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val currentToken = runBlocking { userPreferences.getAccessToken() }

            // Another thread already refreshed while we were waiting — just
            // retry with the token it stored.
            if (currentToken != null && currentToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val newToken = refreshAccessToken() ?: return null
            Log.d(TAG, "Access token refreshed — retrying $path")
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }
    }

    /**
     * Exchange the stored refresh token for a new access token.
     * Returns the new access token, or null if the refresh failed.
     * A 401/403 from the refresh endpoint means the session is over, so the
     * stored credentials are cleared to force a fresh login.
     */
    private fun refreshAccessToken(): String? {
        val refreshToken = runBlocking { userPreferences.getRefreshToken() } ?: return null

        return try {
            val payload = gson.toJson(mapOf("refresh_token" to refreshToken))
            val request = Request.Builder()
                .url(Constants.BASE_URL + "/api/auth/refresh")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            refreshClient.newCall(request).execute().use { resp ->
                val body = resp.body?.string()
                if (resp.isSuccessful && body != null) {
                    val data = gson.fromJson(body, JsonObject::class.java)
                        ?.getAsJsonObject("data")
                    val token = data?.get("access_token")?.asString
                    if (token != null) {
                        runBlocking { userPreferences.updateAccessToken(token) }
                    }
                    token
                } else {
                    Log.w(TAG, "Token refresh failed: HTTP ${resp.code}")
                    if (resp.code == 401 || resp.code == 403) {
                        runBlocking { userPreferences.clear() }
                    }
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Token refresh exception: ${e.message}")
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
