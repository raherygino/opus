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

    /** PUT /api/auth/profile — updates the user's own personnel record. */
    suspend fun updateProfile(
        lastname: String,
        firstname: String,
        phone: String?,
        email: String?,
        address: String?
    ): Resource<User>

    /** PUT /api/auth/password — verifies current password server-side. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Resource<Unit>

    /** POST /api/auth/photo — returns the updated user (new photo filename). */
    suspend fun uploadProfilePhoto(photo: UploadFile): Resource<User>

    /** DELETE /api/auth/photo — returns the updated user (photo cleared). */
    suspend fun deleteProfilePhoto(): Resource<User>
}
