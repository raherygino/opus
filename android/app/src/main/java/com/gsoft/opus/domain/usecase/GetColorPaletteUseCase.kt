package com.gsoft.opus.domain.usecase

import com.gsoft.opus.domain.model.ColorPalette
import com.gsoft.opus.domain.repository.SettingsRepository
import javax.inject.Inject

class GetColorPaletteUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): kotlinx.coroutines.flow.Flow<ColorPalette> = settingsRepository.colorPalette
}
