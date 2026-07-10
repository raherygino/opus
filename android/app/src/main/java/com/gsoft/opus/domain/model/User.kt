package com.gsoft.opus.domain.model

data class User(
    val id: Int,
    val username: String,
    val roleId: Int?,
    val roleCode: String?,
    val roleName: String?,
    val personnelId: Int?,
    val isActive: Boolean,
    val permissions: List<Permission>
)

data class Permission(
    val id: Int,
    val module: String,
    val canView: Boolean,
    val canCreate: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean,
    val canExport: Boolean
)

data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)
