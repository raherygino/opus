package com.gsoft.opus.domain.model

/**
 * MaterielRoulant — vehicle perception & reintegration (VHL / Moto).
 * Sédentaire > Poste > Matériel roulant.
 *
 * A vehicle is "En service" while [heureReintegration] is null
 * and "Réintégré" once the reintegration fields are filled.
 * Reintegration is a one-way transition.
 */
data class MaterielRoulant(
    val id: Int,
    val datePerception: String,
    val heurePerception: String,
    val typeMateriel: String,
    val numeroImmatriculation: String?,
    val descriptionVehicule: String?,
    val agentConducteurPersonnelId: Int?,
    val agentConducteurIm: String?,
    val agentConducteurGrade: String?,
    val agentConducteurNom: String?,
    val chefDeBordPersonnelId: Int?,
    val chefDeBordIm: String?,
    val chefDeBordGrade: String?,
    val chefDeBordNom: String?,
    val kilometrageDepart: String?,
    val niveauCarburantDepart: String?,
    val heureReintegration: String?,
    val dateReintegration: String?,
    val kilometrageRetour: String?,
    val niveauCarburantRetour: String?,
    val observationsTechniques: String?,
    val defaillances: String?,
    val agentVerifie: Boolean,
    val agentVerifieAt: String?,
    val signatureSvg: String?,
    val statut: String,
    val createdBy: Int?,
    val attachments: List<MaterielRoulantAttachment> = emptyList(),
    val createdAt: String?,
    val updatedAt: String?
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heurePerceptionDisplay: String get() = heurePerception.take(5)

    /** Reintegration time displayed as "HH:MM", null while en service. */
    val heureReintegrationDisplay: String? get() = heureReintegration?.take(5)

    /** Whether the vehicle has been returned (one-way transition). */
    val isReintegre: Boolean get() = heureReintegration != null

    /** Display name of the driver (agent conducteur). */
    val conducteurDisplay: String
        get() = listOfNotNull(agentConducteurGrade, agentConducteurNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    /** Display name of the chef de bord, if any. */
    val chefDeBordDisplay: String
        get() = listOfNotNull(chefDeBordGrade, chefDeBordNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    /** Whether defaillances were reported at reintegration. */
    val hasDefaillances: Boolean get() = !defaillances.isNullOrBlank()
}

data class MaterielRoulantAttachment(
    val id: Int,
    val materielRoulantId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
