package com.gsoft.opus.domain.model

data class User(
    val id: Int,
    val username: String,
    val roleId: Int?,
    val roleCode: String?,
    val roleName: String?,
    val personnelId: Int?,
    val isActive: Boolean,
    val permissions: List<Permission>,
    val firstName: String?,
    val lastName: String?,
    val photo: String?,
    val grade: String?,
    val affectation: String?,
    val im: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val lastLogin: String? = null,
    val createdAt: String? = null,
    /** Latest open mouvement type, or "En service" — user management only. */
    val personnelStatus: String? = null
) {
    /** SUPER_ADMIN and STATION_ADMIN accounts are protected from admin actions. */
    val isAdminRole: Boolean
        get() = roleCode == "SUPER_ADMIN" || roleCode == "STATION_ADMIN"
}

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
