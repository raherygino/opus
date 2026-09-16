package com.gsoft.opus.presentation.plainte

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** The three ENTRÉE complaint types. */
val PLAINTE_ENTREE_TYPES = listOf(
    "ST_PARQUET",
    "PLAINTE_DIRECTE",
    "RAPPORT_POLICE"
)

/** Human-readable labels for each ENTRÉE type. */
val PLAINTE_ENTREE_TYPE_LABELS = mapOf(
    "ST_PARQUET" to "ST Parquet",
    "PLAINTE_DIRECTE" to "Plainte directe",
    "RAPPORT_POLICE" to "Rapport de police"
)

/** The two SORTIE natures. */
val PLAINTE_SORTIE_NATURES = listOf(
    "DAT",
    "DEFERREMENT"
)

/** Human-readable labels for each SORTIE nature. */
val PLAINTE_SORTIE_NATURE_LABELS = mapOf(
    "DAT" to "DAT",
    "DEFERREMENT" to "Déferrement"
)

/** Permission module code for the PLAINTE feature. */
const val PJ_PLAINTE_MODULE = "pj_plainte"

/** Build the download URL for an ENTRÉE attachment. */
fun plainteEntreeAttachmentDownloadUrl(plainteEntreeId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/plaintes-entree/$plainteEntreeId/attachments/$attachId/download"

/** Build the download URL for a SORTIE attachment. */
fun plainteSortieAttachmentDownloadUrl(plainteSortieId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/plaintes-sortie/$plainteSortieId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)

/** Formats an API time ("HH:MM:SS" or "HH:MM") as "HH:MM" for display. */
fun formatHeureDisplay(heure: String?): String {
    if (heure.isNullOrBlank()) return "—"
    return heure.take(5)
}
