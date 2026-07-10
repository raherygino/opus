package com.gsoft.opus.domain.repository

import com.gsoft.opus.domain.model.ColorPalette
import com.gsoft.opus.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val colorPalette: Flow<ColorPalette>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setColorPalette(palette: ColorPalette)
}
