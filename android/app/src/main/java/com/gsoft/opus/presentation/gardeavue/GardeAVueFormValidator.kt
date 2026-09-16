package com.gsoft.opus.presentation.gardeavue

/** Regex for a date in YYYY-MM-DD format. */
private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/** Regex for a datetime in YYYY-MM-DD HH:MM(:SS) format. */
private val DATETIME_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}(:\\d{2})?$")

/**
 * Validates a garde à vue form. Returns a map of field → French error
 * message, empty when valid.
 *
 * Matches the server-side validation in GardeAVueController::validate.
 */
fun validateGardeAVueForm(
    nom: String,
    dateNaissance: String,
    debutGav: String,
    finGav: String,
    prolongationGav: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (nom.isBlank()) {
        errors["nom"] = "Le nom est requis"
    }

    if (dateNaissance.isNotBlank() &&
        (!DATE_REGEX.matches(dateNaissance) || runCatching { java.time.LocalDate.parse(dateNaissance) }.isFailure)
    ) {
        errors["date_naissance"] = "La date de naissance est invalide (format attendu : AAAA-MM-JJ)"
    }

    fun parseDateTime(value: String): java.time.LocalDateTime? {
        if (value.isBlank()) return null
        // Accept "YYYY-MM-DD HH:MM" and "YYYY-MM-DD HH:MM:SS"
        val normalized = if (value.length == 16) "$value:00" else value
        return runCatching {
            java.time.LocalDateTime.parse(normalized, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }.getOrNull()
    }

    val debut = parseDateTime(debutGav)
    val fin = parseDateTime(finGav)
    val prolongation = parseDateTime(prolongationGav)

    if (debutGav.isNotBlank() && debut == null) {
        errors["debut_gav"] = "Le début de la garde à vue est invalide (format attendu : AAAA-MM-JJ HH:MM)"
    }
    if (finGav.isNotBlank() && fin == null) {
        errors["fin_gav"] = "La fin de la garde à vue est invalide (format attendu : AAAA-MM-JJ HH:MM)"
    }
    if (prolongationGav.isNotBlank() && prolongation == null) {
        errors["prolongation_gav"] = "La prolongation est invalide (format attendu : AAAA-MM-JJ HH:MM)"
    }

    if (debut != null && fin != null && fin.isBefore(debut)) {
        errors["fin_gav"] = "La fin de la garde à vue ne peut pas être antérieure au début"
    }
    if (fin != null && prolongation != null && prolongation.isBefore(fin)) {
        errors["prolongation_gav"] = "La prolongation ne peut pas être antérieure à la fin de la garde à vue"
    }

    return errors
}
