package com.gsoft.opus.presentation.personnerecherchee

/**
 * Validates a personne recherchée form. Returns a map of field → French
 * error message, empty when valid.
 */
fun validatePersonneRechercheeForm(
    nom: String,
    motif: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (nom.isBlank()) {
        errors["nom"] = "Le nom est requis"
    }

    if (motif.isBlank()) {
        errors["motif"] = "Le motif est requis"
    }

    return errors
}
