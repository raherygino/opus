package com.gsoft.opus.presentation.personnerecherchee

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
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gsoft.opus.presentation.personnel.uriToUploadFile
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonneRechercheeFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: PersonneRechercheeFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Camera capture: we use a temp URI to capture into.
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            val uploadFile = uriToUploadFile(context, cameraPhotoUri!!)
            if (uploadFile != null) {
                viewModel.addPhoto(uploadFile, cameraPhotoUri.toString(), "CAMERA")
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val uploadFile = uriToUploadFile(context, uri)
            if (uploadFile != null) {
                viewModel.addPhoto(uploadFile, uri.toString(), "GALLERY")
            }
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
        }
    }

    fun launchCamera() {
        val uri = createTempImageUri(context)
        cameraPhotoUri = uri
        if (uri != null) {
            cameraLauncher.launch(uri)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEdit) "Modifier la personne recherchée" else "Nouvelle personne recherchée",
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
            // ─── Identité section ─────────────────────────────────────
            FormSectionCard(
                title = "Identité",
                icon = Icons.Outlined.Person,
                subtitle = "Nom et adresse"
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
                    value = state.adresse,
                    onValueChange = viewModel::updateAdresse,
                    label = { Text("Adresse") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Motif section ────────────────────────────────────────
            FormSectionCard(
                title = "Motif",
                icon = Icons.Outlined.Description,
                subtitle = "Motif de la recherche"
            ) {
                OutlinedTextField(
                    value = state.motif,
                    onValueChange = viewModel::updateMotif,
                    label = { Text("Motif *") },
                    isError = state.motif.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Photos section (dedicated multi-image) ──────────────
            FormSectionCard(
                title = "Photos (${state.photos.count { !it.isDeleted }})",
                icon = Icons.Outlined.AddAPhoto,
                subtitle = "Photos de la personne (caméra ou galerie)"
            ) {
                // Photo capture buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhotoSourceButton(
                        icon = Icons.Outlined.AddAPhoto,
                        label = "Prendre une photo",
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f)
                    )
                    PhotoSourceButton(
                        icon = Icons.Outlined.PhotoLibrary,
                        label = "Choisir de la galerie",
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thumbnails grid
                state.photos.forEachIndexed { index, photo ->
                    if (photo.isDeleted) return@forEachIndexed
                    PhotoThumbnailCard(
                        item = photo,
                        onCaptionChange = { viewModel.updatePhotoCaption(index, it) },
                        onRemove = { viewModel.removePhoto(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
}

@Composable
private fun PhotoSourceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PhotoThumbnailCard(
    item: PhotoItem,
    onCaptionChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            val imageModel = item.previewUri ?: item.existingFilename
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = item.caption,
            onValueChange = onCaptionChange,
            label = { Text("Légende (optionnel)") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * Create a temp file URI for camera capture.
 */
private fun createTempImageUri(context: android.content.Context): Uri? {
    return runCatching {
        val file = java.io.File(context.cacheDir, "pr_photo_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }.getOrNull()
}
