package com.gsoft.opus.presentation.correspondance

private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
private val HEURE_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

/**
 * Pure client-side validation for the correspondance form, mirroring the
 * backend rules in CorrespondanceController::validate. Returns the first
 * error message (in French) or null when the input is valid.
 */
object CorrespondanceFormValidator {

    fun validate(
        dateCorrespondance: String,
        heure: String,
        sens: String,
        reference: String,
        emetteurDestinataire: String,
        objet: String
    ): String? {
        if (dateCorrespondance.isBlank() || !DATE_REGEX.matches(dateCorrespondance)) {
            return "La date est requise (format AAAA-MM-JJ)"
        }
        if (heure.isBlank() || !HEURE_REGEX.matches(heure)) {
            return "L'heure est invalide (format attendu : HH:MM)"
        }
        if (sens !in CORRESPONDANCE_SENS) {
            return "Le sens doit être Entrant ou Sortant"
        }
        if (reference.isBlank()) {
            return "Le numéro d'ordre / la référence est requis"
        }
        if (emetteurDestinataire.isBlank()) {
            return "L'émetteur / le destinataire est requis"
        }
        if (objet.isBlank()) {
            return "L'objet est requis"
        }
        return null
    }
}
