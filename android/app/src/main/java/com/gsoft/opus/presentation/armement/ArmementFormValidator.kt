package com.gsoft.opus.presentation.armement

private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
private val HEURE_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
private val INT_REGEX = Regex("^\\d+$")

/**
 * Pure client-side validation for the armement forms, mirroring the backend
 * rules in ArmementController::validate / validateReintegration. Returns the
 * first error message (in French) or null when the input is valid.
 */
object ArmementFormValidator {

    fun validate(
        datePerception: String,
        heurePerception: String,
        agentSelected: Boolean,
        typeArme: String,
        matriculeArme: String,
        munitions: String
    ): String? {
        if (datePerception.isBlank() || !DATE_REGEX.matches(datePerception)) {
            return "La date de la perception est requise (format AAAA-MM-JJ)"
        }
        if (heurePerception.isBlank() || !HEURE_REGEX.matches(heurePerception)) {
            return "L'heure de la perception est invalide (format attendu : HH:MM)"
        }
        if (!agentSelected) {
            return "L'agent preneur est requis"
        }
        if (typeArme.isBlank()) {
            return "Le type de l'arme est requis"
        }
        if (matriculeArme.isBlank()) {
            return "Le matricule de l'arme est requis"
        }
        if (munitions.isNotBlank() && !INT_REGEX.matches(munitions.trim())) {
            return "Les munitions doivent être un nombre entier positif"
        }
        return null
    }

    /**
     * Validation for the reintegration form — only the three reintegration
     * fields. [munitionsPercues] is the round count handed over at
     * perception (null when unknown).
     */
    fun validateReintegration(
        heureReintegration: String,
        etatReintegration: String,
        munitionsConsommees: String,
        munitionsPercues: Int?
    ): String? {
        if (heureReintegration.isBlank() || !HEURE_REGEX.matches(heureReintegration)) {
            return "L'heure de la réintégration est invalide (format attendu : HH:MM)"
        }
        if (etatReintegration.isBlank()) {
            return "L'état à la réintégration est requis"
        }
        val consommees = munitionsConsommees.trim()
        if (consommees.isEmpty() || !INT_REGEX.matches(consommees)) {
            return "Les munitions consommées doivent être un nombre entier positif"
        }
        if (munitionsPercues != null && consommees.toInt() > munitionsPercues) {
            return "Les munitions consommées ne peuvent pas dépasser les munitions perçues ($munitionsPercues)"
        }
        return null
    }
}
