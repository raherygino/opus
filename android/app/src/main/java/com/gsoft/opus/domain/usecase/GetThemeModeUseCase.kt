package com.gsoft.opus.domain.usecase

import com.gsoft.opus.domain.model.ThemeMode
import com.gsoft.opus.domain.repository.SettingsRepository
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): kotlinx.coroutines.flow.Flow<ThemeMode> = settingsRepository.themeMode
}
