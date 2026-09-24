package com.gsoft.opus.domain.model

/**
 * DispositifExceptionnel — exceptional security operation (Service Général).
 *
 * Sector allocations ("Effectif engagé") live in [effectifs].
 */
data class DispositifExceptionnel(
    val id: Int,
    val natureEvenement: String,
    val dateDebut: String,
    val dateFin: String,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String?,
    val agentPrenoms: String?,
    val agentNom: String?,
    val effectifs: List<DispositifEffectif> = emptyList()
) {
    /** Display name of the agent who recorded the entry. */
    val agentDisplayName: String
        get() = listOfNotNull(agentPrenoms, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { agentUsername ?: "" }
}

/** One "Effectif engagé" sector row of a dispositif exceptionnel. */
data class DispositifEffectif(
    val id: Int,
    val secteur: String,
    val chefElementContact: String?,
    val controleContact: String?,
    val materielsArmements: String?,
    val missions: String?,
    val createdAt: String? = null
)
