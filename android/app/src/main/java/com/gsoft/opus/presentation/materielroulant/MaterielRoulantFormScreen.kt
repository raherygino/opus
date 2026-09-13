package com.gsoft.opus.presentation.materielroulant

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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Schedule
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
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton
import com.gsoft.opus.ui.components.OpusDropdown
import com.gsoft.opus.ui.components.SvgSignatureView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterielRoulantFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: MaterielRoulantFormViewModel = hiltViewModel()
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
                title = { Text(if (state.isEdit) "Modifier la perception" else "Nouvelle perception") },
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

        val personnelOptions = state.personnelOptions
        val personnelLabel = { id: Int ->
            personnelOptions.firstOrNull { it.id == id }
                ?.let { "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}" }
                ?: ""
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
            // Véhicule
            FormSectionCard(title = "Véhicule", icon = Icons.Outlined.DirectionsCar) {
                OpusDropdown(
                    label = "Type de véhicule *",
                    options = listOf("VHL", "Moto"),
                    selected = state.typeMateriel,
                    onSelect = viewModel::setTypeMateriel,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.numeroImmatriculation,
                    onValueChange = viewModel::setNumeroImmatriculation,
                    label = { Text("Numéro d'immatriculation") },
                    placeholder = { Text("Ex. 1234 AB 75") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.descriptionVehicule,
                    onValueChange = viewModel::setDescriptionVehicule,
                    label = { Text("Description du véhicule") },
                    placeholder = { Text("Ex. S.U.V 4x4, Berline, Nissan, Toyota...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Équipage
            FormSectionCard(title = "Équipage", icon = Icons.Outlined.DirectionsCar) {
                OpusDropdown(
                    label = "Agent conducteur *",
                    options = personnelOptions.map {
                        "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}"
                    },
                    selected = personnelLabel(state.agentConducteurPersonnelId),
                    onSelect = { label ->
                        val idx = personnelOptions.indexOfFirst {
                            "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}" == label
                        }
                        if (idx >= 0) viewModel.setAgentConducteur(personnelOptions[idx].id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Sélectionner un agent conducteur"
                )

                // Verification status (edit mode) — read-only.
                if (state.isEdit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
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
                        text = "Vérification de l'identité du conducteur",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "Le conducteur doit fournir son code secret pour confirmer son identité avant la remise du véhicule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = state.codeSecret,
                        onValueChange = viewModel::setCodeSecret,
                        label = { Text("Code secret du conducteur") },
                        singleLine = true,
                        enabled = !state.verified && state.agentConducteurPersonnelId > 0,
                        leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
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
                            text = "Signature du conducteur",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = when {
                                state.signatureFromPersonnel -> "Signature récupérée depuis les données du personnel. Vous pouvez la garder ou en dessiner une nouvelle."
                                state.signatureSvg != null -> "Signature dessinée pour cette perception."
                                else -> "Aucune signature dans les données du personnel. Vous pouvez en capturer une ou laisser vide."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.signatureSvg != null) {
                            SvgSignatureView(
                                svg = state.signatureSvg!!,
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

                Spacer(modifier = Modifier.height(8.dp))
                OpusDropdown(
                    label = "Chef de bord (optionnel)",
                    options = personnelOptions.map {
                        "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}"
                    },
                    selected = personnelLabel(state.chefDeBordPersonnelId),
                    onSelect = { label ->
                        val idx = personnelOptions.indexOfFirst {
                            "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}" == label
                        }
                        if (idx >= 0) viewModel.setChefDeBord(personnelOptions[idx].id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Aucun chef de bord"
                )
            }

            // Perception
            FormSectionCard(title = "Perception", icon = Icons.Outlined.CalendarToday) {
                OutlinedTextField(
                    value = state.datePerception,
                    onValueChange = {},
                    label = { Text("Date de la perception *") },
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
                    value = state.heurePerception,
                    onValueChange = viewModel::setHeurePerception,
                    label = { Text("Heure de la perception *") },
                    trailingIcon = {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Compteurs de départ
            FormSectionCard(title = "Compteurs de départ (optionnel)") {
                OutlinedTextField(
                    value = state.kilometrageDepart,
                    onValueChange = viewModel::setKilometrageDepart,
                    label = { Text("Kilométrage de départ (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.niveauCarburantDepart,
                    onValueChange = viewModel::setNiveauCarburantDepart,
                    label = { Text("Carburant de départ (0–100%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Save button
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving && (!state.isEdit || state.canEdit),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (state.isEdit) "Mettre à jour" else "Enregistrer")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
