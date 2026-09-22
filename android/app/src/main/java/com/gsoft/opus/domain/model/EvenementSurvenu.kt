package com.gsoft.opus.domain.model

/**
 * EvenementSurvenu — event that occurred on public roads (Service Général).
 *
 * Single-record feature: the identities of the parties involved
 * (auteurs présumés / victimes / témoins) are stored as text fields
 * directly on the record.
 */
data class EvenementSurvenu(
    val id: Int,
    val dateEvenement: String,
    val heureEvenement: String,
    val typeEvenement: String,
    val lieuExact: String,
    val auteursPresumes: String?,
    val victimes: String?,
    val temoins: String?,
    val mesuresPrises: String?,
    /** GPS latitude captured at record time (mobile only, null on desktop). */
    val latitude: Double?,
    /** GPS longitude captured at record time (mobile only, null on desktop). */
    val longitude: Double?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String?,
    val agentPrenoms: String?,
    val agentNom: String?
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heureDisplay: String get() = heureEvenement.take(5)

    /**
     * The type is stored as the catalog label verbatim. TYPE_LABELS only
     * maps legacy machine codes for rows created before the catalog.
     */
    val typeLabel: String get() = TYPE_LABELS[typeEvenement] ?: typeEvenement

    /** Display name of the agent who recorded the entry. */
    val agentDisplayName: String
        get() = listOfNotNull(agentPrenoms, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { agentUsername ?: "" }

    companion object {
        const val TYPE_INFRACTION = "infraction"
        const val TYPE_INCIDENT = "incident"
        const val TYPE_ACCIDENT = "accident"
        const val TYPE_AUTRE = "autre"

        /** Ordered type codes for dropdowns. */
        val TYPES = listOf(TYPE_INFRACTION, TYPE_INCIDENT, TYPE_ACCIDENT, TYPE_AUTRE)

        /** French labels keyed by type code. */
        val TYPE_LABELS = mapOf(
            TYPE_INFRACTION to "Infraction",
            TYPE_INCIDENT to "Incident",
            TYPE_ACCIDENT to "Accident",
            TYPE_AUTRE to "Autre"
        )
    }
}

/** Attached file of an évènement survenu (stored on disk server-side). */
data class EvenementSurvenuAttachment(
    val id: Int,
    val evenementId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
