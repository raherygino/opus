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
import com.gsoft.opus.ui.components.OpusDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainteSortieFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlainteSortieFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDateSortiePicker by remember { mutableStateOf(false) }
    var showDateDeferrementPicker by remember { mutableStateOf(false) }

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
                        if (state.isEdit) "Modifier la sortie" else "Nouvelle sortie",
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
            // ─── ENTRÉE selection (read-only on edit, picker on create) ──
            val summary = state.entreeSummary
            if (state.isEdit || summary != null) {
                FormSectionCard(
                    title = "Plainte ENTRÉE associée",
                    icon = Icons.Outlined.Description,
                    subtitle = "Plainte d'origine"
                ) {
                    if (summary != null) {
                        Text(
                            text = summary.typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = summary.numeroDossier,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Date: ${formatDateDisplay(summary.datePlainte)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!summary.infraction.isNullOrBlank()) {
                            Text(
                                text = "Infraction: ${summary.infraction}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!summary.miseEnCause.isNullOrBlank()) {
                            Text(
                                text = "MC: ${summary.miseEnCause}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!state.isEdit) {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.TextButton(onClick = { viewModel.changeEntree() }) {
                            Text("Changer de plainte ENTRÉE")
                        }
                    }
                }
            } else if (!state.isEdit && state.plainteEntreeId == 0) {
                // ENTRÉE picker — show available ENTRÉEs without sortie.
                FormSectionCard(
                    title = "Sélectionner une plainte ENTRÉE",
                    icon = Icons.Outlined.Description,
                    subtitle = "Choisissez la plainte à traiter"
                ) {
                    if (state.isLoadingEntrees) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chargement des plaintes...")
                        }
                    } else if (state.availableEntrees.isEmpty()) {
                        Text(
                            text = "Aucune plainte ENTRÉE sans sortie disponible. Créez d'abord une plainte ENTRÉE.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.availableEntrees.forEach { entree ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable { viewModel.selectEntree(entree.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entree.typeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = entree.numeroDossier,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${formatDateDisplay(entree.datePlainte)} — MC: ${entree.miseEnCause ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // ─── Sortie section ────────────────────────────────────────
            FormSectionCard(
                title = "Sortie",
                icon = Icons.Outlined.Gavel,
                subtitle = "Nature et détails de la sortie"
            ) {
                OpusDropdown(
                    label = "Nature *",
                    options = PLAINTE_SORTIE_NATURE_LABELS.values.toList(),
                    selected = PLAINTE_SORTIE_NATURE_LABELS[state.nature] ?: state.nature,
                    onSelect = { label ->
                        PLAINTE_SORTIE_NATURE_LABELS.entries.firstOrNull { it.value == label }
                            ?.let { viewModel.updateNature(it.key) }
                    },
                    isError = state.nature.isBlank() && state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = if (state.dateSortie.isBlank()) "" else formatDateDisplay(state.dateSortie),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date *") },
                    placeholder = { Text("JJ/MM/AAAA") },
                    trailingIcon = {
                        IconButton(onClick = { showDateSortiePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la date")
                        }
                    },
                    singleLine = true,
                    isError = state.dateSortie.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // numero: editable on create (pre-filled with the
                // suggested next number), read-only on edit.
                if (state.isEdit) {
                    if (state.numero.isNotBlank()) {
                        OutlinedTextField(
                            value = state.numero,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Numéro") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = state.numero,
                        onValueChange = viewModel::updateNumero,
                        label = { Text("Numéro (suggéré — modifiable)") },
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

                OutlinedTextField(
                    value = state.numeroTtr,
                    onValueChange = viewModel::updateNumeroTtr,
                    label = { Text("N° TTR *") },
                    singleLine = true,
                    isError = state.numeroTtr.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.nomSubstitut,
                    onValueChange = viewModel::updateNomSubstitut,
                    label = { Text("Nom du Substitut *") },
                    singleLine = true,
                    isError = state.nomSubstitut.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // date_deferrement is DEFERREMENT only.
                if (state.nature == "DEFERREMENT") {
                    OutlinedTextField(
                        value = if (state.dateDeferrement.isBlank()) "" else formatDateDisplay(state.dateDeferrement),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date du déferrement *") },
                        placeholder = { Text("JJ/MM/AAAA") },
                        trailingIcon = {
                            IconButton(onClick = { showDateDeferrementPicker = true }) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la date")
                            }
                        },
                        singleLine = true,
                        isError = state.dateDeferrement.isBlank() && state.errorMessage != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                subtitle = "Documents associés à la sortie"
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

    if (showDateSortiePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date de la sortie",
            onDismiss = { showDateSortiePicker = false },
            onConfirm = { millis ->
                showDateSortiePicker = false
                viewModel.updateDateSortie(millisToIsoDate(millis))
            }
        )
    }

    if (showDateDeferrementPicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date du déferrement",
            onDismiss = { showDateDeferrementPicker = false },
            onConfirm = { millis ->
                showDateDeferrementPicker = false
                viewModel.updateDateDeferrement(millisToIsoDate(millis))
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
