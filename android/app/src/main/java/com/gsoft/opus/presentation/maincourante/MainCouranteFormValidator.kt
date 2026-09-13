package com.gsoft.opus.presentation.maincourante

/** Regex for a date in YYYY-MM-DD format. */
private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/** Regex for a time in HH:MM or HH:MM:SS format. */
private val HEURE_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$")

/**
 * Validates a main courante form. Returns a map of field → French error
 * message, empty when valid. Only the first error per field is reported,
 * matching the Correspondance validator convention.
 */
fun validateMainCouranteForm(
    dateEvenement: String,
    heureEvenement: String,
    categorie: String,
    description: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (dateEvenement.isBlank()) {
        errors["dateEvenement"] = "La période (date) est requise"
    } else if (!DATE_REGEX.matches(dateEvenement) || !runCatching { java.time.LocalDate.parse(dateEvenement) }.isSuccess) {
        errors["dateEvenement"] = "La date est invalide (format attendu : AAAA-MM-JJ)"
    }

    if (heureEvenement.isBlank()) {
        errors["heureEvenement"] = "L'heure précise est requise"
    } else if (!HEURE_REGEX.matches(heureEvenement)) {
        errors["heureEvenement"] = "L'heure est invalide (format attendu : HH:MM)"
    }

    if (categorie.isBlank()) {
        errors["categorie"] = "La catégorie de l'événement est requise"
    }
    // Note: we no longer validate against a fixed list — categories are
    // user-managed through the dedicated dialog and checked server-side.

    if (description.isBlank()) {
        errors["description"] = "La description des faits est requise"
    }

    return errors
}
