package com.gsoft.opus.presentation.rassemblement

/** Regex for a date in YYYY-MM-DD format. */
private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/** Regex for a time in HH:MM or HH:MM:SS format. */
private val HEURE_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$")

/** Non-negative integer (situation de prise d'arme counters). */
private val NUMBER_REGEX = Regex("^\\d+$")

/**
 * Validates a rassemblement journalier form. Returns a map of field → French
 * error message, empty when valid. Only the first error per field is
 * reported, matching the Correspondance validator convention.
 */
fun validateRassemblementForm(
    dateRassemblement: String,
    heureRassemblement: String,
    brigadeService: String,
    effectifTheorique: String,
    present: String,
    absent: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (dateRassemblement.isBlank()) {
        errors["dateRassemblement"] = "La date du rassemblement est requise"
    } else if (!DATE_REGEX.matches(dateRassemblement) || !runCatching { java.time.LocalDate.parse(dateRassemblement) }.isSuccess) {
        errors["dateRassemblement"] = "La date est invalide (format attendu : AAAA-MM-JJ)"
    }

    if (heureRassemblement.isBlank()) {
        errors["heureRassemblement"] = "L'heure du rassemblement est requise"
    } else if (!HEURE_REGEX.matches(heureRassemblement)) {
        errors["heureRassemblement"] = "L'heure est invalide (format attendu : HH:MM)"
    }

    if (brigadeService.isBlank()) {
        errors["brigadeService"] = "La brigade de service est requise"
    }

    // Situation de prise d'arme — part of the record itself.
    for ((field, label, value) in listOf(
        Triple("effectifTheorique", "L'effectif théorique", effectifTheorique),
        Triple("present", "Le nombre de présents", present),
        Triple("absent", "Le nombre d'absents", absent)
    )) {
        if (value.isBlank()) {
            errors[field] = "$label est requis"
        } else if (!NUMBER_REGEX.matches(value)) {
            errors[field] = "$label doit être un entier positif ou nul"
        }
    }

    return errors
}
