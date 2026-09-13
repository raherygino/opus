package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// MainCourante DTOs (event logbook — Sédentaire > Secrétariat & Poste)
// ========================

data class MainCouranteDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_evenement") val dateEvenement: String,
    @SerializedName("heure_evenement") val heureEvenement: String,
    @SerializedName("categorie") val categorie: String,
    @SerializedName("description") val description: String,
    @SerializedName("origine") val origine: String,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<MainCouranteAttachmentDto>? = null
)

data class MainCouranteRequest(
    @SerializedName("date_evenement") val dateEvenement: String,
    @SerializedName("heure_evenement") val heureEvenement: String,
    @SerializedName("categorie") val categorie: String,
    @SerializedName("description") val description: String,
    @SerializedName("origine") val origine: String
)

data class MainCouranteAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("main_courante_id") val mainCouranteId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// ========================
// MainCourante Categorie DTO (user-managed label catalog)
// ========================

data class MainCouranteCategorieDto(
    @SerializedName("id") val id: Int,
    @SerializedName("label") val label: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class MainCouranteCategorieRequest(
    @SerializedName("label") val label: String
)
