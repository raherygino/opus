package com.gsoft.opus.domain.model

/**
 * PersonneRecherchee — wanted person record (Police Judiciaire).
 *
 * Tracks wanted persons with a dedicated multi-image table for photos,
 * separate from the generic PJ attachment system.
 */
data class PersonneRecherchee(
    val id: Int,
    val nom: String,
    val adresse: String?,
    val motif: String,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val photoCount: Int = 0,
    val photos: List<PersonneRechercheePhoto> = emptyList()
)

data class PersonneRechercheePhoto(
    val id: Int,
    val personneRechercheeId: Int,
    val caption: String?,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val width: Int?,
    val height: Int?,
    val captureSource: String?,
    val sortOrder: Int,
    val createdAt: String?
)
