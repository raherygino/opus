package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Objet Saisi DTOs (Police Judiciaire)
// ========================

data class ObjetSaisiDto(
    @SerializedName("id") val id: Int,
    @SerializedName("numero_dossier") val numeroDossier: String? = null,
    @SerializedName("motif") val motif: String,
    @SerializedName("type_objet") val typeObjet: String,
    @SerializedName("proprietaire") val proprietaire: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<ObjetSaisiAttachmentDto>? = null
)

data class ObjetSaisiRequest(
    @SerializedName("numero_dossier") val numeroDossier: String?,
    @SerializedName("motif") val motif: String,
    @SerializedName("type_objet") val typeObjet: String,
    @SerializedName("proprietaire") val proprietaire: String?
)

data class ObjetSaisiAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("objet_saisi_id") val objetSaisiId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// ========================
// Objet Trouvé DTOs (Police Judiciaire)
// ========================

data class ObjetTrouveDto(
    @SerializedName("id") val id: Int,
    @SerializedName("affaire") val affaire: String,
    @SerializedName("motif_decouverte") val motifDecouverte: String,
    @SerializedName("restitution") val restitution: Int = 0,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<ObjetTrouveAttachmentDto>? = null
)

data class ObjetTrouveRequest(
    @SerializedName("affaire") val affaire: String,
    @SerializedName("motif_decouverte") val motifDecouverte: String,
    @SerializedName("restitution") val restitution: Boolean
)

data class ObjetTrouveAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("objet_trouve_id") val objetTrouveId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

fun ObjetSaisiDto.toDomain() = com.gsoft.opus.domain.model.ObjetSaisi(
    id = id,
    numeroDossier = numeroDossier,
    motif = motif,
    typeObjet = typeObjet,
    proprietaire = proprietaire,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun ObjetSaisiAttachmentDto.toDomain() = com.gsoft.opus.domain.model.ObjetSaisiAttachment(
    id = id,
    objetSaisiId = objetSaisiId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

fun ObjetTrouveDto.toDomain() = com.gsoft.opus.domain.model.ObjetTrouve(
    id = id,
    affaire = affaire,
    motifDecouverte = motifDecouverte,
    restitution = restitution == 1,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun ObjetTrouveAttachmentDto.toDomain() = com.gsoft.opus.domain.model.ObjetTrouveAttachment(
    id = id,
    objetTrouveId = objetTrouveId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
