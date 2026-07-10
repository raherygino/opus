package com.gsoft.opus.domain.usecase

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.User
import com.gsoft.opus.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<User> = authRepository.getCurrentUser()
}
