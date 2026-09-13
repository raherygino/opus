package com.gsoft.opus.presentation.materiel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.TaskAlt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.ui.components.FormSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterielReintegrationScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: MaterielReintegrationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Réintégration du matériel", fontWeight = FontWeight.Bold) },
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

        val aff = state.affectation
        if (aff == null) {
            Text(
                text = state.errorMessage ?: "Affectation introuvable",
                modifier = Modifier.padding(16.dp)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Info card
            FormSectionCard(
                title = "Réintégration",
                icon = Icons.Outlined.TaskAlt,
                subtitle = "${aff.agentDisplay.ifBlank { "—" }} — ${aff.lignes.size} matériel(s)"
            ) {
                OutlinedTextField(
                    value = if (state.dateReintegration.isBlank()) "" else formatDateDisplay(state.dateReintegration),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de la réintégration *") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la date")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.heureReintegration,
                    onValueChange = viewModel::setHeureReintegration,
                    label = { Text("Heure de la réintégration *") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Per-line etat_reintegration
            FormSectionCard(
                title = "État à la réintégration (par matériel)",
                icon = Icons.Outlined.TaskAlt,
                subtitle = "Indiquer l'état de chaque matériel au retour"
            ) {
                aff.lignes.forEach { ligne ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = ligne.typeMaterielNom,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.ligneEtats[ligne.id] ?: "",
                            onValueChange = { viewModel.setLigneEtat(ligne.id, it) },
                            label = { Text("État à la réintégration *") },
                            placeholder = { Text("Ex : Bon, Endommagé...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Réintégrer")
            }
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
