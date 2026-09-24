package com.gsoft.opus.presentation.dispositif

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
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gsoft.opus.domain.model.DispositifEffectif
import com.gsoft.opus.presentation.personnel.DetailRow

/**
 * UI row for an "Effectif engagé" line of a dispositif exceptionnel.
 * Same field structure as the rassemblement's repartition rows.
 */
data class EffectifRow(
    val key: Long,
    val secteur: String = "",
    val chefElementContact: String = "",
    val controleContact: String = "",
    val materielsArmements: String = "",
    val missions: String = ""
) {
    val isBlank: Boolean
        get() = listOf(
            secteur, chefElementContact, controleContact, materielsArmements, missions
        ).all { it.isBlank() }
}

/** Maps a domain effectif row to the shared UI row. */
fun DispositifEffectif.toRow(): EffectifRow = EffectifRow(
    key = id.toLong(),
    secteur = secteur,
    chefElementContact = chefElementContact.orEmpty(),
    controleContact = controleContact.orEmpty(),
    materielsArmements = materielsArmements.orEmpty(),
    missions = missions.orEmpty()
)

/**
 * The "Effectif engagé" row list — add/remove/edit rows in the form, or
 * read-only rendering in the detail screen.
 */
@Composable
fun EffectifSection(
    rows: List<EffectifRow>,
    readOnly: Boolean = false,
    invalidKeys: Set<Long> = emptySet(),
    onUpdate: ((key: Long, update: (EffectifRow) -> EffectifRow) -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    onRemove: ((key: Long) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lignes d'effectif",
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
                text = "Aucune ligne d'effectif engagé",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (readOnly) {
                    ReadOnlyEffectifRow(row)
                } else {
                    EditableEffectifRow(row, index, invalidKeys.contains(row.key), onUpdate, onRemove)
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyEffectifRow(row: EffectifRow) {
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
        DetailRow("Chef d'élément avec contact", row.chefElementContact.ifBlank { "—" })
        DetailRow("Contrôle avec contact", row.controleContact.ifBlank { "—" })
        DetailRow("Matériels et armements", row.materielsArmements.ifBlank { "—" })
        DetailRow("Missions", row.missions.ifBlank { "—" })
    }
}

@Composable
private fun EditableEffectifRow(
    row: EffectifRow,
    index: Int,
    secteurInvalid: Boolean,
    onUpdate: ((key: Long, update: (EffectifRow) -> EffectifRow) -> Unit)?,
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

        OutlinedTextField(
            value = row.secteur,
            onValueChange = { v -> onUpdate?.invoke(row.key) { it.copy(secteur = v) } },
            label = { Text("Secteur *") },
            singleLine = true,
            isError = secteurInvalid,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

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
