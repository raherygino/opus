package com.gsoft.opus.presentation.maincourante

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** The three required categories for a main courante entry. */
val MAIN_COURANTE_CATEGORIES = listOf(
    "Entrée/Sortie de tiers",
    "Incident au poste",
    "Renseignement reçu"
)

/** The two origines — which context owns the entry. */
val MAIN_COURANTE_ORIGINES = listOf(
    "Secretariat",
    "Poste"
)

/** Map an origine to its permission module code. */
fun moduleForOrigine(origine: String): String =
    if (origine == "Poste") "sedentaire_poste_main_courante"
    else "sedentaire_secretariat_main_courante"

fun mainCouranteAttachmentDownloadUrl(mainCouranteId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/main-courante/$mainCouranteId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)

/** Formats an API time ("HH:MM:SS" or "HH:MM") as "HH:MM" for display. */
fun formatHeureDisplay(heure: String?): String {
    if (heure.isNullOrBlank()) return "—"
    return heure.take(5)
}
