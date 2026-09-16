package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Arrestation DTOs (Police Judiciaire)
// ========================

data class ArrestationDto(
    @SerializedName("id") val id: Int,
    @SerializedName("numero") val numero: String,
    @SerializedName("date_heure_arrestation") val dateHeureArrestation: String,
    @SerializedName("personne_nom") val personneNom: String,
    @SerializedName("lieu_arrestation") val lieuArrestation: String? = null,
    @SerializedName("motif") val motif: String? = null,
    @SerializedName("policiers") val policiers: String? = null,
    @SerializedName("numero_dossier") val numeroDossier: String? = null,
    @SerializedName("observations") val observations: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<ArrestationAttachmentDto>? = null
)

data class ArrestationRequest(
    @SerializedName("numero") val numero: String?,
    @SerializedName("date_heure_arrestation") val dateHeureArrestation: String,
    @SerializedName("personne_nom") val personneNom: String,
    @SerializedName("lieu_arrestation") val lieuArrestation: String?,
    @SerializedName("motif") val motif: String?,
    @SerializedName("policiers") val policiers: String?,
    @SerializedName("numero_dossier") val numeroDossier: String?,
    @SerializedName("observations") val observations: String?
)

data class ArrestationAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("arrestation_id") val arrestationId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

/** Response from /api/arrestations/next-number. */
data class ArrestationNextNumberDto(
    @SerializedName("numero") val numero: String
)

fun ArrestationDto.toDomain() = com.gsoft.opus.domain.model.Arrestation(
    id = id,
    numero = numero,
    dateHeureArrestation = dateHeureArrestation,
    personneNom = personneNom,
    lieuArrestation = lieuArrestation,
    motif = motif,
    policiers = policiers,
    numeroDossier = numeroDossier,
    observations = observations,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun ArrestationAttachmentDto.toDomain() = com.gsoft.opus.domain.model.ArrestationAttachment(
    id = id,
    arrestationId = arrestationId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
