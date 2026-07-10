package com.gsoft.opus.domain.model

import androidx.compose.ui.graphics.Color

enum class ColorPalette(
    val key: String,
    val displayName: String,
    val previewColor: Color
) {
    INDIGO(
        key = "indigo",
        displayName = "Indigo",
        previewColor = Color(0xFF6C63FF)
    ),
    BLUE(
        key = "blue",
        displayName = "Bleu",
        previewColor = Color(0xFF1976D2)
    ),
    GREEN(
        key = "green",
        displayName = "Vert",
        previewColor = Color(0xFF2E7D32)
    ),
    PURPLE(
        key = "purple",
        displayName = "Violet",
        previewColor = Color(0xFF9C27B0)
    ),
    RED(
        key = "red",
        displayName = "Rouge",
        previewColor = Color(0xFFE53935)
    ),
    ORANGE(
        key = "orange",
        displayName = "Orange",
        previewColor = Color(0xFFEF6C00)
    ),
    TEAL(
        key = "teal",
        displayName = "Sarcelle",
        previewColor = Color(0xFF00897B)
    ),
    BLACK(
        key = "black",
        displayName = "Noir",
        previewColor = Color(0xFF000000)
    );

    companion object {
        fun fromKey(key: String?): ColorPalette =
            entries.firstOrNull { it.key == key } ?: INDIGO
    }
}
