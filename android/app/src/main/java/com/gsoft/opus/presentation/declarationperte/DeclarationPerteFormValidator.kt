package com.gsoft.opus.presentation.declarationperte

private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
private val HEURE_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

/**
 * Pure client-side validation for the déclaration de perte form, mirroring the
 * backend rules in DeclarationPerteController::validate. Returns the first
 * error message (in French) or null when the input is valid.
 */
object DeclarationPerteFormValidator {

    fun validate(
        dateDeclaration: String,
        heure: String,
        identiteDeclarant: String,
        natureObjet: String,
        descriptionObjet: String,
        datePerte: String,
        lieuPerte: String,
        numeroAttestation: String,
        nomAgent: String
    ): String? {
        if (dateDeclaration.isBlank() || !DATE_REGEX.matches(dateDeclaration)) {
            return "La date de déclaration est requise (format AAAA-MM-JJ)"
        }
        if (heure.isBlank() || !HEURE_REGEX.matches(heure)) {
            return "L'heure de déclaration est invalide (format attendu : HH:MM)"
        }
        if (identiteDeclarant.isBlank()) {
            return "L'identité du déclarant est requise"
        }
        if (natureObjet.isBlank()) {
            return "La nature de l'objet est requise"
        }
        if (descriptionObjet.isBlank()) {
            return "La description de l'objet est requise"
        }
        if (datePerte.isBlank() || !DATE_REGEX.matches(datePerte)) {
            return "La date de perte est requise (format AAAA-MM-JJ)"
        }
        if (lieuPerte.isBlank()) {
            return "Le lieu de perte est requis"
        }
        if (numeroAttestation.isBlank()) {
            return "Le numéro d'attestation est requis"
        }
        if (nomAgent.isBlank()) {
            return "Le nom de l'agent est requis"
        }
        return null
    }
}
