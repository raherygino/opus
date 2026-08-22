package com.gsoft.opus.presentation.personnel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Report
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton
import com.gsoft.opus.ui.components.OpusDropdown

private val COMPORTEMENT_TYPES = listOf("Positive", "Negative")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComportementFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ComportementFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            state.successMessage?.let { snackbarHostState.showSnackbar(it) }
            onSaved()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            if (!state.saved) snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nouveau comportement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─── Personnel selection ──────────────────────────────────────
            FormSectionCard(
                title = "Personnel concerné",
                icon = Icons.Outlined.PersonSearch,
                subtitle = "Recherchez la personne concernée"
            ) {
                OutlinedTextField(
                    value = state.searchName,
                    onValueChange = viewModel::onSearchNameChange,
                    label = { Text("Rechercher un personnel") },
                    placeholder = { Text("Nom, prénoms ou IM...") },
                    leadingIcon = { Icon(Icons.Outlined.PersonSearch, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.autocomplete.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        state.autocomplete.take(5).forEach { person ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectPersonnel(person) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${person.firstname} ${person.lastname}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "IM: ${person.im}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (state.personnelId != 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.PersonSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sélectionné: ${state.prenoms} ${state.nom}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ─── Détails du comportement ──────────────────────────────────
            FormSectionCard(
                title = "Détails du comportement",
                icon = Icons.Outlined.Report,
                subtitle = "Type et date"
            ) {
                OpusDropdown(
                    label = "Type *",
                    options = COMPORTEMENT_TYPES,
                    selected = state.type,
                    onSelect = viewModel::onTypeChange,
                    isError = state.type.isBlank() && state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.dateComportement,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date *") },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la date")
                        }
                    },
                    singleLine = true,
                    isError = state.dateComportement.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Motif & décision ─────────────────────────────────────────
            FormSectionCard(
                title = "Motif & décision",
                icon = Icons.Outlined.Gavel,
                subtitle = "Description et décision de la hiérarchie"
            ) {
                OutlinedTextField(
                    value = state.motif,
                    onValueChange = viewModel::onMotifChange,
                    label = { Text("Motif *") },
                    placeholder = { Text("Décrire le motif...") },
                    isError = state.motif.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.decision,
                    onValueChange = viewModel::onDecisionChange,
                    label = { Text("Décision de la hiérarchie") },
                    placeholder = { Text("Décision prise par la hiérarchie...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.errorMessage != null && !state.saved) {
                ErrorMessage(message = state.errorMessage!!)
            }

            Spacer(modifier = Modifier.height(4.dp))

            GradientButton(
                text = "Enregistrer",
                onClick = { viewModel.save() },
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date du comportement",
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                viewModel.onDateChange(millisToIsoDate(millis))
            }
        )
    }
}
