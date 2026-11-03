package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.User

/** Input data for creating/updating a user account (Super Admin). */
data class UserFormData(
    /** Required on create: the personnel record the account is linked to. */
    val personnelId: Int? = null,
    val username: String,
    /** Required on create; null on update keeps the current password. */
    val password: String? = null,
    val roleId: Int,
    /** Edit only. */
    val isActive: Boolean? = null
)

interface UserRepository {
    suspend fun getUsers(): Resource<List<User>>
    suspend fun getUser(id: Int): Resource<User>
    suspend fun createUser(data: UserFormData): Resource<User>
    suspend fun updateUser(id: Int, data: UserFormData): Resource<User>
    suspend fun deleteUser(id: Int): Resource<Unit>

    /** Personnel records not yet linked to a user account (create form). */
    suspend fun getAvailablePersonnel(): Resource<List<Personnel>>
}
