package com.gsoft.opus.domain.model

/**
 * Garde à Vue — police custody record (Police Judiciaire).
 *
 * Tracks the full GAV lifecycle for a person: identity, investigation,
 * health/rights, and the start/end/prolongation datetimes.
 */
data class GardeAVue(
    val id: Int,
    val nom: String,
    val prenoms: String?,
    val dateNaissance: String?,
    val adresse: String?,
    val enqueteurPermance: String?,
    val opjGav: String?,
    val motif: String?,
    val etatSante: String?,
    val droitsNotifies: String?,
    val personneContacter: String?,
    val debutGav: String?,
    val finGav: String?,
    val prolongationGav: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<GardeAVueAttachment> = emptyList()
)

data class GardeAVueAttachment(
    val id: Int,
    val gardeAVueId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
