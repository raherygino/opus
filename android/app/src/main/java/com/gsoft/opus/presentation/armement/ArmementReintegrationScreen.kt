package com.gsoft.opus.presentation.armement

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.OpusTimePickerDialog
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton

/**
 * Full-screen reintegration form. Replaces the old dialog-based approach
 * so we can use native date and time pickers. Navigated to via:
 *   MainRoutes.ArmementReintegration.createRoute(armementId)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmementReintegrationScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ArmementReintegrationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success message → pop back
    LaunchedEffect(state.userMessage) {
        if (state.userMessage != null) {
            snackbarHostState.showSnackbar(state.userMessage!!)
            onSaved()
        }
    }
    // Show error message
    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            snackbarHostState.showSnackbar(state.errorMessage!!)
            viewModel.dismissMessage()
        }
    }

    // Date / time picker visibility
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted)
        if (granted) viewModel.captureLocation()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Réintégration de l'arme", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val armement = state.armement ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Weapon info card
            FormSectionCard(
                title = "Arme concernée",
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                subtitle = armement.armeDisplay
            ) {
                Text(
                    text = "Agent: ${armement.agentPreneurDisplay.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Perçue le ${formatDateDisplay(armement.datePerception)} à ${armement.heurePerceptionDisplay}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reintegration fields
            FormSectionCard(
                title = "Informations de réintégration",
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                subtitle = "Date, heure et état de l'arme au retour"
            ) {
                // Date picker field
                OutlinedTextField(
                    value = if (state.dateReintegration.isBlank()) "" else formatDateDisplay(state.dateReintegration),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de la réintégration *") },
                    placeholder = { Text("JJ/MM/AAAA") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la date")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Time picker field
                OutlinedTextField(
                    value = state.heureReintegration,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Heure de la réintégration *") },
                    placeholder = { Text("HH:MM") },
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Outlined.Schedule, contentDescription = "Choisir l'heure")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.etatReintegration,
                    onValueChange = viewModel::updateEtatReintegration,
                    label = { Text("État à la réintégration *") },
                    placeholder = { Text("État de l'arme au retour...") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.munitionsConsommees,
                    onValueChange = viewModel::updateMunitionsConsommees,
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
            }

            // GPS capture section
            FormSectionCard(
                title = "Localisation de la réintégration",
                icon = Icons.Outlined.LocationOn,
                subtitle = "Position GPS requise (mobile)"
            ) {
                if (state.latitude != null && state.longitude != null) {
                    Text(
                        text = "Lat: %.6f, Lon: %.6f".format(state.latitude, state.longitude),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Position requise pour la réintégration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.locationError != null) {
                    Text(
                        text = state.locationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (state.hasLocationPermission) {
                                viewModel.captureLocation()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = !state.isCapturingLocation && !state.isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isCapturingLocation) {
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
                    if (state.latitude != null) {
                        OutlinedButton(
                            onClick = viewModel::clearLocation,
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Effacer")
                        }
                    }
                }
            }

            // Submit button
            GradientButton(
                text = "Réintégrer",
                onClick = viewModel::submit,
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date de la réintégration",
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                viewModel.updateDateReintegration(millisToIsoDate(millis))
            }
        )
    }

    // Time picker dialog
    if (showTimePicker) {
        val parts = state.heureReintegration.split(":")
        val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        OpusTimePickerDialog(
            visible = true,
            initialHour = initHour,
            initialMinute = initMinute,
            title = "Heure de la réintégration",
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                showTimePicker = false
                viewModel.updateHeureReintegration("%02d:%02d".format(hour, minute))
            }
        )
    }
}
