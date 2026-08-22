package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ─── Correspondance ───────────────────────────────────────────────────

data class CorrespondanceDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_correspondance") val dateCorrespondance: String,
    @SerializedName("heure_enregistrement") val heureEnregistrement: String,
    @SerializedName("sens") val sens: String,
    @SerializedName("reference") val reference: String,
    @SerializedName("emetteur_destinataire") val emetteurDestinataire: String,
    @SerializedName("objet") val objet: String,
    @SerializedName("statut") val statut: String = "Enregistré",
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<CorrespondanceAttachmentDto>? = null
)

data class CorrespondanceRequest(
    @SerializedName("date_correspondance") val dateCorrespondance: String,
    @SerializedName("heure_enregistrement") val heureEnregistrement: String,
    @SerializedName("sens") val sens: String,
    @SerializedName("reference") val reference: String,
    @SerializedName("emetteur_destinataire") val emetteurDestinataire: String,
    @SerializedName("objet") val objet: String,
    @SerializedName("statut") val statut: String? = null
)

data class CorrespondanceAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("correspondance_id") val correspondanceId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
