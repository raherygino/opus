package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Renseignement PJ DTOs (Police Judiciaire)
// ========================

data class RenseignementPjDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nature_infraction") val natureInfraction: String,
    @SerializedName("date_lieu_faits") val dateLieuFaits: String? = null,
    @SerializedName("circonstances") val circonstances: String? = null,
    @SerializedName("prejudices") val prejudices: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<RenseignementPjAttachmentDto>? = null
)

data class RenseignementPjRequest(
    @SerializedName("nature_infraction") val natureInfraction: String,
    @SerializedName("date_lieu_faits") val dateLieuFaits: String?,
    @SerializedName("circonstances") val circonstances: String?,
    @SerializedName("prejudices") val prejudices: String?
)

data class RenseignementPjAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("renseignement_id") val renseignementId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

fun RenseignementPjDto.toDomain() = com.gsoft.opus.domain.model.RenseignementPj(
    id = id,
    natureInfraction = natureInfraction,
    dateLieuFaits = dateLieuFaits,
    circonstances = circonstances,
    prejudices = prejudices,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun RenseignementPjAttachmentDto.toDomain() = com.gsoft.opus.domain.model.RenseignementPjAttachment(
    id = id,
    renseignementId = renseignementId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
