package com.gsoft.opus.presentation.declarationperte

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
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.WorkOutline
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
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.presentation.personnel.uriToUploadFile
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclarationPerteFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: DeclarationPerteFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showDatePertePicker by remember { mutableStateOf(false) }

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
                        if (state.isEdit) "Modifier la déclaration de perte" else "Nouvelle déclaration de perte",
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
            // ─── Déclaration section ──────────────────────────────────
            FormSectionCard(
                title = "Déclaration",
                icon = Icons.Outlined.CalendarMonth,
                subtitle = "Date, heure, déclarant et agent"
            ) {
                OutlinedTextField(
                    value = if (state.dateDeclaration.isBlank()) "" else formatDateDisplay(state.dateDeclaration),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de déclaration *") },
                    placeholder = { Text("JJ/MM/AAAA") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la date")
                        }
                    },
                    singleLine = true,
                    isError = state.dateDeclaration.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.heure,
                    onValueChange = viewModel::updateHeure,
                    label = { Text("Heure de déclaration *") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    isError = state.heure.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.identiteDeclarant,
                    onValueChange = viewModel::updateIdentiteDeclarant,
                    label = { Text("Identité du déclarant *") },
                    singleLine = true,
                    isError = state.identiteDeclarant.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.nomAgent,
                    onValueChange = viewModel::updateNomAgent,
                    label = { Text("Nom de l'agent *") },
                    singleLine = true,
                    isError = state.nomAgent.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Objet perdu section ──────────────────────────────────
            FormSectionCard(
                title = "Objet perdu",
                icon = Icons.Outlined.WorkOutline,
                subtitle = "Nature et description de l'objet"
            ) {
                OutlinedTextField(
                    value = state.natureObjet,
                    onValueChange = viewModel::updateNatureObjet,
                    label = { Text("Nature de l'objet *") },
                    singleLine = true,
                    isError = state.natureObjet.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.descriptionObjet,
                    onValueChange = viewModel::updateDescriptionObjet,
                    label = { Text("Description de l'objet *") },
                    minLines = 3,
                    isError = state.descriptionObjet.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Perte présumée section ───────────────────────────────
            FormSectionCard(
                title = "Perte présumée",
                icon = Icons.Outlined.Place,
                subtitle = "Date et lieu présumés de la perte"
            ) {
                OutlinedTextField(
                    value = if (state.datePerte.isBlank()) "" else formatDateDisplay(state.datePerte),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de perte *") },
                    placeholder = { Text("JJ/MM/AAAA") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePertePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la date")
                        }
                    },
                    singleLine = true,
                    isError = state.datePerte.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.lieuPerte,
                    onValueChange = viewModel::updateLieuPerte,
                    label = { Text("Lieu de perte *") },
                    singleLine = true,
                    isError = state.lieuPerte.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Attestation section ──────────────────────────────────
            FormSectionCard(
                title = "Attestation",
                icon = Icons.Outlined.FileCopy,
                subtitle = "Attestation de perte délivrée"
            ) {
                OutlinedTextField(
                    value = state.numeroAttestation,
                    onValueChange = viewModel::updateNumeroAttestation,
                    label = { Text("Numéro d'attestation *") },
                    singleLine = true,
                    isError = state.numeroAttestation.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Pièces jointes section ─────────────────────────────────
            FormSectionCard(
                title = "Pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés à la déclaration de perte"
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

            // ─── Error + submit ───────────────────────────────────────
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
            title = "Date de la déclaration",
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                viewModel.updateDateDeclaration(millisToIsoDate(millis))
            }
        )
    }

    if (showDatePertePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date de la perte",
            onDismiss = { showDatePertePicker = false },
            onConfirm = { millis ->
                showDatePertePicker = false
                viewModel.updateDatePerte(millisToIsoDate(millis))
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
