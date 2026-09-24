package com.gsoft.opus.presentation.dispositif

/** Regex for a date in YYYY-MM-DD format. */
private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/**
 * Validates a dispositif exceptionnel form. Returns a map of field → French
 * error message, empty when valid. Only the first error per field is
 * reported, matching the other SG validators.
 */
fun validateDispositifForm(
    natureEvenement: String,
    dateDebut: String,
    dateFin: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (natureEvenement.isBlank()) {
        errors["natureEvenement"] = "La nature de l'évènement est requise"
    }

    fun checkDate(field: String, label: String, value: String) {
        if (value.isBlank()) {
            errors[field] = "La date $label est requise"
        } else if (!DATE_REGEX.matches(value) || !runCatching { java.time.LocalDate.parse(value) }.isSuccess) {
            errors[field] = "La date $label est invalide (format attendu : AAAA-MM-JJ)"
        }
    }
    checkDate("dateDebut", "de début", dateDebut)
    checkDate("dateFin", "de fin", dateFin)

    // Range coherence — only checked when both dates are present and valid.
    if (!errors.containsKey("dateDebut") && !errors.containsKey("dateFin")) {
        val debut = runCatching { java.time.LocalDate.parse(dateDebut) }.getOrNull()
        val fin = runCatching { java.time.LocalDate.parse(dateFin) }.getOrNull()
        if (debut != null && fin != null && fin.isBefore(debut)) {
            errors["dateFin"] = "La date de fin doit être postérieure ou égale à la date de début"
        }
    }

    return errors
}
