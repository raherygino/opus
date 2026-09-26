package com.gsoft.opus.presentation.registreenquete

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Permission module code for the REGISTRE D'ENQUÊTE feature. */
const val PJ_ENQUETE_MODULE = "pj_enquete"

val REGISTRE_ENQUETE_STATUT_LABELS = mapOf(
    "EN_COURS" to "En cours",
    "SUSPENDUE" to "Suspendue",
    "TRANSMISE" to "Transmise",
    "CLOTUREE" to "Clôturée"
)

fun enqueteAttachmentDownloadUrl(enqueteId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/registres-enquete/$enqueteId/attachments/$attachId/download"

fun isEnqueteImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)

/**
 * Validates a registre d'enquête form. Returns field → French error map.
 */
fun validateEnqueteForm(
    dateOuverture: String,
    natureInfraction: String,
    enqueteurPersonnelId: Int
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (dateOuverture.isBlank()) {
        errors["date_ouverture"] = "La date d'ouverture est requise"
    } else if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dateOuverture)) {
        errors["date_ouverture"] = "La date d'ouverture doit être au format AAAA-MM-JJ"
    }
    if (natureInfraction.isBlank()) {
        errors["nature_infraction"] = "La nature de l'infraction est requise"
    }
    if (enqueteurPersonnelId <= 0) {
        errors["enqueteur_personnel_id"] = "L'enquêteur est requis"
    }
    return errors
}
