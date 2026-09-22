package com.gsoft.opus.presentation.evenement

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Backend module code used for permission checks on the évènements survenus feature. */
const val EVENEMENT_MODULE = "sg_evenement_survenu"

fun evenementAttachmentDownloadUrl(evenementId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/evenements-survenus/$evenementId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImageEvenementAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
