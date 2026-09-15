package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Convocation DTOs (Police Judiciaire — Convocation)
// ========================

data class ConvocationDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("date_convocation") val dateConvocation: String,
    @SerializedName("numero") val numero: String,
    @SerializedName("nom") val nom: String,
    @SerializedName("adresse") val adresse: String? = null,
    @SerializedName("infraction") val infraction: String? = null,
    @SerializedName("personne_accuse_recu") val personneAccuseRecu: String? = null,
    @SerializedName("numero_dossier") val numeroDossier: String? = null,
    @SerializedName("observation") val observation: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<ConvocationAttachmentDto>? = null
)

data class ConvocationRequest(
    @SerializedName("type") val type: String,
    @SerializedName("date_convocation") val dateConvocation: String,
    @SerializedName("numero") val numero: String? = null,
    @SerializedName("nom") val nom: String,
    @SerializedName("adresse") val adresse: String?,
    @SerializedName("infraction") val infraction: String?,
    @SerializedName("personne_accuse_recu") val personneAccuseRecu: String?,
    @SerializedName("numero_dossier") val numeroDossier: String?,
    @SerializedName("observation") val observation: String?
)

data class ConvocationAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("convocation_id") val convocationId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
