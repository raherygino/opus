package com.gsoft.opus.domain.model

/**
 * ObjetSaisi — seized object (Police Judiciaire, OBJET SAISI tab).
 */
data class ObjetSaisi(
    val id: Int,
    val numeroDossier: String?,
    val motif: String,
    val typeObjet: String,
    val proprietaire: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<ObjetSaisiAttachment> = emptyList()
) {
    val typeObjetLabel: String
        get() = when (typeObjet) {
            "TELEPHONE" -> "Téléphone"
            "ORDINATEUR" -> "Ordinateur"
            "VEHICULE" -> "Véhicule"
            "DOCUMENT" -> "Document"
            "ARGENT" -> "Argent"
            "ARME" -> "Arme"
            "EFFETS_PERSONNELS" -> "Effets personnels"
            "AUTRE" -> "Autre"
            else -> typeObjet
        }
}

data class ObjetSaisiAttachment(
    val id: Int,
    val objetSaisiId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)

/**
 * ObjetTrouve — found object (Police Judiciaire, OBJET TROUVÉ tab).
 */
data class ObjetTrouve(
    val id: Int,
    val affaire: String,
    val motifDecouverte: String,
    val restitution: Boolean,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<ObjetTrouveAttachment> = emptyList()
) {
    val motifDecouverteLabel: String
        get() = when (motifDecouverte) {
            "REQUISITION" -> "Réquisition"
            "SUR_PERSONNE" -> "Sur une personne"
            "PERQUISITION" -> "Perquisition"
            else -> motifDecouverte
        }
}

data class ObjetTrouveAttachment(
    val id: Int,
    val objetTrouveId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
