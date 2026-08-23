package com.gsoft.opus.presentation.declarationperte

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Backend module code used for permission checks on the déclaration de perte feature. */
const val DECLARATION_PERTE_MODULE = "sedentaire_secretariat_declaration_perte"

/** Formats an API time ("HH:MM:SS" or "HH:MM") as "HH:MM" for display. */
fun formatHeureDeclarationDisplay(heure: String?): String {
    if (heure.isNullOrBlank()) return "—"
    return heure.take(5)
}

fun declarationPerteAttachmentDownloadUrl(declarationId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/declarations-perte/$declarationId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImageDeclarationPerteAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
