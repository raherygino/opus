package com.gsoft.opus.presentation.situationgav

/** Regex for a datetime in YYYY-MM-DD HH:MM(:SS) format. */
private val DATETIME_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}(:\\d{2})?$")

/**
 * Validates a Situation GAV form. Returns a map of field → French error
 * message, empty when valid.
 *
 * Matches the server-side validation in SituationGavController::validate.
 */
fun validateSituationGavForm(
    gardeAVueId: Int,
    dateControle: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (gardeAVueId <= 0) {
        errors["garde_a_vue_id"] = "La personne concernée est requise"
    }

    if (dateControle.isBlank()) {
        errors["date_controle"] = "La date et l'heure du contrôle sont requises"
    } else if (!DATETIME_REGEX.matches(dateControle) ||
        runCatching {
            val normalized = if (dateControle.length == 16) "$dateControle:00" else dateControle
            java.time.LocalDateTime.parse(normalized, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }.isFailure
    ) {
        errors["date_controle"] = "La date/heure du contrôle est invalide (format attendu : AAAA-MM-JJ HH:MM)"
    }

    return errors
}
