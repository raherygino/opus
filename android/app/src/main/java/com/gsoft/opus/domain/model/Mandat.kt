package com.gsoft.opus.domain.model

/**
 * Mandat — judicial warrant (Police Judiciaire).
 */
data class Mandat(
    val id: Int,
    val numero: String,
    val type: String,
    val autorite: String?,
    val personneNom: String,
    val dateLieuNaissance: String?,
    val motif: String?,
    val qualificationInfraction: String?,
    val opjExecution: String?,
    val dateHeureExecution: String?,
    val lieuExecution: String?,
    val observations: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<MandatAttachment> = emptyList()
)

data class MandatAttachment(
    val id: Int,
    val mandatId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
