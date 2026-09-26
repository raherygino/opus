package com.gsoft.opus.domain.model

/**
 * RegistreEnquete — investigation registry entry (Police Judiciaire).
 */
data class RegistreEnquete(
    val id: Int,
    val numero: String,
    val dateOuverture: String,
    val numeroDossier: String?,
    val natureInfraction: String,
    val dateLieuFaits: String?,
    val plaignant: String?,
    val miseEnCause: String?,
    val enqueteurPersonnelId: Int?,
    val opjPersonnelId: Int?,
    val statut: String,
    val observations: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val enqueteurPrenoms: String? = null,
    val enqueteurNom: String? = null,
    val enqueteurGrade: String? = null,
    val enqueteurIm: String? = null,
    val opjPrenoms: String? = null,
    val opjNom: String? = null,
    val opjGrade: String? = null,
    val opjIm: String? = null,
    val attachments: List<RegistreEnqueteAttachment> = emptyList()
)

data class RegistreEnqueteAttachment(
    val id: Int,
    val registreEnqueteId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
