package com.gsoft.opus.presentation.activite

/** Regex for a date in YYYY-MM-DD format. */
private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/** Regex for a time in HH:MM or HH:MM:SS format. */
private val HEURE_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$")

/**
 * Validates an activité form. Returns a map of field → French error
 * message, empty when valid. Only the first error per field is reported,
 * matching the EvenementSurvenu validator convention.
 */
fun validateActiviteForm(
    dateActivite: String,
    heureActivite: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (dateActivite.isBlank()) {
        errors["dateActivite"] = "La date de l'activité est requise"
    } else if (!DATE_REGEX.matches(dateActivite) || !runCatching { java.time.LocalDate.parse(dateActivite) }.isSuccess) {
        errors["dateActivite"] = "La date est invalide (format attendu : AAAA-MM-JJ)"
    }

    if (heureActivite.isBlank()) {
        errors["heureActivite"] = "L'heure de l'activité est requise"
    } else if (!HEURE_REGEX.matches(heureActivite)) {
        errors["heureActivite"] = "L'heure est invalide (format attendu : HH:MM)"
    }

    return errors
}
