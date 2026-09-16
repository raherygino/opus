package com.gsoft.opus.presentation.convocation

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** The two convocation types. */
val CONVOCATION_TYPES = listOf(
    "ST_PARQUET",
    "PLAINTE_DIRECTE"
)

/** Human-readable labels for each convocation type. */
val CONVOCATION_TYPE_LABELS = mapOf(
    "ST_PARQUET" to "ST Parquet",
    "PLAINTE_DIRECTE" to "Plainte directe"
)

/** Permission module code for the CONVOCATION feature. */
const val PJ_CONVOCATION_MODULE = "pj_convocation"

/** Build the download URL for a convocation attachment. */
fun convocationAttachmentDownloadUrl(convocationId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/convocations/$convocationId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
