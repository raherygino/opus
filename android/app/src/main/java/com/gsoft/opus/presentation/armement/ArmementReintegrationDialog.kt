package com.gsoft.opus.presentation.armement

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gsoft.opus.domain.model.Armement
import com.gsoft.opus.domain.repository.ReintegrationData
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated réintégration form — asks the reintegration fields plus the
 * date and GPS coordinates. All perception data is preserved server-side.
 * Shown from both the list and the detail screen while the weapon is
 * still en cours.
 *
 * On Android, GPS coordinates are required — the user must capture their
 * location before the reintegration can be confirmed.
 */
@Composable
fun ArmementReintegrationDialog(
    armement: Armement,
    isSubmitting: Boolean,
    errorMessage: String?,
    onConfirm: (ReintegrationData) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var dateReintegration by remember(armement.id) {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var heure by remember(armement.id) {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.US).format(Date()))
    }
    var etat by remember(armement.id) { mutableStateOf("") }
    var munitionsConsommees by remember(armement.id) { mutableStateOf("") }
    var localError by remember(armement.id) { mutableStateOf<String?>(null) }

    // GPS state
    var latitude by remember(armement.id) { mutableStateOf<Double?>(null) }
    var longitude by remember(armement.id) { mutableStateOf<Double?>(null) }
    var isCapturingLocation by remember(armement.id) { mutableStateOf(false) }
    var locationError by remember(armement.id) { mutableStateOf<String?>(null) }
    var hasPermission by remember(armement.id) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED
                        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermission = granted
        if (granted) {
            captureLocation(context) { lat, lon, err ->
                if (err != null) {
                    locationError = err
                    isCapturingLocation = false
                } else {
                    latitude = lat
                    longitude = lon
                    locationError = null
                    isCapturingLocation = false
                }
            }
        } else {
            locationError = "Autorisation de localisation requise"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Column {
                Text("Réintégration de l'arme", fontWeight = FontWeight.Bold)
                Text(
                    text = armement.armeDisplay +
                        if (armement.agentPreneurDisplay.isNotBlank()) " · ${armement.agentPreneurDisplay}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dateReintegration,
                    onValueChange = { dateReintegration = it },
                    label = { Text("Date de la réintégration *") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = heure,
                    onValueChange = { heure = it },
                    label = { Text("Heure de la réintégration *") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = etat,
                    onValueChange = { etat = it },
                    label = { Text("État à la réintégration *") },
                    placeholder = { Text("État de l'arme au retour...") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = munitionsConsommees,
                    onValueChange = { munitionsConsommees = it.filter(Char::isDigit) },
                    label = {
                        Text(
                            "Munitions consommées *" +
                                (armement.munitions?.let { " (perçues : $it)" } ?: "")
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // GPS capture section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = " Localisation de la réintégration",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (latitude != null && longitude != null) {
                            Text(
                                text = "Lat: %.6f, Lon: %.6f".format(latitude, longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Position requise pour la réintégration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (locationError != null) {
                            Text(
                                text = locationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (hasPermission) {
                                    isCapturingLocation = true
                                    locationError = null
                                    captureLocation(context) { lat, lon, err ->
                                        if (err != null) {
                                            locationError = err
                                            isCapturingLocation = false
                                        } else {
                                            latitude = lat
                                            longitude = lon
                                            locationError = null
                                            isCapturingLocation = false
                                        }
                                    }
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !isCapturingLocation && !isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isCapturingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text("  Capture...")
                            } else {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("  Capturer ma position")
                            }
                        }
                    }
                }

                val error = localError ?: errorMessage
                if (error != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validationError = ArmementFormValidator.validateReintegration(
                        heureReintegration = heure,
                        etatReintegration = etat,
                        munitionsConsommees = munitionsConsommees,
                        munitionsPercues = armement.munitions
                    )
                    if (validationError != null) {
                        localError = validationError
                        return@Button
                    }
                    if (dateReintegration.isBlank()) {
                        localError = "La date de la réintégration est requise"
                        return@Button
                    }
                    // GPS coordinates are required on Android
                    if (latitude == null || longitude == null) {
                        localError = "La capture de la position GPS est requise pour la réintégration"
                        return@Button
                    }
                    localError = null
                    onConfirm(
                        ReintegrationData(
                            heureReintegration = heure,
                            dateReintegration = dateReintegration,
                            etatReintegration = etat,
                            munitionsConsommees = munitionsConsommees.trim().toInt(),
                            reintegrationLatitude = latitude,
                            reintegrationLongitude = longitude
                        )
                    )
                },
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Réintégrer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Capture the current device location using the FusedLocationProvider.
 * Calls [onResult] with the latitude/longitude on success, or an error
 * message on failure.
 */
@SuppressLint("MissingPermission")
private fun captureLocation(
    context: Context,
    onResult: (Double?, Double?, String?) -> Unit
) {
    // Check location services
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
        !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        onResult(null, null, "Activez les services de localisation (GPS) sur votre appareil")
        return
    }

    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) {
                onResult(location.latitude, location.longitude, null)
            } else {
                onResult(null, null, "Impossible d'obtenir la position. Réessayez.")
            }
        }
        .addOnFailureListener { e ->
            onResult(null, null, "Erreur de localisation: ${e.message ?: "inconnue"}")
        }
}
