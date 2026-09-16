package com.gsoft.opus.domain.model

/**
 * Arrestation — arrest record (Police Judiciaire).
 */
data class Arrestation(
    val id: Int,
    val numero: String,
    val dateHeureArrestation: String,
    val personneNom: String,
    val lieuArrestation: String?,
    val motif: String?,
    val policiers: String?,
    val numeroDossier: String?,
    val observations: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<ArrestationAttachment> = emptyList()
)

data class ArrestationAttachment(
    val id: Int,
    val arrestationId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
