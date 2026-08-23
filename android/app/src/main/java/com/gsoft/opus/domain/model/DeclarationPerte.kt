package com.gsoft.opus.domain.model

data class DeclarationPerte(
    val id: Int,
    val dateDeclaration: String,
    val heureDeclaration: String,
    val identiteDeclarant: String,
    val natureObjet: String,
    val descriptionObjet: String,
    val datePerte: String,
    val lieuPerte: String,
    val numeroAttestation: String,
    val nomAgent: String,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String?,
    val agentPrenoms: String?,
    val agentNom: String?
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heureDisplay: String get() = heureDeclaration.take(5)

    /** Display name of the agent de secrétariat who recorded the entry. */
    val agentDisplayName: String
        get() = listOfNotNull(agentPrenoms, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { agentUsername ?: "" }
}

data class DeclarationPerteAttachment(
    val id: Int,
    val declarationId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
