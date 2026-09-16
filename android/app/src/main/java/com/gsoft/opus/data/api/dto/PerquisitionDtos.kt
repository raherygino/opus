package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Perquisition DTOs (Police Judiciaire)
// ========================

data class PerquisitionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("numero") val numero: String,
    @SerializedName("numero_ttr") val numeroTtr: String? = null,
    @SerializedName("substitut") val substitut: String? = null,
    @SerializedName("affaire") val affaire: String,
    @SerializedName("motif") val motif: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<PerquisitionAttachmentDto>? = null
)

data class PerquisitionRequest(
    @SerializedName("numero") val numero: String?,
    @SerializedName("numero_ttr") val numeroTtr: String?,
    @SerializedName("substitut") val substitut: String?,
    @SerializedName("affaire") val affaire: String,
    @SerializedName("motif") val motif: String?
)

data class PerquisitionAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("perquisition_id") val perquisitionId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

/** Response from /api/perquisitions/next-number. */
data class PerquisitionNextNumberDto(
    @SerializedName("numero") val numero: String
)

fun PerquisitionDto.toDomain() = com.gsoft.opus.domain.model.Perquisition(
    id = id,
    numero = numero,
    numeroTtr = numeroTtr,
    substitut = substitut,
    affaire = affaire,
    motif = motif,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun PerquisitionAttachmentDto.toDomain() = com.gsoft.opus.domain.model.PerquisitionAttachment(
    id = id,
    perquisitionId = perquisitionId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
