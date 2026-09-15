package com.gsoft.opus.presentation.mandat

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Permission module code for the MANDAT feature. */
const val PJ_MANDAT_MODULE = "pj_mandat"

/** Mandat types (objet du mandat) — keep in sync with Mandat::TYPES on the API. */
val MANDAT_TYPES = listOf("AMENER", "COMPARUTION", "ARRET", "DEPOT")

val MANDAT_TYPE_LABELS: Map<String, String> = mapOf(
    "AMENER" to "Mandat d'amener",
    "COMPARUTION" to "Mandat de comparution",
    "ARRET" to "Mandat d'arrêt",
    "DEPOT" to "Mandat de dépôt"
)

fun mandatTypeLabel(type: String): String = MANDAT_TYPE_LABELS[type] ?: type

fun mandatAttachmentDownloadUrl(mandatId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/mandats/$mandatId/attachments/$attachId/download"

fun isMandatImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)

/**
 * Validates a mandat form. Returns field → French error map.
 * dateHeureExecution is a combined "YYYY-MM-DD HH:MM:SS" string or empty.
 */
fun validateMandatForm(
    numero: String,
    type: String,
    personneNom: String,
    dateHeureExecution: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (type.isBlank()) {
        errors["type"] = "Le type de mandat est requis"
    }
    if (personneNom.isBlank()) {
        errors["personne_nom"] = "Le nom et prénom de la personne concernée sont requis"
    }
    if (dateHeureExecution.isNotBlank() &&
        !Regex("^\\d{4}-\\d{2}-\\d{2}( \\d{2}:\\d{2}(:\\d{2})?)?$").matches(dateHeureExecution)
    ) {
        errors["date_heure_execution"] = "La date et l'heure d'exécution sont invalides"
    }
    return errors
}
