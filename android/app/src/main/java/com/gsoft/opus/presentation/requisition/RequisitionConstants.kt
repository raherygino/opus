package com.gsoft.opus.presentation.requisition

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** The four requisition types. */
val REQUISITION_TYPES = listOf(
    "TPH",
    "MEDECIN_LEGISTE",
    "CIM",
    "AUTRE"
)

/** Human-readable labels for each requisition type. */
val REQUISITION_TYPE_LABELS = mapOf(
    "TPH" to "TPH",
    "MEDECIN_LEGISTE" to "Médecin légiste",
    "CIM" to "CIM",
    "AUTRE" to "Autre"
)

/** Permission module code for the REQUISITION feature. */
const val PJ_REQUISITION_MODULE = "pj_requisition"

/** Build the download URL for a requisition attachment. */
fun requisitionAttachmentDownloadUrl(requisitionId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/requisitions/$requisitionId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isRequisitionImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
