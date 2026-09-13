package com.gsoft.opus.presentation.materielroulant

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

const val MATERIEL_ROULANT_MODULE = "sedentaire_poste_materiel_roulant"

fun materielRoulantAttachmentDownloadUrl(materielRoulantId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/materiels-roulants/$materielRoulantId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImageMaterielRoulantAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
