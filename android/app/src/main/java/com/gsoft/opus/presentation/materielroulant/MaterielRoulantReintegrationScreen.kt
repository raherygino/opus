package com.gsoft.opus.presentation.materielroulant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.ui.components.FormSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterielRoulantReintegrationScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: MaterielRoulantReintegrationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réintégration du véhicule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val item = state.item
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Matériel roulant introuvable")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${item.typeMateriel} — ${item.conducteurDisplay.ifBlank { "—" }}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FormSectionCard(title = "Date et heure de retour") {
                OutlinedTextField(
                    value = state.dateReintegration,
                    onValueChange = {},
                    label = { Text("Date de la réintégration *") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = "Choisir la date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.heureReintegration,
                    onValueChange = viewModel::setHeureReintegration,
                    label = { Text("Heure de la réintégration *") },
                    trailingIcon = {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FormSectionCard(title = "Compteurs de retour") {
                OutlinedTextField(
                    value = state.kilometrageRetour,
                    onValueChange = viewModel::setKilometrageRetour,
                    label = { Text("Kilométrage de retour (km) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.niveauCarburantRetour,
                    onValueChange = viewModel::setNiveauCarburantRetour,
                    label = { Text("Carburant de retour (0–100%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FormSectionCard(title = "Observations techniques (optionnel)") {
                OutlinedTextField(
                    value = state.observationsTechniques,
                    onValueChange = viewModel::setObservationsTechniques,
                    label = { Text("Observations générales sur l'état du véhicule") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FormSectionCard(title = "Défaillances signalées (optionnel)") {
                OutlinedTextField(
                    value = state.defaillances,
                    onValueChange = viewModel::setDefaillances,
                    label = { Text("Pannes, dommages ou anomalies constatés") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Réintégrer")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date de la réintégration",
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                viewModel.setDateReintegration(millisToIsoDate(millis))
            }
        )
    }
}
