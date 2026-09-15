package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Requisition DTOs (Police Judiciaire)
// ========================

data class RequisitionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("date_requisition") val dateRequisition: String,
    @SerializedName("numero") val numero: String,
    @SerializedName("numero_ttr") val numeroTtr: String? = null,
    @SerializedName("nom_substitut") val nomSubstitut: String? = null,
    @SerializedName("affaire") val affaire: String,
    @SerializedName("numero_dossier") val numeroDossier: String? = null,
    @SerializedName("opj") val opj: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<RequisitionAttachmentDto>? = null
)

data class RequisitionRequest(
    @SerializedName("type") val type: String,
    @SerializedName("date_requisition") val dateRequisition: String,
    @SerializedName("numero") val numero: String?,
    @SerializedName("numero_ttr") val numeroTtr: String?,
    @SerializedName("nom_substitut") val nomSubstitut: String?,
    @SerializedName("affaire") val affaire: String,
    @SerializedName("numero_dossier") val numeroDossier: String?,
    @SerializedName("opj") val opj: String?
)

data class RequisitionAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("requisition_id") val requisitionId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class RequisitionNextNumberDto(
    @SerializedName("numero") val numero: String
)

fun RequisitionDto.toDomain() = com.gsoft.opus.domain.model.Requisition(
    id = id,
    type = type,
    dateRequisition = dateRequisition,
    numero = numero,
    numeroTtr = numeroTtr,
    nomSubstitut = nomSubstitut,
    affaire = affaire,
    numeroDossier = numeroDossier,
    opj = opj,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun RequisitionAttachmentDto.toDomain() = com.gsoft.opus.domain.model.RequisitionAttachment(
    id = id,
    requisitionId = requisitionId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
