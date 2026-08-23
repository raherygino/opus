package com.gsoft.opus.presentation.passation

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Backend module code used for permission checks on the passation feature. */
const val PASSATION_MODULE = "sedentaire_poste_passation"

/** Formats an API time ("HH:MM:SS" or "HH:MM") as "HH:MM" for display. */
fun formatHeurePassationDisplay(heure: String?): String {
    if (heure.isNullOrBlank()) return "—"
    return heure.take(5)
}

fun passationAttachmentDownloadUrl(passationId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/passations/$passationId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImagePassationAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
