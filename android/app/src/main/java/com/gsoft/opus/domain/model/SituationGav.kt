package com.gsoft.opus.domain.model

/**
 * Situation GAV — contrôle d'une personne en garde à vue (Sédentaire > Poste).
 *
 * References an existing [GardeAVue] record (personne concernée) and an
 * optional [Personnel] record (agent ayant effectué le contrôle). Identity
 * fields are joined server-side, never duplicated.
 */
data class SituationGav(
    val id: Int,
    val gardeAVueId: Int,
    val dateControle: String?,
    val agentControleId: Int?,
    val etatGeneral: String?,
    val observations: String?,
    val mesuresPrises: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    /** Joined from garde_a_vue (personne concernée). */
    val personneNom: String? = null,
    val personnePrenoms: String? = null,
    val personneDebutGav: String? = null,
    val personneFinGav: String? = null,
    /** Joined from personnel (agent ayant effectué le contrôle). */
    val agentControleGrade: String? = null,
    val agentControleNom: String? = null,
    val agentControlePrenoms: String? = null,
    val agentControleIm: String? = null,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<SituationGavAttachment> = emptyList()
)

data class SituationGavAttachment(
    val id: Int,
    val situationGavId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
