package com.gsoft.opus.presentation.requisition

private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/**
 * Validates a requisition form. Returns a map of field → French error
 * message, empty when valid.
 */
fun validateRequisitionForm(
    type: String,
    dateRequisition: String,
    affaire: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (type.isBlank() || type !in REQUISITION_TYPES) {
        errors["type"] = "Le type de réquisition est requis"
    }

    if (dateRequisition.isBlank()) {
        errors["date_requisition"] = "La date est requise"
    } else if (!DATE_REGEX.matches(dateRequisition) ||
        runCatching { java.time.LocalDate.parse(dateRequisition) }.isFailure
    ) {
        errors["date_requisition"] = "La date est invalide (format attendu : AAAA-MM-JJ)"
    }

    if (affaire.isBlank()) {
        errors["affaire"] = "L'affaire est requise"
    }

    return errors
}
