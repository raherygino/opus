package com.gsoft.opus.presentation.dispositif

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
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gsoft.opus.presentation.personnel.OpusDateRangePickerDialog
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.isoDateToMillis
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispositifFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: DispositifFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRangePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEdit) "Modifier le dispositif" else "Nouveau dispositif",
                        fontWeight = FontWeight.Bold
                    )
                },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─── Informations générales ──
            FormSectionCard(
                title = "Informations générales",
                icon = Icons.Outlined.Shield,
                subtitle = "Nature de l'évènement et période du dispositif"
            ) {
                OutlinedTextField(
                    value = state.natureEvenement,
                    onValueChange = viewModel::updateNatureEvenement,
                    label = { Text("Nature de l'évènement *") },
                    placeholder = { Text("Ex : Visite VIP, manifestation, match de football") },
                    singleLine = true,
                    isError = state.natureEvenement.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Période — date-range picker (début → fin)
                OutlinedTextField(
                    value = if (state.dateDebut.isBlank() && state.dateFin.isBlank()) ""
                    else "${formatDateDisplay(state.dateDebut)} → ${formatDateDisplay(state.dateFin)}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Période *") },
                    placeholder = { Text("JJ/MM/AAAA → JJ/MM/AAAA") },
                    trailingIcon = {
                        IconButton(onClick = { showRangePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la période")
                        }
                    },
                    singleLine = true,
                    isError = (state.dateDebut.isBlank() || state.dateFin.isBlank()) && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Effectif engagé ──
            FormSectionCard(
                title = "Effectif engagé",
                icon = Icons.Outlined.ViewColumn,
                subtitle = "Répartition des effectifs par secteur"
            ) {
                EffectifSection(
                    rows = state.effectifs,
                    invalidKeys = state.invalidSecteurKeys,
                    onUpdate = viewModel::updateEffectif,
                    onAdd = viewModel::addEffectif,
                    onRemove = viewModel::removeEffectif
                )
            }

            // ─── Error + submit ─────────────────────────────────────
            if (state.errorMessage != null) {
                ErrorMessage(message = state.errorMessage!!)
            }

            Spacer(modifier = Modifier.height(4.dp))

            GradientButton(
                text = if (state.isEdit) "Mettre à jour" else "Enregistrer",
                onClick = { viewModel.save() },
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showRangePicker) {
        OpusDateRangePickerDialog(
            visible = true,
            initialStartMillis = isoDateToMillis(state.dateDebut),
            initialEndMillis = isoDateToMillis(state.dateFin),
            title = "Période du dispositif",
            onDismiss = { showRangePicker = false },
            onConfirm = { startMillis, endMillis ->
                showRangePicker = false
                viewModel.updatePeriode(
                    millisToIsoDate(startMillis),
                    millisToIsoDate(endMillis)
                )
            }
        )
    }
}
