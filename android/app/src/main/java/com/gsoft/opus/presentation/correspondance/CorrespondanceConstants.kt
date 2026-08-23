package com.gsoft.opus.presentation.correspondance

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Same sens list as the desktop correspondance page. */
val CORRESPONDANCE_SENS = listOf(
    "Entrant",
    "Sortant"
)

/** Suggested default statut values. The statut field is free-text, these are
 *  only used as a sensible default for new correspondances. */
val CORRESPONDANCE_STATUTS = listOf(
    "Enregistré",
    "En traitement",
    "Traité",
    "Archivé"
)

fun correspondanceAttachmentDownloadUrl(correspondanceId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/correspondances/$correspondanceId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)

/** Formats an API time ("HH:MM:SS" or "HH:MM") as "HH:MM" for display. */
fun formatHeureDisplay(heure: String?): String {
    if (heure.isNullOrBlank()) return "—"
    return heure.take(5)
}
