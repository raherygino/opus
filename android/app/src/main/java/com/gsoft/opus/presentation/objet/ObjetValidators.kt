package com.gsoft.opus.presentation.objet

/**
 * Validates an objet saisi form. Returns field → French error map.
 */
fun validateObjetSaisiForm(
    typeObjet: String,
    motif: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (typeObjet.isBlank() || typeObjet !in OBJET_SAISI_TYPES) {
        errors["type_objet"] = "Le type d'objet est requis"
    }
    if (motif.isBlank()) {
        errors["motif"] = "Le motif est requis"
    }
    return errors
}

/**
 * Validates an objet trouvé form. Returns field → French error map.
 */
fun validateObjetTrouveForm(
    affaire: String,
    motifDecouverte: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (affaire.isBlank()) {
        errors["affaire"] = "L'affaire est requise"
    }
    if (motifDecouverte.isBlank() || motifDecouverte !in OBJET_TROUVE_MOTIFS) {
        errors["motif_decouverte"] = "Le motif de découverte est requis"
    }
    return errors
}
