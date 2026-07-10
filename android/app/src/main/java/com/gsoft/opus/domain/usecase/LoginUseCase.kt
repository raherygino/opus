package com.gsoft.opus.domain.usecase

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String, rememberMe: Boolean): Resource<AuthResult> {
        if (username.isBlank()) return Resource.error("Username is required")
        if (password.isBlank()) return Resource.error("Password is required")
        return authRepository.login(username, password, rememberMe)
    }
}
