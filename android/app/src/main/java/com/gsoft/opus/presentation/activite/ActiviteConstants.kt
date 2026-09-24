package com.gsoft.opus.presentation.activite

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Backend module code used for permission checks on the activité feature. */
const val ACTIVITE_MODULE = "sg_activite"

fun activiteAttachmentDownloadUrl(activiteId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/activites/$activiteId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImageActiviteAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
