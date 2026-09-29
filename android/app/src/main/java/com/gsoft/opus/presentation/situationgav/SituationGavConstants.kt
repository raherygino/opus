package com.gsoft.opus.presentation.situationgav

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Permission module code for the Situation GAV feature (Sédentaire > Poste). */
const val SED_SITUATION_GAV_MODULE = "sedentaire_poste_situation_gav"

/** Build the download URL for a Situation GAV attachment. */
fun situationGavAttachmentDownloadUrl(situationGavId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/situations-gav/$situationGavId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isSituationGavImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
