package com.gsoft.opus.domain.usecase

import com.gsoft.opus.domain.repository.AuthRepository
import javax.inject.Inject

class CheckAuthStatusUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Boolean = authRepository.isLoggedIn()
}
