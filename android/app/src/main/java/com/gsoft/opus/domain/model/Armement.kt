package com.gsoft.opus.domain.model

/**
 * Armement — perception / réintégration d'une arme (Sédentaire > Poste).
 * A weapon is "en cours de perception" while [heureReintegration] is null
 * and "réintégrée" once the reintegration fields are filled.
 */
data class Armement(
    val id: Int,
    val datePerception: String,
    val heurePerception: String,
    val agentPreneurPersonnelId: Int?,
    val agentPreneurIm: String?,
    val agentPreneurGrade: String?,
    val agentPreneurNom: String?,
    /** FK to the exact arme perceived (nullable for legacy records). */
    val armeId: Int?,
    val typeArme: String,
    val matriculeArme: String,
    val munitions: Int?,
    val secteurMission: String?,
    val etatPerception: String?,
    /** Whether the agent preneur identity was verified via code secret. */
    val agentVerifie: Boolean,
    /** When the agent identity was verified (timestamp string). */
    val agentVerifieAt: String?,
    /** SVG vector data of the agent signature captured at perception. */
    val signatureSvg: String?,
    /** GPS latitude captured at perception time (mobile only, null on desktop). */
    val latitude: Double?,
    /** GPS longitude captured at perception time (mobile only, null on desktop). */
    val longitude: Double?,
    val heureReintegration: String?,
    val dateReintegration: String?,
    val etatReintegration: String?,
    val munitionsConsommees: Int?,
    /** GPS latitude captured at reintegration (mobile only, null on desktop). */
    val reintegrationLatitude: Double?,
    /** GPS longitude captured at reintegration (mobile only, null on desktop). */
    val reintegrationLongitude: Double?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heurePerceptionDisplay: String get() = heurePerception.take(5)

    /** Reintegration time displayed as "HH:MM", null while en cours. */
    val heureReintegrationDisplay: String? get() = heureReintegration?.take(5)

    /** Whether the weapon has been returned (one-way transition). */
    val isReintegree: Boolean get() = heureReintegration != null

    /** Display name of the agent preneur. */
    val agentPreneurDisplay: String
        get() = listOfNotNull(agentPreneurGrade, agentPreneurNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    /** Weapon display ("type — matricule"). */
    val armeDisplay: String
        get() = listOf(typeArme, matriculeArme)
            .filter { it.isNotBlank() }
            .joinToString(" — ")

    /** Remaining rounds once reintegrated, when both counts are known. */
    val munitionsRestantes: Int?
        get() = if (munitions != null && munitionsConsommees != null) munitions - munitionsConsommees else null
}

data class ArmementAttachment(
    val id: Int,
    val armementId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
