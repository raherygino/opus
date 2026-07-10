package com.gsoft.opus.domain.usecase

import com.gsoft.opus.domain.model.ThemeMode
import com.gsoft.opus.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = settingsRepository.setThemeMode(mode)
}
