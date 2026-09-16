package com.gsoft.opus.domain.model

/**
 * Requisition — judicial requisition (Police Judiciaire).
 *
 * Tracks requisitions issued to external services (TPH, médecin légiste,
 * CIM, autre). The numero is auto-generated via PlainteSequence (REQ)
 * when the user does not supply one, and is user-overridable.
 */
data class Requisition(
    val id: Int,
    val type: String,
    val dateRequisition: String,
    val numero: String,
    val numeroTtr: String?,
    val nomSubstitut: String?,
    val affaire: String,
    val numeroDossier: String?,
    val opj: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<RequisitionAttachment> = emptyList()
) {
    val typeLabel: String
        get() = when (type) {
            "TPH" -> "TPH"
            "MEDECIN_LEGISTE" -> "Médecin légiste"
            "CIM" -> "CIM"
            "AUTRE" -> "Autre"
            else -> type
        }
}

data class RequisitionAttachment(
    val id: Int,
    val requisitionId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
