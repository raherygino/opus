package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// PersonneRecherchee DTOs (Police Judiciaire)
// ========================

data class PersonneRechercheeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nom") val nom: String,
    @SerializedName("adresse") val adresse: String? = null,
    @SerializedName("motif") val motif: String,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("photo_count") val photoCount: Int = 0,
    @SerializedName("photos") val photos: List<PersonneRechercheePhotoDto>? = null
)

data class PersonneRechercheeRequest(
    @SerializedName("nom") val nom: String,
    @SerializedName("adresse") val adresse: String?,
    @SerializedName("motif") val motif: String
)

data class PersonneRechercheePhotoDto(
    @SerializedName("id") val id: Int,
    @SerializedName("personne_recherchee_id") val personneRechercheeId: Int,
    @SerializedName("caption") val caption: String? = null,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("capture_source") val captureSource: String? = null,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null
)

data class PersonneRechercheePhotoCaptionRequest(
    @SerializedName("caption") val caption: String?
)

fun PersonneRechercheeDto.toDomain() = com.gsoft.opus.domain.model.PersonneRecherchee(
    id = id,
    nom = nom,
    adresse = adresse,
    motif = motif,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    photoCount = photoCount,
    photos = photos?.map { it.toDomain() } ?: emptyList()
)

fun PersonneRechercheePhotoDto.toDomain() = com.gsoft.opus.domain.model.PersonneRechercheePhoto(
    id = id,
    personneRechercheeId = personneRechercheeId,
    caption = caption,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    width = width,
    height = height,
    captureSource = captureSource,
    sortOrder = sortOrder,
    createdAt = createdAt
)
