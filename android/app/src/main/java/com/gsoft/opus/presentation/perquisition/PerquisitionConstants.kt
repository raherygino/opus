package com.gsoft.opus.presentation.perquisition

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Permission module code for the PERQUISITION feature. */
const val PJ_PERQUISITION_MODULE = "pj_perquisition"

fun perquisitionAttachmentDownloadUrl(perquisitionId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/perquisitions/$perquisitionId/attachments/$attachId/download"

fun isPerquisitionImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)

/**
 * Validates a perquisition form. Returns field → French error map.
 */
fun validatePerquisitionForm(
    affaire: String
): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (affaire.isBlank()) {
        errors["affaire"] = "L'affaire est requise"
    }
    return errors
}
