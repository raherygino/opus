package com.gsoft.opus.domain.model

/**
 * RenseignementPj — renseignement judiciaire (Police Judiciaire).
 */
data class RenseignementPj(
    val id: Int,
    val natureInfraction: String,
    val dateLieuFaits: String?,
    val circonstances: String?,
    val prejudices: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<RenseignementPjAttachment> = emptyList()
)

data class RenseignementPjAttachment(
    val id: Int,
    val renseignementId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
