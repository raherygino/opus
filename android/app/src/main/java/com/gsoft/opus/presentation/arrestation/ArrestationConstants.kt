package com.gsoft.opus.presentation.arrestation

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Permission module code for the ARRESTATION feature. */
const val PJ_ARRESTATION_MODULE = "pj_arrestation"

fun arrestationAttachmentDownloadUrl(arrestationId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/arrestations/$arrestationId/attachments/$attachId/download"

fun isArrestationImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)

/**
 * Validates an arrestation form. Returns field → French error map.
 * dateHeureArrestation is a combined "YYYY-MM-DD HH:MM:SS" string or empty.
 */
fun validateArrestationForm(
    personneNom: String,
    dateHeureArrestation: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (personneNom.isBlank()) {
        errors["personne_nom"] = "Le nom et prénom de la personne arrêtée sont requis"
    }
    if (dateHeureArrestation.isBlank()) {
        errors["date_heure_arrestation"] = "La date et l'heure de l'arrestation sont requises"
    } else if (!Regex("^\\d{4}-\\d{2}-\\d{2}( \\d{2}:\\d{2}(:\\d{2})?)?$").matches(dateHeureArrestation)) {
        errors["date_heure_arrestation"] = "La date et l'heure de l'arrestation sont invalides"
    }
    return errors
}
