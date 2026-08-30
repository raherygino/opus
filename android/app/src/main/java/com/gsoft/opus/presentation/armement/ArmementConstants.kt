package com.gsoft.opus.presentation.armement

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Backend module code used for permission checks on the armement feature. */
const val ARMEMENT_MODULE = "sedentaire_poste_armement"

fun armementAttachmentDownloadUrl(armementId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/armements/$armementId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isImageArmementAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
