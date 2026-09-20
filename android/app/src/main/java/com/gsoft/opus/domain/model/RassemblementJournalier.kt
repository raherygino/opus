package com.gsoft.opus.domain.model

/**
 * RassemblementJournalier — daily briefing record (Service Général).
 *
 * The situation de prise d'arme (weapon-taking roll call) is stored directly
 * on the record. Sector allocations live in [repartitions]; the Diurne and
 * Nocturne sections share the exact same structure and only differ by
 * [RepartitionSecteur.type].
 */
data class RassemblementJournalier(
    val id: Int,
    val dateRassemblement: String,
    val heureRassemblement: String,
    val brigadeService: String,
    val officierPermanence: String?,
    val inspecteurPermanence: String?,
    val chefPoste: String?,
    val instructionsAutorite: String?,
    val effectifTheorique: Int,
    val present: Int,
    val absent: Int,
    val motifAbsence: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String?,
    val agentPrenoms: String?,
    val agentNom: String?,
    val repartitions: List<RepartitionSecteur> = emptyList()
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heureDisplay: String get() = heureRassemblement.take(5)

    /** Display name of the agent who recorded the entry. */
    val agentDisplayName: String
        get() = listOfNotNull(agentPrenoms, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { agentUsername ?: "" }
}

/** One sector-allocation row — Diurne and Nocturne share this structure. */
data class RepartitionSecteur(
    val id: Int,
    val type: String,
    val secteur: String,
    val effectifEngage: String?,
    val chefElementContact: String?,
    val controleContact: String?,
    val materielsArmements: String?,
    val missions: String?,
    val createdAt: String? = null
) {
    val isDiurne: Boolean get() = type == TYPE_DIURNE

    companion object {
        const val TYPE_DIURNE = "diurne"
        const val TYPE_NOCTURNE = "nocturne"
    }
}
