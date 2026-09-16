package com.gsoft.opus.presentation.arrestation

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.LocalPolice
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
import com.gsoft.opus.presentation.gardeavue.formatDateTimeDisplay
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.presentation.personnel.uriToUploadFile
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.OpusTimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrestationFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ArrestationFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Two-step datetime picker state for date_heure_arrestation.
    var showDateTimeDatePicker by remember { mutableStateOf(false) }
    var showDateTimeTimePicker by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf("") }

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
                        if (state.isEdit) "Modifier l'arrestation" else "Nouvelle arrestation",
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
            // ─── Arrestation ─────────────────────────────────────────
            FormSectionCard(
                title = "Arrestation",
                icon = Icons.Outlined.LocalPolice,
                subtitle = "Numéro et date de l'arrestation"
            ) {
                OutlinedTextField(
                    value = state.numero,
                    onValueChange = viewModel::updateNumero,
                    label = { Text("Numéro de l'arrestation ${if (!state.isEdit) "(suggéré — modifiable)" else ""}") },
                    enabled = !state.isEdit,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!state.isEdit) {
                    Text(
                        text = "Laisser vide pour auto-génération",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { viewModel.loadSuggestedNumber() }
                            .padding(vertical = 2.dp)
                    )
                }
                OutlinedTextField(
                    value = if (state.dateHeureArrestation.isBlank()) "" else formatDateTimeDisplay(state.dateHeureArrestation),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date et heure de l'arrestation *") },
                    placeholder = { Text("AAAA-MM-JJ HH:MM") },
                    trailingIcon = {
                        IconButton(onClick = { showDateTimeDatePicker = true }) {
                            Icon(Icons.Outlined.Schedule, contentDescription = "Choisir la date et l'heure")
                        }
                    },
                    isError = state.dateHeureArrestation.isBlank() && state.errorMessage != null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Personne arrêtée ────────────────────────────────────
            FormSectionCard(
                title = "Personne arrêtée",
                icon = Icons.Outlined.Description,
                subtitle = "Identité et lieu d'arrestation"
            ) {
                OutlinedTextField(
                    value = state.personneNom,
                    onValueChange = viewModel::updatePersonneNom,
                    label = { Text("Nom et prénom *") },
                    isError = state.personneNom.isBlank() && state.errorMessage != null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.lieuArrestation,
                    onValueChange = viewModel::updateLieuArrestation,
                    label = { Text("Adresse ou lieu d'arrestation") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Motif & policiers ───────────────────────────────────
            FormSectionCard(
                title = "Motif & policiers",
                icon = Icons.Outlined.Description,
                subtitle = "Motif de l'arrestation et policiers ayant procédé"
            ) {
                OutlinedTextField(
                    value = state.motif,
                    onValueChange = viewModel::updateMotif,
                    label = { Text("Motif de l'arrestation") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.policiers,
                    onValueChange = viewModel::updatePoliciers,
                    label = { Text("Noms et grades des policiers (un par ligne)") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Saisissez un policier par ligne pour permettre plusieurs officiers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ─── Dossier rattaché ────────────────────────────────────
            FormSectionCard(
                title = "Dossier rattaché",
                icon = Icons.Outlined.Description,
                subtitle = "Référence au dossier associé"
            ) {
                OutlinedTextField(
                    value = state.numeroDossier,
                    onValueChange = viewModel::updateNumeroDossier,
                    label = { Text("N° du dossier concerné") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Observations ───────────────────────────────────────
            FormSectionCard(
                title = "Informations complémentaires",
                icon = Icons.Outlined.Description,
                subtitle = "Observations"
            ) {
                OutlinedTextField(
                    value = state.observations,
                    onValueChange = viewModel::updateObservations,
                    label = { Text("Observations") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Pièces jointes ─────────────────────────────────────
            FormSectionCard(
                title = "Pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés à l'arrestation"
            ) {
                state.attachments.filter { !it.isDeleted }.forEach { item ->
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

    // Datetime pickers for date_heure_arrestation.
    if (showDateTimeDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date d'arrestation",
            onDismiss = {
                showDateTimeDatePicker = false
            },
            onConfirm = { millis ->
                pickedDate = millisToIsoDate(millis)
                showDateTimeDatePicker = false
                showDateTimeTimePicker = true
            }
        )
    }
    if (showDateTimeTimePicker) {
        OpusTimePickerDialog(
            visible = true,
            title = "Heure d'arrestation",
            onDismiss = {
                showDateTimeTimePicker = false
            },
            onConfirm = { hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                viewModel.updateDateHeureArrestation("$pickedDate $time:00")
                showDateTimeTimePicker = false
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
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Ajouter une pièce jointe",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
