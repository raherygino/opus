package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Registre d'enquête DTOs (Police Judiciaire)
// ========================

data class RegistreEnqueteDto(
    @SerializedName("id") val id: Int,
    @SerializedName("numero") val numero: String,
    @SerializedName("date_ouverture") val dateOuverture: String,
    @SerializedName("numero_dossier") val numeroDossier: String? = null,
    @SerializedName("nature_infraction") val natureInfraction: String,
    @SerializedName("date_lieu_faits") val dateLieuFaits: String? = null,
    @SerializedName("plaignant") val plaignant: String? = null,
    @SerializedName("mise_en_cause") val miseEnCause: String? = null,
    @SerializedName("enqueteur_personnel_id") val enqueteurPersonnelId: Int? = null,
    @SerializedName("opj_personnel_id") val opjPersonnelId: Int? = null,
    @SerializedName("statut") val statut: String,
    @SerializedName("observations") val observations: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("enqueteur_prenoms") val enqueteurPrenoms: String? = null,
    @SerializedName("enqueteur_nom") val enqueteurNom: String? = null,
    @SerializedName("enqueteur_grade") val enqueteurGrade: String? = null,
    @SerializedName("enqueteur_im") val enqueteurIm: String? = null,
    @SerializedName("opj_prenoms") val opjPrenoms: String? = null,
    @SerializedName("opj_nom") val opjNom: String? = null,
    @SerializedName("opj_grade") val opjGrade: String? = null,
    @SerializedName("opj_im") val opjIm: String? = null,
    @SerializedName("attachments") val attachments: List<RegistreEnqueteAttachmentDto>? = null
)

data class RegistreEnqueteRequest(
    @SerializedName("numero") val numero: String?,
    @SerializedName("date_ouverture") val dateOuverture: String,
    @SerializedName("numero_dossier") val numeroDossier: String?,
    @SerializedName("nature_infraction") val natureInfraction: String,
    @SerializedName("date_lieu_faits") val dateLieuFaits: String?,
    @SerializedName("plaignant") val plaignant: String?,
    @SerializedName("mise_en_cause") val miseEnCause: String?,
    @SerializedName("enqueteur_personnel_id") val enqueteurPersonnelId: Int?,
    @SerializedName("opj_personnel_id") val opjPersonnelId: Int?,
    @SerializedName("statut") val statut: String?,
    @SerializedName("observations") val observations: String?
)

data class RegistreEnqueteAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("registre_enquete_id") val registreEnqueteId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

/** Response from /api/registres-enquete/next-number. */
data class RegistreEnqueteNextNumberDto(
    @SerializedName("numero") val numero: String
)

fun RegistreEnqueteDto.toDomain() = com.gsoft.opus.domain.model.RegistreEnquete(
    id = id,
    numero = numero,
    dateOuverture = dateOuverture,
    numeroDossier = numeroDossier,
    natureInfraction = natureInfraction,
    dateLieuFaits = dateLieuFaits,
    plaignant = plaignant,
    miseEnCause = miseEnCause,
    enqueteurPersonnelId = enqueteurPersonnelId,
    opjPersonnelId = opjPersonnelId,
    statut = statut,
    observations = observations,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    enqueteurPrenoms = enqueteurPrenoms,
    enqueteurNom = enqueteurNom,
    enqueteurGrade = enqueteurGrade,
    enqueteurIm = enqueteurIm,
    opjPrenoms = opjPrenoms,
    opjNom = opjNom,
    opjGrade = opjGrade,
    opjIm = opjIm,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun RegistreEnqueteAttachmentDto.toDomain() = com.gsoft.opus.domain.model.RegistreEnqueteAttachment(
    id = id,
    registreEnqueteId = registreEnqueteId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
