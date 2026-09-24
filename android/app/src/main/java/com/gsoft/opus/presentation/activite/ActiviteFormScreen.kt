package com.gsoft.opus.presentation.activite

import android.Manifest
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gsoft.opus.domain.model.Activite
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
fun ActiviteFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ActiviteFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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

    // ─── Camera capture ─────────────────────────────────────────────
    // Taking a photo directly uses the device camera (TakePicture to a
    // FileProvider URI), exactly like the évènements survenus feature.
    var attachmentCameraIndex by remember { mutableStateOf(-1) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val index = attachmentCameraIndex
        val uri = cameraPhotoUri
        attachmentCameraIndex = -1
        cameraPhotoUri = null
        if (success && uri != null && index >= 0) {
            uriToUploadFile(context, uri)?.let { viewModel.setAttachmentFile(index, it) }
        }
    }

    fun startCameraCapture(index: Int) {
        val uri = createTempImageUri(context)
        if (uri != null) {
            cameraPhotoUri = uri
            attachmentCameraIndex = index
            cameraLauncher.launch(uri)
        }
    }

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
                        if (state.isEdit) "Modifier l'activité" else "Nouvelle activité",
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
                icon = Icons.Outlined.Route,
                subtitle = "Patrouilles et interventions"
            ) {
                OutlinedTextField(
                    value = if (state.dateActivite.isBlank()) "" else formatDateDisplay(state.dateActivite),
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
                    isError = state.dateActivite.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.heureActivite,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Heure *") },
                    placeholder = { Text("HH:MM") },
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Outlined.AccessTime, contentDescription = "Choisir l'heure")
                        }
                    },
                    singleLine = true,
                    isError = state.heureActivite.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                )
            }

            // ─── Type de patrouille ──
            FormSectionCard(
                title = "Type de patrouille",
                icon = Icons.Outlined.Route,
                subtitle = "Sélectionnez les modes de patrouille et renseignez leur itinéraire"
            ) {
                Activite.PATROUILLE_TYPES.forEach { (type, typeLabel) ->
                    Text(
                        text = "Patrouille $typeLabel",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Activite.PATROUILLE_MODES.forEach { (mode, modeLabel) ->
                        val row = state.patrouilles.firstOrNull { it.type == type && it.mode == mode }
                            ?: return@forEach
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.togglePatrouille(type, mode) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = row.selected,
                                    onCheckedChange = { viewModel.togglePatrouille(type, mode) }
                                )
                                Text(
                                    text = modeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (row.selected) {
                                OutlinedTextField(
                                    value = row.itineraire,
                                    onValueChange = { viewModel.updatePatrouilleItineraire(type, mode, it) },
                                    label = { Text("Itinéraire") },
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 40.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ─── Opération & faits ──
            FormSectionCard(
                title = "Opération & faits",
                icon = Icons.Outlined.AttachFile
            ) {
                OutlinedTextField(
                    value = state.operationCiblee,
                    onValueChange = viewModel::updateOperationCiblee,
                    label = { Text("Opération ciblée") },
                    placeholder = { Text("Ex : Contrôle CIN, Contrôle débit de boissons") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.faitsConstates,
                    onValueChange = viewModel::updateFaitsConstates,
                    label = { Text("Faits constatés") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Hiérarchie ──
            FormSectionCard(
                title = "Hiérarchie",
                icon = Icons.Outlined.Route
            ) {
                OutlinedTextField(
                    value = state.compteRenduHierarchie,
                    onValueChange = viewModel::updateCompteRenduHierarchie,
                    label = { Text("Compte-rendu hiérarchie") },
                    placeholder = { Text("Compte-rendu temps réel à l'Autorité et/ou au second") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.conduiteATenir,
                    onValueChange = viewModel::updateConduiteATenir,
                    label = { Text("Conduite à tenir") },
                    placeholder = { Text("Instructions de l'Autorité et/ou du second") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Intervention ──
            FormSectionCard(
                title = "Intervention",
                icon = Icons.Outlined.Route
            ) {
                OutlinedTextField(
                    value = state.natureIntervention,
                    onValueChange = viewModel::updateNatureIntervention,
                    label = { Text("Nature de l'intervention") },
                    placeholder = { Text("Ex : Tapage nocturne, Braquage en cours, Intervention police-secours") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.suitesDonnees,
                    onValueChange = viewModel::updateSuitesDonnees,
                    label = { Text("Suites données") },
                    placeholder = { Text("Ex : Conduite au Poste pour examen de situation, Interpellation, RAS") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Pièces jointes section ─────────────────────────────
            FormSectionCard(
                title = "Pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés à l'activité"
            ) {
                state.attachments.filter { !it.isDeleted }.forEach { item ->
                    val realIndex = state.attachments.indexOf(item)
                    AttachmentFieldCard(
                        title = item.title,
                        fileName = item.uploadFile?.fileName ?: item.existingFilename,
                        imageBytes = item.uploadFile?.takeIf { f ->
                            f.mimeType?.startsWith("image/") == true
                        }?.bytes,
                        onTitleChange = { viewModel.updateAttachmentTitle(realIndex, it) },
                        onPickFile = {
                            attachmentFilePickerIndex = realIndex
                            attachmentFilePickerLauncher.launch("*/*")
                        },
                        onTakePhoto = { startCameraCapture(realIndex) },
                        onRemove = { viewModel.removeAttachment(realIndex) }
                    )
                }

                AddAttachmentButton(onClick = { viewModel.addAttachment() })
            }

            // ─── Localisation GPS ─────────────────────────────────────
            // On Android, the agent must enable location services and
            // capture the device's GPS coordinates before creating an
            // activité. This is required — the save is blocked until a
            // location is captured (on create), like the évènement feature.
            if (!state.isEdit) {
                FormSectionCard(
                    title = "Localisation GPS",
                    icon = Icons.Outlined.LocationOn,
                    subtitle = "Position requise pour enregistrer l'activité"
                ) {
                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val granted = permissions.values.any { it }
                        if (granted) {
                            viewModel.captureLocation()
                        }
                    }

                    if (state.isCapturingLocation) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Capture de la position...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (state.latitude != null && state.longitude != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Position capturée",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Lat: ${"%.6f".format(state.latitude)}, Lon: ${"%.6f".format(state.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { viewModel.captureLocation() }) {
                                Text("Recapturer")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (viewModel.hasLocationPermission()) {
                                    viewModel.captureLocation()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Capturer ma position")
                        }
                    }

                    if (state.locationError != null) {
                        Text(
                            text = state.locationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
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
            title = "Date de l'activité",
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                viewModel.updateDateActivite(millisToIsoDate(millis))
            }
        )
    }

    if (showTimePicker) {
        val parts = state.heureActivite.split(":")
        OpusTimePickerDialog(
            visible = true,
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            title = "Heure de l'activité",
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                showTimePicker = false
                viewModel.updateHeureActivite("%02d:%02d".format(hour, minute))
            }
        )
    }
}

@Composable
private fun AttachmentFieldCard(
    title: String,
    fileName: String?,
    imageBytes: ByteArray?,
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

        // Preview the captured/picked image before saving — the user can
        // retake it via the camera button below.
        if (imageBytes != null) {
            AsyncImage(
                model = imageBytes,
                contentDescription = "Aperçu de la photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
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
            IconButton(onClick = onTakePhoto, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = "Prendre une photo",
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Ajouter une pièce jointe",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Create a temp file URI for camera capture.
 */
private fun createTempImageUri(context: android.content.Context): Uri? {
    return runCatching {
        val file = java.io.File(context.cacheDir, "act_photo_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }.getOrNull()
}
