package com.gsoft.opus.domain.model

data class Correspondance(
    val id: Int,
    val dateCorrespondance: String,
    val heureEnregistrement: String,
    val sens: String,
    val reference: String,
    val emetteurDestinataire: String,
    val objet: String,
    val statut: String,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String?,
    val agentPrenoms: String?,
    val agentNom: String?
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heureDisplay: String get() = heureEnregistrement.take(5)

    /** Display name of the agent de secrétariat who recorded the entry. */
    val agentDisplayName: String
        get() = listOfNotNull(agentPrenoms, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { agentUsername ?: "" }
}

data class CorrespondanceAttachment(
    val id: Int,
    val correspondanceId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
