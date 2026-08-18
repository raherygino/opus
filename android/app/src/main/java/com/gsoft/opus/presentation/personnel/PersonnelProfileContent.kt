package com.gsoft.opus.presentation.personnel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.gsoft.opus.domain.model.Comportement
import com.gsoft.opus.domain.model.Mouvement
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.PersonnelAttachment

/**
 * Shared social-profile-style content for both personnel detail screens
 * (Gestion Personnel and the bottom-navigation Personnels tab).
 *
 * @param state               The [PersonnelDetailUiState] driving the content.
 * @param onEdit              When non-null, shows the edit action button.
 * @param onAddAttachment     When non-null, shows the add-attachment action button.
 * @param onDeleteAttachment  When non-null, attachments show a delete button.
 */
@Composable
fun PersonnelProfileContent(
    state: PersonnelDetailUiState,
    onEdit: ((Int) -> Unit)? = null,
    onAddAttachment: (() -> Unit)? = null,
    onDeleteAttachment: ((PersonnelAttachment) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    var showPhotoViewer by remember { mutableStateOf(false) }

    // Expandable INFO sections — attachments open by default
    var attachmentsExpanded by remember { mutableStateOf(true) }
    var mouvementsExpanded by remember { mutableStateOf(false) }
    var comportementsExpanded by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.notFound || state.personnel == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Personnel introuvable", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val p = state.personnel
    val photoUrl = p.photo?.let { personnelPhotoUrl(p.id) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── Profile header (photo + name) ────────────────────────────
        item {
            ProfileHeader(
                photoUrl = photoUrl,
                name = "${p.firstname} ${p.lastname}",
                grade = p.grade,
                im = p.im,
                affectation = p.affectation,
                status = p.status,
                onPhotoClick = { if (photoUrl != null) showPhotoViewer = true }
            )
        }

        // ─── Circular action buttons ──────────────────────────────────
        if (onEdit != null || onAddAttachment != null || state.canViewRecords) {
            item {
                ProfileActionButtonsRow(
                    showEdit = onEdit != null,
                    onEdit = { onEdit?.invoke(p.id) },
                    showAddAttachment = onAddAttachment != null,
                    onAddAttachment = { onAddAttachment?.invoke() },
                    showRecords = state.canViewRecords,
                    onShowMouvements = { mouvementsExpanded = !mouvementsExpanded },
                    onShowComportements = { comportementsExpanded = !comportementsExpanded }
                )
            }
        }

        // ─── Contact rows ─────────────────────────────────────────────
        if (!p.phone.isNullOrBlank() || !p.address.isNullOrBlank()) {
            item {
                ProfileContactCard(phone = p.phone, address = p.address)
            }
        }

        // ─── INFO section ─────────────────────────────────────────────
        item {
            InfoSectionLabel("INFO")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column {
                    InfoRow(
                        icon = Icons.Outlined.AttachFile,
                        label = "Pièces jointes",
                        count = state.attachments.size,
                        expanded = attachmentsExpanded,
                        onClick = { attachmentsExpanded = !attachmentsExpanded }
                    )
                    if (state.canViewRecords) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                        InfoRow(
                            icon = Icons.Outlined.Schedule,
                            label = "Mouvements",
                            count = state.mouvements.size,
                            expanded = mouvementsExpanded,
                            onClick = { mouvementsExpanded = !mouvementsExpanded }
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                        InfoRow(
                            icon = Icons.Outlined.Report,
                            label = "Comportements",
                            count = state.comportements.size,
                            expanded = comportementsExpanded,
                            onClick = { comportementsExpanded = !comportementsExpanded }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ─── Attachments content ──────────────────────────────────────
        if (attachmentsExpanded) {
            if (state.attachments.isEmpty()) {
                item { ProfileEmptySection("Aucune pièce jointe") }
            } else {
                items(state.attachments, key = { it.id }) { att ->
                    ProfileAttachmentItem(
                        attachment = att,
                        onDownload = {
                            val url = personnelAttachmentDownloadUrl(p.id, att.id)
                            openUrl(context, url)
                        },
                        onDelete = onDeleteAttachment?.let { del -> { del(att) } }
                    )
                }
            }
        }

        // ─── Mouvements content (permission-gated) ────────────────────
        if (mouvementsExpanded && state.canViewRecords) {
            if (state.mouvements.isEmpty()) {
                item { ProfileEmptySection("Aucun mouvement") }
            } else {
                items(state.mouvements, key = { it.id }) { mvt ->
                    ProfileMouvementItem(mvt)
                }
            }
        }

        // ─── Comportements content (permission-gated) ─────────────────
        if (comportementsExpanded && state.canViewRecords) {
            if (state.comportements.isEmpty()) {
                item { ProfileEmptySection("Aucun comportement") }
            } else {
                items(state.comportements, key = { it.id }) { comp ->
                    ProfileComportementItem(comp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Full-screen photo viewer (like social media)
    if (showPhotoViewer && photoUrl != null) {
        ProfilePhotoViewerDialog(
            photoUrl = photoUrl,
            name = "${p.firstname} ${p.lastname}",
            onDismiss = { showPhotoViewer = false }
        )
    }
}

// ─── Profile header ─────────────────────────────────────────────────────────

// Gmail-style palette for letter avatars
private val ProfileAvatarColors = listOf(
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF009688), Color(0xFF4CAF50),
    Color(0xFFFF9800), Color(0xFF795548)
)

/** Letter avatar using the first letter of the lastname (Contacts/Gmail style). */
@Composable
private fun ProfileLetterAvatar(name: String, size: androidx.compose.ui.unit.Dp) {
    val initial = name.split(" ").lastOrNull()?.firstOrNull()?.uppercase() ?: "?"
    val color = ProfileAvatarColors[kotlin.math.abs(name.hashCode()) % ProfileAvatarColors.size]
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileHeader(
    photoUrl: String?,
    name: String,
    grade: String,
    im: String,
    affectation: String?,
    status: String,
    onPhotoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large circular photo — tap to view full screen
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPhotoClick),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                ProfileLetterAvatar(name = name, size = 120.dp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = grade,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!affectation.isNullOrBlank()) {
            Text(
                text = affectation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "IM: $im",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            StatusBadge(status = status)
        }
    }
}

// ─── Circular action buttons ────────────────────────────────────────────────

@Composable
private fun ProfileActionButtonsRow(
    showEdit: Boolean,
    onEdit: () -> Unit,
    showAddAttachment: Boolean,
    onAddAttachment: () -> Unit,
    showRecords: Boolean,
    onShowMouvements: () -> Unit,
    onShowComportements: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp, horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (showEdit) {
            ProfileActionButton(icon = Icons.Outlined.Edit, label = "Modifier", onClick = onEdit)
        }
        if (showAddAttachment) {
            ProfileActionButton(icon = Icons.Outlined.Add, label = "Pièce jointe", onClick = onAddAttachment)
        }
        if (showRecords) {
            ProfileActionButton(icon = Icons.Outlined.Schedule, label = "Mouvements", onClick = onShowMouvements)
            ProfileActionButton(icon = Icons.Outlined.Report, label = "Comportement", onClick = onShowComportements)
        }
    }
}

@Composable
private fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ─── Contact card ───────────────────────────────────────────────────────────

@Composable
private fun ProfileContactCard(phone: String?, address: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            if (!phone.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ContactPhone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (!phone.isNullOrBlank() && !address.isNullOrBlank()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )
            }
            if (!address.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ─── INFO section ───────────────────────────────────────────────────────────

@Composable
private fun InfoSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation)
        )
    }
}

// ─── Full-screen photo viewer ───────────────────────────────────────────────

@Composable
private fun ProfilePhotoViewerDialog(
    photoUrl: String,
    name: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss)
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Fermer",
                    tint = Color.White
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }
    }
}

// ─── Section content ────────────────────────────────────────────────────────

@Composable
private fun ProfileEmptySection(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ProfileAttachmentItem(
    attachment: PersonnelAttachment,
    onDownload: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(attachment.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = attachment.originalFilename,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileMouvementItem(mvt: Mouvement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mvt.typeMouvement,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = if (mvt.isReturned) "present" else "absent")
            }
            Text(
                text = "Départ: ${formatDateDisplay(mvt.dateDepart)} • Retour: ${formatDateDisplay(mvt.dateRetour)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileComportementItem(comp: Comportement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = comp.type,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Date: ${formatDateDisplay(comp.dateComportement)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Motif: ${comp.motif}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            comp.decision?.let {
                Text(
                    text = "Décision: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
