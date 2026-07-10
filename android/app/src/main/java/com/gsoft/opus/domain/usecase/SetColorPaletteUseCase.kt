package com.gsoft.opus.domain.usecase

import com.gsoft.opus.domain.model.ColorPalette
import com.gsoft.opus.domain.repository.SettingsRepository
import javax.inject.Inject

class SetColorPaletteUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(palette: ColorPalette) = settingsRepository.setColorPalette(palette)
}
