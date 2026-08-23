package com.gsoft.opus.domain.model

data class Passation(
    val id: Int,
    val datePassation: String,
    val heurePassation: String,
    val chefDescendantUserId: Int?,
    val chefDescendantGrade: String?,
    val chefDescendantLastname: String?,
    val chefMontantUserId: Int?,
    val chefMontantGrade: String?,
    val chefMontantLastname: String?,
    val instructionsAutorite: String?,
    val incidentsSurvenus: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val chefDescendantUsername: String?,
    val chefMontantUsername: String?
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heureDisplay: String get() = heurePassation.take(5)

    /** Display name of the chef de poste descendant. */
    val chefDescendantDisplay: String
        get() = listOfNotNull(chefDescendantGrade, chefDescendantLastname)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { chefDescendantUsername ?: "" }

    /** Display name of the chef de poste montant. */
    val chefMontantDisplay: String
        get() = listOfNotNull(chefMontantGrade, chefMontantLastname)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { chefMontantUsername ?: "" }
}

data class PassationAttachment(
    val id: Int,
    val passationId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)

/** Identity returned by POST /api/auth/verify (chef montant credential check). */
data class VerifiedIdentity(
    val id: Int,
    val username: String,
    val grade: String?,
    val firstname: String?,
    val lastname: String?
)
