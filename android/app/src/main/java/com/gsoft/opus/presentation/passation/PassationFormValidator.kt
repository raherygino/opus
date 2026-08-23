package com.gsoft.opus.presentation.passation

private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
private val HEURE_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

/**
 * Pure client-side validation for the passation form, mirroring the backend
 * rules in PassationController::validate. Returns the first error message
 * (in French) or null when the input is valid.
 *
 * Note: the chef montant identity is validated separately via /auth/verify
 * — this validator only checks the passation fields. The chef montant MUST
 * be authenticated (montantIdentity != null) before save() is called.
 */
object PassationFormValidator {

    fun validate(
        datePassation: String,
        heurePassation: String,
        montantVerified: Boolean
    ): String? {
        if (datePassation.isBlank() || !DATE_REGEX.matches(datePassation)) {
            return "La date de la passation est requise (format AAAA-MM-JJ)"
        }
        if (heurePassation.isBlank() || !HEURE_REGEX.matches(heurePassation)) {
            return "L'heure de la passation est invalide (format attendu : HH:MM)"
        }
        if (!montantVerified) {
            return "Le chef de poste montant doit être authentifié"
        }
        return null
    }
}
