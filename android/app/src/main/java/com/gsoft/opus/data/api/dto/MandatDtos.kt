package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Mandat DTOs (Police Judiciaire)
// ========================

data class MandatDto(
    @SerializedName("id") val id: Int,
    @SerializedName("numero") val numero: String,
    @SerializedName("type") val type: String,
    @SerializedName("autorite") val autorite: String? = null,
    @SerializedName("personne_nom") val personneNom: String,
    @SerializedName("date_lieu_naissance") val dateLieuNaissance: String? = null,
    @SerializedName("motif") val motif: String? = null,
    @SerializedName("qualification_infraction") val qualificationInfraction: String? = null,
    @SerializedName("opj_execution") val opjExecution: String? = null,
    @SerializedName("date_heure_execution") val dateHeureExecution: String? = null,
    @SerializedName("lieu_execution") val lieuExecution: String? = null,
    @SerializedName("observations") val observations: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<MandatAttachmentDto>? = null
)

data class MandatRequest(
    @SerializedName("numero") val numero: String?,
    @SerializedName("type") val type: String,
    @SerializedName("autorite") val autorite: String?,
    @SerializedName("personne_nom") val personneNom: String,
    @SerializedName("date_lieu_naissance") val dateLieuNaissance: String?,
    @SerializedName("motif") val motif: String?,
    @SerializedName("qualification_infraction") val qualificationInfraction: String?,
    @SerializedName("opj_execution") val opjExecution: String?,
    @SerializedName("date_heure_execution") val dateHeureExecution: String?,
    @SerializedName("lieu_execution") val lieuExecution: String?,
    @SerializedName("observations") val observations: String?
)

data class MandatAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("mandat_id") val mandatId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

/** Response from /api/mandats/next-number. */
data class MandatNextNumberDto(
    @SerializedName("numero") val numero: String
)

fun MandatDto.toDomain() = com.gsoft.opus.domain.model.Mandat(
    id = id,
    numero = numero,
    type = type,
    autorite = autorite,
    personneNom = personneNom,
    dateLieuNaissance = dateLieuNaissance,
    motif = motif,
    qualificationInfraction = qualificationInfraction,
    opjExecution = opjExecution,
    dateHeureExecution = dateHeureExecution,
    lieuExecution = lieuExecution,
    observations = observations,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun MandatAttachmentDto.toDomain() = com.gsoft.opus.domain.model.MandatAttachment(
    id = id,
    mandatId = mandatId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
