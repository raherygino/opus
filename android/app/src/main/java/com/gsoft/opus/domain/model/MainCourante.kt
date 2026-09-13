package com.gsoft.opus.domain.model

/**
 * MainCourante — event logbook (Sédentaire > Secrétariat & Poste).
 *
 * A single table backs both contexts, distinguished by [origine].
 * Each context has its own permission module code checked client-side.
 */
data class MainCourante(
    val id: Int,
    val dateEvenement: String,
    val heureEvenement: String,
    val categorie: String,
    val description: String,
    val origine: String,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String?,
    val agentPrenoms: String?,
    val agentNom: String?,
    val attachments: List<MainCouranteAttachment> = emptyList()
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heureDisplay: String get() = heureEvenement.take(5)

    /** Display name of the agent who recorded the entry. */
    val agentDisplayName: String
        get() = listOfNotNull(agentPrenoms, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { agentUsername ?: "" }
}

data class MainCouranteAttachment(
    val id: Int,
    val mainCouranteId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
