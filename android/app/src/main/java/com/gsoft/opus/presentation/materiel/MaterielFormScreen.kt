package com.gsoft.opus.presentation.materiel

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.presentation.armement.SignatureCaptureDialog
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton
import com.gsoft.opus.ui.components.OpusDropdown
import com.gsoft.opus.ui.components.SvgSignatureView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterielFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: MaterielFormViewModel = hiltViewModel()
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
                title = {
                    Text(
                        if (state.isEdit) "Modifier l'affectation" else "Nouvelle affectation",
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
            // ─── Agent section (picker + verification + signature) ───
            FormSectionCard(
                title = "Agent utilisateur",
                icon = Icons.Outlined.Person,
                subtitle = "Agent qui reçoit le matériel"
            ) {
                val personnelOptions = state.personnelOptions
                val selectedLabel = personnelOptions
                    .firstOrNull { it.id == state.agentPersonnelId }
                    ?.let { "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}" }
                    ?: ""
                OpusDropdown(
                    label = "Agent *",
                    options = personnelOptions.map {
                        "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}"
                    },
                    selected = selectedLabel,
                    onSelect = { label ->
                        val idx = personnelOptions.indexOfFirst {
                            "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}" == label
                        }
                        if (idx >= 0) viewModel.setAgentPersonnelId(personnelOptions[idx].id)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Verification status (edit mode) — read-only.
                if (state.isEdit) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.verified) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "  Identité vérifiée au moment de la perception",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = "Identité non vérifiée (enregistrée avant la fonctionnalité de vérification)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Verification step (create mode only).
                if (!state.isEdit) {
                    Text(
                        text = "Vérification de l'identité",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "L'agent doit fournir son code secret pour confirmer son identité avant la remise du matériel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = state.codeSecret,
                        onValueChange = viewModel::setCodeSecret,
                        label = { Text("Code secret de l'agent") },
                        singleLine = true,
                        enabled = !state.verified && state.agentPersonnelId > 0,
                        leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                    if (state.verified) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " Vérifié",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        GradientButton(
                            text = "Vérifier",
                            onClick = { viewModel.verifyCode() },
                            isLoading = state.verifying,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                    if (state.verifyError != null) {
                        Text(
                            text = state.verifyError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Signature capture (after verification, optional).
                    if (state.verified) {
                        var showSignatureDialog by remember { mutableStateOf(false) }
                        Text(
                            text = "Signature de l'agent (optionnel)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = when {
                                state.signatureFromPersonnel -> "Signature récupérée depuis les données du personnel. Vous pouvez la garder ou en dessiner une nouvelle."
                                state.signatureSvg != null -> "Signature dessinée pour cette affectation."
                                else -> "Aucune signature dans les données du personnel. Vous pouvez en capturer une ou laisser vide."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.signatureSvg != null) {
                            val svg = state.signatureSvg!!
                            SvgSignatureView(
                                svg = svg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .padding(top = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { showSignatureDialog = true }) {
                                    Icon(Icons.Outlined.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("  Refaire")
                                }
                                IconButton(onClick = { viewModel.setSignatureSvg(null) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showSignatureDialog = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Icon(Icons.Outlined.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("  Capturer la signature")
                            }
                        }

                        if (showSignatureDialog) {
                            SignatureCaptureDialog(
                                onConfirm = { svg ->
                                    viewModel.setSignatureSvg(svg)
                                    showSignatureDialog = false
                                },
                                onDismiss = { showSignatureDialog = false }
                            )
                        }
                    }
                }
            }

            // ─── Perception section ─────────────────────────────────
            FormSectionCard(
                title = "Perception",
                icon = Icons.Outlined.CalendarMonth,
                subtitle = "Date et heure de l'affectation"
            ) {
                OutlinedTextField(
                    value = if (state.datePerception.isBlank()) "" else formatDateDisplay(state.datePerception),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de la perception *") },
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

                OutlinedTextField(
                    value = state.heurePerception,
                    onValueChange = viewModel::setHeurePerception,
                    label = { Text("Heure de la perception *") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Matériels section (multi-select lines) ─────────────
            FormSectionCard(
                title = "Matériels (${state.lignes.size})",
                icon = Icons.Outlined.Inventory,
                subtitle = "Ajouter un ou plusieurs matériels à affecter"
            ) {
                state.lignes.forEachIndexed { index, ligne ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Matériel #${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (state.lignes.size > 1) {
                                IconButton(onClick = { viewModel.removeLigne(index) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Retirer",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(0.dp)
                                    )
                                }
                            }
                        }

                        val typeOptions = state.typeOptions
                        val selectedTypeLabel = typeOptions
                            .firstOrNull { it.id == ligne.typeMaterielId }
                            ?.nom
                            ?: ""
                        OpusDropdown(
                            label = "Type de matériel *",
                            options = typeOptions.map { it.nom },
                            selected = selectedTypeLabel,
                            onSelect = { label ->
                                val idx = typeOptions.indexOfFirst { it.nom == label }
                                if (idx >= 0) {
                                    viewModel.updateLigne(index, ligne.copy(typeMaterielId = typeOptions[idx].id))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = ligne.numeroMateriel,
                            onValueChange = { viewModel.updateLigne(index, ligne.copy(numeroMateriel = it)) },
                            label = { Text("ID Matériel *") },
                            placeholder = { Text("Ex : R-001") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = ligne.etatEmport,
                            onValueChange = { viewModel.updateLigne(index, ligne.copy(etatEmport = it)) },
                            label = { Text("État à l'emport (optionnel)") },
                            placeholder = { Text("Ex : Bon, Neuf, Moyen...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (state.typeOptions.isEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aucun type de matériel enregistré. Contactez un administrateur pour en créer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = viewModel::addLigne,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.typeOptions.isNotEmpty()
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Ajouter un matériel")
                }
            }

            // ─── Observations section ───────────────────────────────
            FormSectionCard(
                title = "Observations",
                icon = Icons.Outlined.Inventory,
                subtitle = "Notes optionnelles"
            ) {
                OutlinedTextField(
                    value = state.observations,
                    onValueChange = viewModel::setObservations,
                    label = { Text("Observations (optionnel)") },
                    placeholder = { Text("Observations...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Save button ────────────────────────────────────────
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
                Text(if (state.isEdit) "Mettre à jour" else "Enregistrer")
            }
        }
    }

    if (showDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date de la perception",
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                viewModel.setDatePerception(millisToIsoDate(millis))
            }
        )
    }
}
