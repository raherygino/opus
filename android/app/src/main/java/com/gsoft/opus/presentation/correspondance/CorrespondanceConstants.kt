package com.gsoft.opus.presentation.correspondance

import com.gsoft.opus.core.Constants

/** Same sens list as the desktop correspondance page. */
val CORRESPONDANCE_SENS = listOf(
    "Entrant",
    "Sortant"
)

/** Same statut list as the desktop correspondance page. */
val CORRESPONDANCE_STATUTS = listOf(
    "Enregistré",
    "En traitement",
    "Traité",
    "Archivé"
)

fun correspondanceAttachmentDownloadUrl(correspondanceId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/correspondances/$correspondanceId/attachments/$attachId/download"

/** Formats an API time ("HH:MM:SS" or "HH:MM") as "HH:MM" for display. */
fun formatHeureDisplay(heure: String?): String {
    if (heure.isNullOrBlank()) return "—"
    return heure.take(5)
}
