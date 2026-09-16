package com.gsoft.opus.presentation.plainte

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.presentation.personnel.uriToUploadFile
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton
import com.gsoft.opus.ui.components.OpusDropdown

private fun personnelLabel(p: Personnel): String =
    "${p.lastname} ${p.firstname} (${p.im}) — ${p.grade}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainteFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlainteFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
        }
    }

    var attachmentFilePickerIndex by remember { mutableStateOf(-1) }
    val attachmentFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && attachmentFilePickerIndex >= 0) {
            val uploadFile = uriToUploadFile(context, uri)
            if (uploadFile != null) {
                viewModel.setAttachmentFile(attachmentFilePickerIndex, uploadFile)
            }
        }
        attachmentFilePickerIndex = -1
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEdit) "Modifier la plainte" else "Nouvelle plainte ENTRÉE",
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
            // ─── Type + Dossier section ────────────────────────────────
            FormSectionCard(
                title = "Type de plainte & dossier",
                icon = Icons.Outlined.Description,
                subtitle = "Sélectionnez le type de plainte"
            ) {
                OpusDropdown(
                    label = "Type de plainte *",
                    options = PLAINTE_ENTREE_TYPE_LABELS.values.toList(),
                    selected = PLAINTE_ENTREE_TYPE_LABELS[state.type] ?: state.type,
                    onSelect = { label ->
                        PLAINTE_ENTREE_TYPE_LABELS.entries.firstOrNull { it.value == label }
                            ?.let { viewModel.updateType(it.key) }
                    },
                    isError = state.type.isBlank() && state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = if (state.datePlainte.isBlank()) "" else formatDateDisplay(state.datePlainte),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date *") },
                    placeholder = { Text("JJ/MM/AAAA") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la date")
                        }
                    },
                    singleLine = true,
                    isError = state.datePlainte.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // numero_dossier: editable on create (pre-filled with the
                // suggested next number), read-only on edit.
                if (state.isEdit) {
                    if (state.numeroDossier.isNotBlank()) {
                        OutlinedTextField(
                            value = state.numeroDossier,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Numéro du dossier") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = state.numeroDossier,
                        onValueChange = viewModel::updateNumeroDossier,
                        label = { Text("Numéro du dossier (suggéré — modifiable)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.refreshSuggestedNumber() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Régénérer le numéro suggéré")
                    }
                }

                // numero_st is ST_PARQUET only.
                if (state.type == "ST_PARQUET") {
                    OutlinedTextField(
                        value = state.numeroSt,
                        onValueChange = viewModel::updateNumeroSt,
                        label = { Text("Numéro du ST *") },
                        singleLine = true,
                        isError = state.numeroSt.isBlank() && state.errorMessage != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ─── Personnes section ────────────────────────────────────
            FormSectionCard(
                title = "Personnes",
                icon = Icons.Outlined.Person,
                subtitle = "OPJ, enquêteur, parties"
            ) {
                // OPJ picker
                val opjOptions = state.personnelOptions
                val opjSelectedLabel = opjOptions
                    .firstOrNull { it.id == state.opjPersonnelId }
                    ?.let { personnelLabel(it) } ?: ""
                OpusDropdown(
                    label = "OPJ *",
                    options = opjOptions.map { personnelLabel(it) },
                    selected = opjSelectedLabel,
                    onSelect = { label ->
                        opjOptions.firstOrNull { personnelLabel(it) == label }
                            ?.let { viewModel.updateOpj(it.id) }
                    },
                    placeholder = "Sélectionner un OPJ",
                    isError = state.opjPersonnelId == 0 && state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Enquêteur picker
                val enqOptions = state.personnelOptions
                val enqSelectedLabel = enqOptions
                    .firstOrNull { it.id == state.enqueteurPersonnelId }
                    ?.let { personnelLabel(it) } ?: ""
                OpusDropdown(
                    label = "Enquêteur *",
                    options = enqOptions.map { personnelLabel(it) },
                    selected = enqSelectedLabel,
                    onSelect = { label ->
                        enqOptions.firstOrNull { personnelLabel(it) == label }
                            ?.let { viewModel.updateEnqueteur(it.id) }
                    },
                    placeholder = "Sélectionner un enquêteur",
                    isError = state.enqueteurPersonnelId == 0 && state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                // partie_civile (ST & PD only)
                if (state.type in listOf("ST_PARQUET", "PLAINTE_DIRECTE")) {
                    OutlinedTextField(
                        value = state.partieCivile,
                        onValueChange = viewModel::updatePartieCivile,
                        label = { Text("Partie civile (PC) *") },
                        singleLine = true,
                        isError = state.partieCivile.isBlank() && state.errorMessage != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // mise_en_cause (all types)
                OutlinedTextField(
                    value = state.miseEnCause,
                    onValueChange = viewModel::updateMiseEnCause,
                    label = { Text("Mise en cause (MC) *") },
                    singleLine = true,
                    isError = state.miseEnCause.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // adresse_pc (ST & PD only)
                if (state.type in listOf("ST_PARQUET", "PLAINTE_DIRECTE")) {
                    OutlinedTextField(
                        value = state.adressePc,
                        onValueChange = viewModel::updateAdressePc,
                        label = { Text("Adresse du PC *") },
                        minLines = 2,
                        isError = state.adressePc.isBlank() && state.errorMessage != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ─── Infraction section ───────────────────────────────────
            FormSectionCard(
                title = "Infraction",
                icon = Icons.Outlined.Gavel,
                subtitle = "Détails de l'infraction"
            ) {
                OutlinedTextField(
                    value = state.infraction,
                    onValueChange = viewModel::updateInfraction,
                    label = { Text("Infraction *") },
                    singleLine = true,
                    isError = state.infraction.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.prejudice,
                    onValueChange = viewModel::updatePrejudice,
                    label = { Text("Préjudice *") },
                    minLines = 2,
                    isError = state.prejudice.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.lieuInfraction,
                    onValueChange = viewModel::updateLieuInfraction,
                    label = { Text("Lieu de l'infraction *") },
                    singleLine = true,
                    isError = state.lieuInfraction.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.heureInfraction,
                    onValueChange = viewModel::updateHeureInfraction,
                    label = { Text("Heure de l'infraction") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    isError = state.heureInfraction.isNotBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Observation section ──────────────────────────────────
            FormSectionCard(
                title = "Observation",
                icon = Icons.Outlined.Description,
                subtitle = "Remarques complémentaires"
            ) {
                OutlinedTextField(
                    value = state.observation,
                    onValueChange = viewModel::updateObservation,
                    label = { Text("Observation") },
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Pièces jointes section ──────────────────────────────
            FormSectionCard(
                title = "Pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés à la plainte"
            ) {
                state.attachments.filter { !it.isDeleted }.forEachIndexed { _, item ->
                    val realIndex = state.attachments.indexOf(item)
                    AttachmentFieldCard(
                        title = item.title,
                        fileName = item.uploadFile?.fileName ?: item.existingFilename,
                        onTitleChange = { viewModel.updateAttachmentTitle(realIndex, it) },
                        onPickFile = {
                            attachmentFilePickerIndex = realIndex
                            attachmentFilePickerLauncher.launch("*/*")
                        },
                        onRemove = { viewModel.removeAttachment(realIndex) }
                    )
                }

                AddAttachmentButton(onClick = { viewModel.addAttachment() })
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

    if (showDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date de la plainte",
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                viewModel.updateDatePlainte(millisToIsoDate(millis))
            }
        )
    }
}

@Composable
private fun AttachmentFieldCard(
    title: String,
    fileName: String?,
    onTitleChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Titre") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onPickFile() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = fileName ?: "Aucun fichier — touchez pour choisir",
                style = MaterialTheme.typography.bodySmall,
                color = if (fileName.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddAttachmentButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Ajouter une pièce jointe",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
