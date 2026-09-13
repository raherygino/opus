package com.gsoft.opus.presentation.plainte

/** Regex for a date in YYYY-MM-DD format. */
private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/** Regex for a time in HH:MM or HH:MM:SS format. */
private val HEURE_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$")

/**
 * Validates a plainte ENTRÉE form. Returns a map of field → French error
 * message, empty when valid. Type-conditional rules:
 *   ST_PARQUET      — requires numeroSt, partieCivile, adressePc
 *   PLAINTE_DIRECTE — requires partieCivile, adressePc (no numeroSt)
 *   RAPPORT_POLICE  — requires miseEnCause only (no PC, no adresse, no numeroSt)
 *
 * Matches the server-side validation in PlainteEntreeController::validate.
 */
fun validatePlainteEntreeForm(
    type: String,
    datePlainte: String,
    numeroSt: String,
    opjPersonnelId: Int,
    enqueteurPersonnelId: Int,
    partieCivile: String,
    miseEnCause: String,
    adressePc: String,
    infraction: String,
    prejudice: String,
    lieuInfraction: String,
    heureInfraction: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (type.isBlank()) {
        errors["type"] = "Le type de plainte est requis"
    } else if (type !in PLAINTE_ENTREE_TYPES) {
        errors["type"] = "Le type de plainte est invalide"
    }

    if (datePlainte.isBlank()) {
        errors["datePlainte"] = "La date est requise"
    } else if (!DATE_REGEX.matches(datePlainte) || runCatching { java.time.LocalDate.parse(datePlainte) }.isFailure) {
        errors["datePlainte"] = "La date est invalide (format attendu : AAAA-MM-JJ)"
    }

    // Type-conditional: ST requires numeroSt.
    if (type == "ST_PARQUET" && numeroSt.isBlank()) {
        errors["numeroSt"] = "Le numéro du ST est requis pour ce type"
    }

    if (opjPersonnelId <= 0) {
        errors["opjPersonnelId"] = "L'OPJ est requis"
    }
    if (enqueteurPersonnelId <= 0) {
        errors["enqueteurPersonnelId"] = "L'enquêteur est requis"
    }

    // Type-conditional: ST & PD require partieCivile.
    if (type in listOf("ST_PARQUET", "PLAINTE_DIRECTE") && partieCivile.isBlank()) {
        errors["partieCivile"] = "La partie civile est requise pour ce type"
    }

    // All types require miseEnCause.
    if (miseEnCause.isBlank()) {
        errors["miseEnCause"] = "La mise en cause est requise"
    }

    // Type-conditional: ST & PD require adressePc.
    if (type in listOf("ST_PARQUET", "PLAINTE_DIRECTE") && adressePc.isBlank()) {
        errors["adressePc"] = "L'adresse du PC est requise pour ce type"
    }

    if (infraction.isBlank()) {
        errors["infraction"] = "L'infraction est requise"
    }
    if (prejudice.isBlank()) {
        errors["prejudice"] = "Le préjudice est requis"
    }
    if (lieuInfraction.isBlank()) {
        errors["lieuInfraction"] = "Le lieu de l'infraction est requis"
    }

    // heureInfraction is optional but validated if present.
    if (heureInfraction.isNotBlank() && !HEURE_REGEX.matches(heureInfraction)) {
        errors["heureInfraction"] = "L'heure est invalide (format attendu : HH:MM)"
    }

    return errors
}

/**
 * Validates a plainte SORTIE form. Returns a map of field → French error
 * message, empty when valid. dateDeferrement is required only when
 * nature = DEFERREMENT.
 *
 * Matches the server-side validation in PlainteSortieController::validate.
 */
fun validatePlainteSortieForm(
    nature: String,
    dateSortie: String,
    numeroTtr: String,
    nomSubstitut: String,
    dateDeferrement: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (nature.isBlank()) {
        errors["nature"] = "La nature est requise"
    } else if (nature !in PLAINTE_SORTIE_NATURES) {
        errors["nature"] = "La nature est invalide"
    }

    if (dateSortie.isBlank()) {
        errors["dateSortie"] = "La date est requise"
    } else if (!DATE_REGEX.matches(dateSortie) || runCatching { java.time.LocalDate.parse(dateSortie) }.isFailure) {
        errors["dateSortie"] = "La date est invalide (format attendu : AAAA-MM-JJ)"
    }

    if (numeroTtr.isBlank()) {
        errors["numeroTtr"] = "Le N° TTR est requis"
    }
    if (nomSubstitut.isBlank()) {
        errors["nomSubstitut"] = "Le nom du substitut est requis"
    }

    // DEFERREMENT requires dateDeferrement.
    if (nature == "DEFERREMENT") {
        if (dateDeferrement.isBlank()) {
            errors["dateDeferrement"] = "La date du déferrement est requise pour ce type"
        } else if (!DATE_REGEX.matches(dateDeferrement) || runCatching { java.time.LocalDate.parse(dateDeferrement) }.isFailure) {
            errors["dateDeferrement"] = "La date du déferrement est invalide (format attendu : AAAA-MM-JJ)"
        }
    }

    return errors
}
