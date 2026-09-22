package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Évènements survenus DTOs (Service Général)
// ========================

data class EvenementSurvenuDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_evenement") val dateEvenement: String,
    @SerializedName("heure_evenement") val heureEvenement: String,
    @SerializedName("type_evenement") val typeEvenement: String,
    @SerializedName("lieu_exact") val lieuExact: String,
    @SerializedName("auteurs_presumes") val auteursPresumes: String? = null,
    @SerializedName("victimes") val victimes: String? = null,
    @SerializedName("temoins") val temoins: String? = null,
    @SerializedName("mesures_prises") val mesuresPrises: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<EvenementSurvenuAttachmentDto>? = null
)

data class EvenementSurvenuRequest(
    @SerializedName("date_evenement") val dateEvenement: String,
    @SerializedName("heure_evenement") val heureEvenement: String,
    @SerializedName("type_evenement") val typeEvenement: String,
    @SerializedName("lieu_exact") val lieuExact: String,
    @SerializedName("auteurs_presumes") val auteursPresumes: String? = null,
    @SerializedName("victimes") val victimes: String? = null,
    @SerializedName("temoins") val temoins: String? = null,
    @SerializedName("mesures_prises") val mesuresPrises: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class EvenementSurvenuAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("evenement_id") val evenementId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
