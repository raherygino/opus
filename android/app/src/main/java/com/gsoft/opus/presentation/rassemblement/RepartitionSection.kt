package com.gsoft.opus.presentation.rassemblement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gsoft.opus.domain.model.RepartitionSecteur
import com.gsoft.opus.presentation.personnel.DetailRow

/**
 * UI row for a repartition — the Diurne and Nocturne sections share this
 * exact structure; only the `type` value differs.
 */
data class RepartitionRow(
    val key: Long,
    val type: String,
    val secteur: String = "",
    val effectifEngage: String = "",
    val chefElementContact: String = "",
    val controleContact: String = "",
    val materielsArmements: String = "",
    val missions: String = ""
) {
    val isBlank: Boolean
        get() = listOf(
            secteur, effectifEngage, chefElementContact,
            controleContact, materielsArmements, missions
        ).all { it.isBlank() }
}

/** The two sections of the unified repartition table. */
val REPARTITION_SECTIONS: List<Triple<String, String, ImageVector>> = listOf(
    Triple(RepartitionSecteur.TYPE_DIURNE, "Diurne", Icons.Outlined.LightMode),
    Triple(RepartitionSecteur.TYPE_NOCTURNE, "Nocturne", Icons.Outlined.DarkMode)
)

/** Maps a domain repartition to the shared UI row. */
fun RepartitionSecteur.toRow(): RepartitionRow = RepartitionRow(
    key = id.toLong(),
    type = type,
    secteur = secteur,
    effectifEngage = effectifEngage.orEmpty(),
    chefElementContact = chefElementContact.orEmpty(),
    controleContact = controleContact.orEmpty(),
    materielsArmements = materielsArmements.orEmpty(),
    missions = missions.orEmpty()
)

/**
 * One section of the unified "Répartition par secteur" table. Both the
 * Diurne and Nocturne sections are generated from this single composable —
 * only the type (and its label/icon) differs.
 */
@Composable
fun RepartitionSection(
    type: String,
    label: String,
    icon: ImageVector,
    rows: List<RepartitionRow>,
    readOnly: Boolean = false,
    onUpdate: ((key: Long, update: (RepartitionRow) -> RepartitionRow) -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    onRemove: ((key: Long) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header — shared structure, only the type differs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Répartition par secteur – $label",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${rows.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onAdd != null && !readOnly) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onAdd) {
                    Text("Ajouter", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (rows.isEmpty()) {
            Text(
                text = "Aucune répartition ${label.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (readOnly) {
                    ReadOnlyRepartitionRow(row)
                } else {
                    EditableRepartitionRow(row, index, onUpdate, onRemove)
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyRepartitionRow(row: RepartitionRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Text(
            text = row.secteur.ifBlank { "Secteur" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        DetailRow("Effectif engagé", row.effectifEngage.ifBlank { "—" })
        DetailRow("Chef d'élément avec contact", row.chefElementContact.ifBlank { "—" })
        DetailRow("Contrôle avec contact", row.controleContact.ifBlank { "—" })
        DetailRow("Matériels et armements", row.materielsArmements.ifBlank { "—" })
        DetailRow("Missions", row.missions.ifBlank { "—" })
    }
}

@Composable
private fun EditableRepartitionRow(
    row: RepartitionRow,
    index: Int,
    onUpdate: ((key: Long, update: (RepartitionRow) -> RepartitionRow) -> Unit)?,
    onRemove: ((key: Long) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ligne ${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (onRemove != null) {
                IconButton(onClick = { onRemove(row.key) }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Supprimer la ligne",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = row.secteur,
                onValueChange = { v -> onUpdate?.invoke(row.key) { it.copy(secteur = v) } },
                label = { Text("Secteur *") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.4f)
            )
            OutlinedTextField(
                value = row.effectifEngage,
                onValueChange = { v -> onUpdate?.invoke(row.key) { it.copy(effectifEngage = v) } },
                label = { Text("Effectif engagé") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = row.chefElementContact,
            onValueChange = { v -> onUpdate?.invoke(row.key) { it.copy(chefElementContact = v) } },
            label = { Text("Chef d'élément avec contact") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = row.controleContact,
            onValueChange = { v -> onUpdate?.invoke(row.key) { it.copy(controleContact = v) } },
            label = { Text("Contrôle avec contact") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = row.materielsArmements,
            onValueChange = { v -> onUpdate?.invoke(row.key) { it.copy(materielsArmements = v) } },
            label = { Text("Matériels et armements") },
            minLines = 2,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = row.missions,
            onValueChange = { v -> onUpdate?.invoke(row.key) { it.copy(missions = v) } },
            label = { Text("Missions") },
            minLines = 2,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
