package com.gsoft.opus.presentation.evenement

/** Regex for a date in YYYY-MM-DD format. */
private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/** Regex for a time in HH:MM or HH:MM:SS format. */
private val HEURE_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$")

/**
 * Validates an évènement survenu form. Returns a map of field → French
 * error message, empty when valid. Only the first error per field is
 * reported, matching the Rassemblement validator convention.
 */
fun validateEvenementForm(
    dateEvenement: String,
    heureEvenement: String,
    typeEvenement: String,
    lieuExact: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (dateEvenement.isBlank()) {
        errors["dateEvenement"] = "La date de l'évènement est requise"
    } else if (!DATE_REGEX.matches(dateEvenement) || !runCatching { java.time.LocalDate.parse(dateEvenement) }.isSuccess) {
        errors["dateEvenement"] = "La date est invalide (format attendu : AAAA-MM-JJ)"
    }

    if (heureEvenement.isBlank()) {
        errors["heureEvenement"] = "L'heure de l'évènement est requise"
    } else if (!HEURE_REGEX.matches(heureEvenement)) {
        errors["heureEvenement"] = "L'heure est invalide (format attendu : HH:MM)"
    }

    // The type comes from the user-managed catalog — only non-blank is
    // checked client-side; the server validates it exists in the catalog.
    if (typeEvenement.isBlank()) {
        errors["typeEvenement"] = "Le type d'événement est requis"
    }

    if (lieuExact.isBlank()) {
        errors["lieuExact"] = "Le lieu exact est requis"
    }

    return errors
}
