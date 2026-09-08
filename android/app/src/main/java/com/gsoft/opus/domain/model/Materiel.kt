package com.gsoft.opus.domain.model

/**
 * TypeMateriel — equipment type catalog (e.g. Radio, Bâton, Gilet, Menottes).
 * Sédentaire > Poste > Matériels.
 */
data class TypeMateriel(
    val id: Int,
    val nom: String,
    val description: String?,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * One material line item within an assignment (type + unique ID + states).
 */
data class AffectationMaterielLigne(
    val id: Int,
    val affectationId: Int,
    val typeMaterielId: Int,
    /** Snapshot of the type name at assignment time. */
    val typeMaterielNom: String,
    /** Condition state at issue (perception). */
    val etatEmport: String?,
    /** Condition state at return (réintégration). Null until returned. */
    val etatReintegration: String?,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * AffectationMateriel — equipment assignment header (agent + perception/reintegration dates + lignes).
 * An assignment is "Assigné" while [heureReintegration] is null
 * and "Réintégré" once the reintegration fields are filled.
 */
data class AffectationMateriel(
    val id: Int,
    val agentPersonnelId: Int,
    val agentIm: String?,
    val agentGrade: String?,
    val agentNom: String?,
    val datePerception: String,
    val heurePerception: String,
    val dateReintegration: String?,
    val heureReintegration: String?,
    val statut: String,
    val observations: String?,
    val agentVerifie: Boolean = false,
    val agentVerifieAt: String? = null,
    val signatureSvg: String? = null,
    val createdBy: Int?,
    val lignes: List<AffectationMaterielLigne> = emptyList(),
    val createdAt: String?,
    val updatedAt: String?
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heurePerceptionDisplay: String get() = heurePerception.take(5)

    /** Reintegration time displayed as "HH:MM", null while en cours. */
    val heureReintegrationDisplay: String? get() = heureReintegration?.take(5)

    /** Whether the material has been returned (one-way transition). */
    val isReintegre: Boolean get() = heureReintegration != null

    /** Display name of the agent. */
    val agentDisplay: String
        get() = listOfNotNull(agentGrade, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    /** Summary of material types in this assignment. */
    val materielsSummary: String
        get() = lignes.joinToString(", ") { it.typeMaterielNom }
}
