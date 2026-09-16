package com.gsoft.opus.domain.model

/**
 * Perquisition — search warrant (Police Judiciaire).
 */
data class Perquisition(
    val id: Int,
    val numero: String,
    val numeroTtr: String?,
    val substitut: String?,
    val affaire: String,
    val motif: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<PerquisitionAttachment> = emptyList()
)

data class PerquisitionAttachment(
    val id: Int,
    val perquisitionId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
