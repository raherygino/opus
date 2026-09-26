package com.gsoft.opus.presentation.registredeferrement

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Permission module code for the REGISTRE DE DÉFERREMENT feature. */
const val PJ_DEFERREMENT_MODULE = "pj_deferrement"

fun deferrementAttachmentDownloadUrl(deferrementId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/registres-deferrement/$deferrementId/attachments/$attachId/download"

fun isDeferrementImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)

/**
 * Validates a registre de déferrement form. Returns field → French error map.
 * dateHeureDeferrement is a combined "YYYY-MM-DD HH:MM:SS" string or empty.
 */
fun validateDeferrementForm(
    personneNom: String,
    dateHeureDeferrement: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (dateHeureDeferrement.isBlank()) {
        errors["date_heure_deferrement"] = "La date et l'heure du déferrement sont requises"
    } else if (!Regex("^\\d{4}-\\d{2}-\\d{2}( \\d{2}:\\d{2}(:\\d{2})?)?$").matches(dateHeureDeferrement)) {
        errors["date_heure_deferrement"] = "Format attendu : AAAA-MM-JJ HH:MM"
    }
    if (personneNom.isBlank()) {
        errors["personne_nom"] = "Le nom et prénom de la personne déférée sont requis"
    }
    return errors
}
