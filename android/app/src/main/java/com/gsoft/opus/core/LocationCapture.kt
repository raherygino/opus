package com.gsoft.opus.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Result of a location capture attempt.
 */
sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult()
    data class Error(val message: String) : LocationResult()
}

/**
 * Location capture utility that works on both GMS and non-GMS devices.
 *
 * - If Google Play Services is available, uses [FusedLocationProviderClient]
 *   for the best accuracy.
 * - If Play Services is unavailable (e.g. Huawei devices, custom ROMs,
 *   emulators without GMS), falls back to the Android framework
 *   [LocationManager] API.
 *
 * Usage:
 *   when (val result = LocationCapture.capture(context)) {
 *       is LocationResult.Success -> // use result.latitude, result.longitude
 *       is LocationResult.Error -> // show result.message
 *   }
 */
object LocationCapture {

    /**
     * Check if the app has location permission.
     */
    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if any location provider is enabled (GPS or network).
     */
    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    /**
     * Check if Google Play Services is available on this device.
     */
    private fun isGmsAvailable(context: Context): Boolean {
        return try {
            val status = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
            status == ConnectionResult.SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Capture the current location. Tries FusedLocationProvider first,
     * falls back to LocationManager if Play Services is unavailable.
     * Must be called from a coroutine.
     */
    suspend fun capture(context: Context): LocationResult {
        if (!hasPermission(context)) {
            return LocationResult.Error("Autorisation de localisation requise")
        }
        if (!isLocationEnabled(context)) {
            return LocationResult.Error("Activez les services de localisation (GPS) sur votre appareil")
        }

        // Try FusedLocationProvider first (best accuracy, lower battery)
        if (isGmsAvailable(context)) {
            return try {
                val location = LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .await()
                if (location != null) {
                    LocationResult.Success(location.latitude, location.longitude)
                } else {
                    // Fused returned null — try LocationManager as a secondary fallback
                    captureViaLocationManager(context)
                }
            } catch (e: Exception) {
                // Play Services threw — fall back to LocationManager
                captureViaLocationManager(context)
            }
        }

        // No Play Services — use the framework LocationManager
        return captureViaLocationManager(context)
    }

    /**
     * Fallback: use the Android framework [LocationManager] to get the
     * current location. Tries getLastKnownLocation first (instant), then
     * requests a single update if the last known location is stale or null.
     */
    @SuppressLint("MissingPermission")
    private suspend fun captureViaLocationManager(context: Context): LocationResult {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Try to get the last known location from any provider (fast path)
        val providers = lm.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = lm.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }

        // If we have a recent last known location (within 30 seconds), use it
        if (bestLocation != null &&
            System.currentTimeMillis() - bestLocation.time < 30_000) {
            return LocationResult.Success(bestLocation.latitude, bestLocation.longitude)
        }

        // Otherwise request a single fresh location update
        return requestSingleUpdate(context, lm)
    }

    /**
     * Request a single location update from the best available provider.
     * Times out after 15 seconds.
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(context: Context, lm: LocationManager): LocationResult {
        // Prefer GPS for accuracy, fall back to network
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return LocationResult.Error("Aucun fournisseur de localisation disponible")
        }

        return suspendCancellableCoroutine { cont ->
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) {
                        cont.resume(LocationResult.Success(location.latitude, location.longitude))
                    }
                }

                // Required overrides for older API levels
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                @Suppress("DEPRECATION")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    lm.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                        if (location != null) {
                            if (cont.isActive) {
                                cont.resume(LocationResult.Success(location.latitude, location.longitude))
                            }
                        } else {
                            if (cont.isActive) {
                                cont.resume(LocationResult.Error("Impossible d'obtenir la position. Réessayez."))
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }
            } catch (e: SecurityException) {
                if (cont.isActive) {
                    cont.resume(LocationResult.Error("Autorisation de localisation refusée"))
                }
            }

            // Timeout: 15 seconds
            cont.invokeOnCancellation {
                try {
                    @Suppress("DEPRECATION")
                    lm.removeUpdates(listener)
                } catch (_: Exception) {}
            }
        }
    }
}

/**
 * Await a Google Play Services Task result, suspending the coroutine
 * until it completes. Used by [LocationCapture] for the FusedLocation
 * path. On non-GMS devices this is never called (we check availability
 * first), so the GMS dependency is only loaded when needed.
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? {
    return suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { e ->
            if (cont.isActive) {
                // Rethrow so the caller's try/catch can fall back to LocationManager
                cont.resume(null)
            }
        }
    }
}
