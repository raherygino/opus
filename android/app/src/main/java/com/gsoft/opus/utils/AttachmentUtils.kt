package com.gsoft.opus.utils

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg")

/**
 * Whether an attachment is an image that can be previewed inside the app.
 * Uses the MIME type when available and falls back to the file extension.
 */
fun isImageFile(mimeType: String?, filename: String?): Boolean {
    if (mimeType?.startsWith("image/") == true) return true
    val ext = filename?.substringAfterLast('.', "")?.lowercase() ?: return false
    return ext in IMAGE_EXTENSIONS
}
