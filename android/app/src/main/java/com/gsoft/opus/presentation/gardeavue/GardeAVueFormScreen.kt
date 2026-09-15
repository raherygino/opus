package com.gsoft.opus.presentation.gardeavue

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
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LocalPolice
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
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.OpusTimePickerDialog
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.presentation.personnel.uriToUploadFile
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardeAVueFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: GardeAVueFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Date picker state for the date_naissance field.
    var showDatePicker by remember { mutableStateOf(false) }

    // Datetime picker state: which datetime field is being edited, and the
    // intermediate date/time selections before combining them.
    var activeDateTimeField by remember { mutableStateOf<String?>(null) }
    var pickedDate by remember { mutableStateOf("") }
    var pickedTime by remember { mutableStateOf("") }
    var showDateTimeDatePicker by remember { mutableStateOf(false) }
    var showDateTimeTimePicker by remember { mutableStateOf(false) }

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
                        if (state.isEdit) "Modifier la garde à vue" else "Nouvelle garde à vue",
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
            // ─── Identité section ────────────────────────────────────
            FormSectionCard(
                title = "Identité",
                icon = Icons.Outlined.Person,
                subtitle = "Informations sur la personne en garde à vue"
            ) {
                OutlinedTextField(
                    value = state.nom,
                    onValueChange = viewModel::updateNom,
                    label = { Text("Nom *") },
                    singleLine = true,
                    isError = state.nom.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.prenoms,
                    onValueChange = viewModel::updatePrenoms,
                    label = { Text("Prénoms") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = if (state.dateNaissance.isBlank()) "" else formatDateDisplay(state.dateNaissance),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de naissance") },
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
                    value = state.adresse,
                    onValueChange = viewModel::updateAdresse,
                    label = { Text("Adresse") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Police / Investigation section ─────────────────────
            FormSectionCard(
                title = "Enquête",
                icon = Icons.Outlined.LocalPolice,
                subtitle = "Enquêteur et OPJ ayant décidé la garde à vue"
            ) {
                OutlinedTextField(
                    value = state.enqueteurPermance,
                    onValueChange = viewModel::updateEnqueteurPermance,
                    label = { Text("Enquêteur de permanence") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.opjGav,
                    onValueChange = viewModel::updateOpjGav,
                    label = { Text("OPJ ayant décidé la garde à vue") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.motif,
                    onValueChange = viewModel::updateMotif,
                    label = { Text("Motif") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Santé & droits section ──────────────────────────────
            FormSectionCard(
                title = "Santé & droits",
                icon = Icons.Outlined.HealthAndSafety,
                subtitle = "État de santé, droits notifiés et personne à contacter"
            ) {
                OutlinedTextField(
                    value = state.etatSante,
                    onValueChange = viewModel::updateEtatSante,
                    label = { Text("État de santé") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.droitsNotifies,
                    onValueChange = viewModel::updateDroitsNotifies,
                    label = { Text("Droits notifiés") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.personneContacter,
                    onValueChange = viewModel::updatePersonneContacter,
                    label = { Text("Personne à contacter") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── GAV Dates section ───────────────────────────────────
            FormSectionCard(
                title = "Dates de la garde à vue",
                icon = Icons.Outlined.Schedule,
                subtitle = "Début, fin et prolongation"
            ) {
                DateTimeField(
                    label = "Début de GAV",
                    value = state.debutGav,
                    onPick = {
                        activeDateTimeField = "debut_gav"
                        pickedDate = state.debutGav.take(10)
                        pickedTime = if (state.debutGav.length >= 16) state.debutGav.substring(11, 16) else ""
                        showDateTimeDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DateTimeField(
                    label = "Fin de GAV",
                    value = state.finGav,
                    onPick = {
                        activeDateTimeField = "fin_gav"
                        pickedDate = state.finGav.take(10)
                        pickedTime = if (state.finGav.length >= 16) state.finGav.substring(11, 16) else ""
                        showDateTimeDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DateTimeField(
                    label = "Prolongation de GAV",
                    value = state.prolongationGav,
                    onPick = {
                        activeDateTimeField = "prolongation_gav"
                        pickedDate = state.prolongationGav.take(10)
                        pickedTime = if (state.prolongationGav.length >= 16) state.prolongationGav.substring(11, 16) else ""
                        showDateTimeDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Pièces jointes section ──────────────────────────────
            FormSectionCard(
                title = "Pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés à la garde à vue"
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

    // Date picker for date_naissance
    if (showDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date de naissance",
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                viewModel.updateDateNaissance(millisToIsoDate(millis))
            }
        )
    }

    // Datetime pickers for the GAV date fields.
    // Step 1: pick the date.
    if (showDateTimeDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date",
            onDismiss = {
                showDateTimeDatePicker = false
                activeDateTimeField = null
            },
            onConfirm = { millis ->
                pickedDate = millisToIsoDate(millis)
                showDateTimeDatePicker = false
                showDateTimeTimePicker = true
            }
        )
    }
    // Step 2: pick the time, then combine with the date.
    if (showDateTimeTimePicker) {
        val initialHour = pickedTime.substringBefore(":").toIntOrNull() ?: 0
        val initialMinute = pickedTime.substringAfter(":").toIntOrNull() ?: 0
        OpusTimePickerDialog(
            visible = true,
            initialHour = initialHour,
            initialMinute = initialMinute,
            title = "Heure",
            onDismiss = {
                showDateTimeTimePicker = false
                activeDateTimeField = null
            },
            onConfirm = { hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                val combined = "$pickedDate $time:00"
                when (activeDateTimeField) {
                    "debut_gav" -> viewModel.updateDebutGav(combined)
                    "fin_gav" -> viewModel.updateFinGav(combined)
                    "prolongation_gav" -> viewModel.updateProlongationGav(combined)
                }
                showDateTimeTimePicker = false
                activeDateTimeField = null
            }
        )
    }
}

@Composable
private fun DateTimeField(
    label: String,
    value: String,
    onPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = if (value.isBlank()) "" else formatDateTimeDisplay(value),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("AAAA-MM-JJ HH:MM") },
        trailingIcon = {
            IconButton(onClick = onPick) {
                Icon(Icons.Outlined.Schedule, contentDescription = "Choisir la date et l'heure")
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
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
