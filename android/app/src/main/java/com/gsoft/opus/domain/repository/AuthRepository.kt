package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String, rememberMe: Boolean): Resource<AuthResult>
    suspend fun refreshToken(): Resource<String>
    suspend fun getCurrentUser(): Resource<User>
    suspend fun isLoggedIn(): Boolean
    suspend fun logout()
    fun getSavedUsername(): Flow<String>
    fun getRememberMe(): Flow<Boolean>
}
