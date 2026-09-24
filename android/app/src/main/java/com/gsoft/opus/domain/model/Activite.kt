package com.gsoft.opus.domain.model

/**
 * Activite — patrol / intervention record (Service Général).
 *
 * Patrol itineraries live in six nullable fields (one per type×mode pair).
 * A null itinerary means the patrol mode was not selected; an empty string
 * means the mode was selected without a detailed itinerary.
 */
data class Activite(
    val id: Int,
    val dateActivite: String,
    val heureActivite: String,
    val patrouilleDiurneMotoriseeItineraire: String?,
    val patrouilleDiurnePedestreItineraire: String?,
    val patrouilleDiurnePorteeItineraire: String?,
    val patrouilleNocturneMotoriseeItineraire: String?,
    val patrouilleNocturnePedestreItineraire: String?,
    val patrouilleNocturnePorteeItineraire: String?,
    val operationCiblee: String?,
    val faitsConstates: String?,
    val compteRenduHierarchie: String?,
    val conduiteATenir: String?,
    val natureIntervention: String?,
    val suitesDonnees: String?,
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
    val heureDisplay: String get() = heureActivite.take(5)

    /** Display name of the agent who recorded the entry. */
    val agentDisplayName: String
        get() = listOfNotNull(agentPrenoms, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { agentUsername ?: "" }

    /** Itinerary stored for the given patrol type/mode (null = not selected). */
    fun itineraire(type: String, mode: String): String? = when (type to mode) {
        "diurne" to "motorisee" -> patrouilleDiurneMotoriseeItineraire
        "diurne" to "pedestre" -> patrouilleDiurnePedestreItineraire
        "diurne" to "portee" -> patrouilleDiurnePorteeItineraire
        "nocturne" to "motorisee" -> patrouilleNocturneMotoriseeItineraire
        "nocturne" to "pedestre" -> patrouilleNocturnePedestreItineraire
        "nocturne" to "portee" -> patrouilleNocturnePorteeItineraire
        else -> null
    }

    /** Whether at least one patrol mode was selected. */
    val hasPatrouille: Boolean
        get() = listOf(
            patrouilleDiurneMotoriseeItineraire, patrouilleDiurnePedestreItineraire,
            patrouilleDiurnePorteeItineraire, patrouilleNocturneMotoriseeItineraire,
            patrouilleNocturnePedestreItineraire, patrouilleNocturnePorteeItineraire
        ).any { it != null }

    /**
     * Short summary for list rows, e.g. "Diurne motorisée • Nocturne portée".
     * Empty when no patrol mode was selected.
     */
    val patrouilleSummary: String
        get() = PATROUILLE_TYPES.flatMap { (type, typeLabel) ->
            PATROUILLE_MODES.mapNotNull { (mode, modeLabel) ->
                if (itineraire(type, mode) != null) "$typeLabel $modeLabel" else null
            }
        }.joinToString(" • ")

    companion object {
        /** Patrol types in display order: code → French label. */
        val PATROUILLE_TYPES = listOf(
            "diurne" to "Diurne",
            "nocturne" to "Nocturne"
        )

        /** Patrol modes in display order: code → French label. */
        val PATROUILLE_MODES = listOf(
            "motorisee" to "Motorisée",
            "pedestre" to "Pédestre",
            "portee" to "Portée"
        )
    }
}

/** Attached file of an activité (stored on disk server-side). */
data class ActiviteAttachment(
    val id: Int,
    val activiteId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
