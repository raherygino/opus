package com.gsoft.opus.presentation.gardeavue

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Permission module code for the GAV feature. */
const val PJ_GAV_MODULE = "pj_gav"

/** Build the download URL for a GAV attachment. */
fun gardeAVueAttachmentDownloadUrl(gardeAVueId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/garde-a-vue/$gardeAVueId/attachments/$attachId/download"

/** Whether an attachment is an image — delegates to the shared helper. */
fun isGavImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
