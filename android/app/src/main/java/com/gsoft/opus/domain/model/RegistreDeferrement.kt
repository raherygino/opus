package com.gsoft.opus.domain.model

/**
 * RegistreDeferrement — deferment/handing-over registry entry (Police Judiciaire).
 */
data class RegistreDeferrement(
    val id: Int,
    val numero: String,
    val dateHeureDeferrement: String,
    val personneNom: String,
    val dateLieuNaissance: String?,
    val infraction: String?,
    val numeroDossier: String?,
    val autorite: String?,
    val destination: String?,
    val escorte: String?,
    val suiteDonnee: String?,
    val observations: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<RegistreDeferrementAttachment> = emptyList()
)

data class RegistreDeferrementAttachment(
    val id: Int,
    val registreDeferrementId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
