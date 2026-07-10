package com.gsoft.opus.data.repository

import com.gsoft.opus.data.local.UserPreferences
import com.gsoft.opus.domain.model.ColorPalette
import com.gsoft.opus.domain.model.ThemeMode
import com.gsoft.opus.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val userPreferences: UserPreferences
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = userPreferences.themeMode
    override val colorPalette: Flow<ColorPalette> = userPreferences.colorPalette

    override suspend fun setThemeMode(mode: ThemeMode) {
        userPreferences.setThemeMode(mode)
    }

    override suspend fun setColorPalette(palette: ColorPalette) {
        userPreferences.setColorPalette(palette)
    }
}
