package com.gsoft.opus.presentation.situationgav

import android.net.Uri
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
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
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
import com.gsoft.opus.domain.model.GardeAVue
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.presentation.gardeavue.formatDateTimeDisplay
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.OpusTimePickerDialog
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.presentation.personnel.uriToUploadFile
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton
import com.gsoft.opus.ui.components.OpusDropdown

/** Personne concernée: GAV person identity + custody start for disambiguation. */
private fun gavLabel(g: GardeAVue): String {
    val name = listOfNotNull(g.nom, g.prenoms).filter { it.isNotBlank() }.joinToString(" ")
    val debut = g.debutGav?.let { formatDateTimeDisplay(it) }
    return if (debut.isNullOrBlank()) name else "$name — GAV du $debut"
}

/** Agent display: grade + lastname. */
private fun agentLabel(p: Personnel): String =
    "${p.grade} ${p.lastname}".trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SituationGavFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: SituationGavFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Datetime picker state for date_controle.
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

    // Camera capture into a temp URI, then attach to the current attachment row.
    var attachmentCameraIndex by remember { mutableStateOf(-1) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val index = attachmentCameraIndex
        val uri = cameraPhotoUri
        if (success && uri != null && index >= 0) {
            val uploadFile = uriToUploadFile(context, uri)
            if (uploadFile != null) {
                viewModel.setAttachmentFile(index, uploadFile)
            }
        }
        attachmentCameraIndex = -1
        cameraPhotoUri = null
    }

    fun launchCameraForAttachment(index: Int) {
        val uri = createTempImageUri(context)
        if (uri != null) {
            attachmentCameraIndex = index
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEdit) "Modifier la situation GAV" else "Nouvelle situation GAV",
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
            // ─── Contrôle section ────────────────────────────────────
            FormSectionCard(
                title = "Contrôle",
                icon = Icons.Outlined.Person,
                subtitle = "Personne concernée, date/heure et agent"
            ) {
                // Personne concernée (from existing GAV records)
                val gavOptions = state.gavOptions
                val gavSelectedLabel = gavOptions
                    .firstOrNull { it.id == state.gardeAVueId }
                    ?.let { gavLabel(it) } ?: ""
                OpusDropdown(
                    label = "Personne concernée *",
                    options = gavOptions.map { gavLabel(it) },
                    selected = gavSelectedLabel,
                    onSelect = { label ->
                        gavOptions.firstOrNull { gavLabel(it) == label }
                            ?.let { viewModel.updateGardeAVueId(it.id) }
                    },
                    placeholder = "Sélectionner une personne en garde à vue",
                    isError = state.gardeAVueId == 0 && state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date et heure du contrôle
                DateTimeField(
                    label = "Date et heure du contrôle *",
                    value = state.dateControle,
                    onPick = {
                        pickedDate = state.dateControle.take(10)
                        pickedTime = if (state.dateControle.length >= 16) state.dateControle.substring(11, 16) else ""
                        showDateTimeDatePicker = true
                    },
                    isError = state.dateControle.isBlank() && state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Agent ayant effectué le contrôle (grade + lastname)
                val agentOptions = state.personnelOptions
                val agentSelectedLabel = agentOptions
                    .firstOrNull { it.id == state.agentControleId }
                    ?.let { agentLabel(it) } ?: ""
                OpusDropdown(
                    label = "Agent ayant effectué le contrôle",
                    options = agentOptions.map { agentLabel(it) },
                    selected = agentSelectedLabel,
                    onSelect = { label ->
                        agentOptions.firstOrNull { agentLabel(it) == label }
                            ?.let { viewModel.updateAgentControle(it.id) }
                    },
                    placeholder = "Sélectionner un agent",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── État & observations section ─────────────────────────
            FormSectionCard(
                title = "État & observations",
                icon = Icons.Outlined.HealthAndSafety,
                subtitle = "État général, observations et mesures prises"
            ) {
                OutlinedTextField(
                    value = state.etatGeneral,
                    onValueChange = viewModel::updateEtatGeneral,
                    label = { Text("État général de la personne") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.observations,
                    onValueChange = viewModel::updateObservations,
                    label = { Text("Observations") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.mesuresPrises,
                    onValueChange = viewModel::updateMesuresPrises,
                    label = { Text("Mesures prises") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Photos / pièces jointes section ──────────────────────
            FormSectionCard(
                title = "Photos & pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Photos ou documents associés au contrôle"
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
                        onTakePhoto = { launchCameraForAttachment(realIndex) },
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

    // Datetime pickers for date_controle.
    // Step 1: pick the date.
    if (showDateTimeDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date du contrôle",
            onDismiss = { showDateTimeDatePicker = false },
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
            title = "Heure du contrôle",
            onDismiss = { showDateTimeTimePicker = false },
            onConfirm = { hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                viewModel.updateDateControle("$pickedDate $time:00")
                showDateTimeTimePicker = false
            }
        )
    }
}

@Composable
private fun DateTimeField(
    label: String,
    value: String,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = if (value.isBlank()) "" else formatDateTimeDisplay(value),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("AAAA-MM-JJ HH:MM") },
        isError = isError,
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
    onTakePhoto: () -> Unit,
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
            IconButton(onClick = onTakePhoto) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = "Prendre une photo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
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
            text = "Ajouter une photo ou un fichier",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Create a temp file URI for camera capture (same pattern as
 * PersonneRechercheeFormScreen — uses the app FileProvider).
 */
private fun createTempImageUri(context: android.content.Context): Uri? {
    return runCatching {
        val file = java.io.File(context.cacheDir, "sgav_photo_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }.getOrNull()
}
