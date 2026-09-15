package com.gsoft.opus.presentation.convocation

/** Regex for a date in YYYY-MM-DD format. */
private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/**
 * Validates a convocation form. Returns a map of field → French error
 * message, empty when valid. Both types share the same fields.
 *
 * Matches the server-side validation in ConvocationController::validate.
 */
fun validateConvocationForm(
    type: String,
    dateConvocation: String,
    nom: String,
    infraction: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (type.isBlank()) {
        errors["type"] = "Le type de convocation est requis"
    } else if (type !in CONVOCATION_TYPES) {
        errors["type"] = "Le type de convocation est invalide"
    }

    if (dateConvocation.isBlank()) {
        errors["dateConvocation"] = "La date est requise"
    } else if (!DATE_REGEX.matches(dateConvocation) || runCatching { java.time.LocalDate.parse(dateConvocation) }.isFailure) {
        errors["dateConvocation"] = "La date est invalide (format attendu : AAAA-MM-JJ)"
    }

    if (nom.isBlank()) {
        errors["nom"] = "Le nom est requis"
    }

    if (infraction.isBlank()) {
        errors["infraction"] = "L'infraction est requise"
    }

    return errors
}
