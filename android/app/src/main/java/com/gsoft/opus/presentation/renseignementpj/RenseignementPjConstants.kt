package com.gsoft.opus.presentation.renseignementpj

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Permission module code for the RENSEIGNEMENT PJ feature. */
const val PJ_RENSEIGNEMENT_MODULE = "pj_renseignement"

fun renseignementPjAttachmentDownloadUrl(renseignementId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/renseignements-pj/$renseignementId/attachments/$attachId/download"

fun isRenseignementPjImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)

/**
 * Validates a renseignement PJ form. Returns field → French error map.
 */
fun validateRenseignementPjForm(
    natureInfraction: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (natureInfraction.isBlank()) {
        errors["nature_infraction"] = "La nature de l'infraction est requise"
    }
    return errors
}
