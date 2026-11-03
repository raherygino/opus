package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("username") val username: String = "",
    @SerializedName("role_id") val roleId: Int? = null,
    @SerializedName("role_code") val roleCode: String? = null,
    @SerializedName("role_name") val roleName: String? = null,
    @SerializedName("personnel_id") val personnelId: Int? = null,
    @SerializedName("is_active") val isActive: Int? = null,
    @SerializedName("last_login") val lastLogin: String? = null,
    @SerializedName("permissions") val permissions: List<PermissionDto>? = null,
    @SerializedName("firstname") val firstname: String? = null,
    @SerializedName("lastname") val lastname: String? = null,
    @SerializedName("photo") val photo: String? = null,
    @SerializedName("grade") val grade: String? = null,
    @SerializedName("affectation") val affectation: String? = null,
    @SerializedName("im") val im: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("personnel_status") val personnelStatus: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class PermissionDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("role_id") val roleId: Int? = null,
    @SerializedName("module") val module: String? = null,
    @SerializedName("can_view") val canView: Int? = null,
    @SerializedName("can_create") val canCreate: Int? = null,
    @SerializedName("can_edit") val canEdit: Int? = null,
    @SerializedName("can_delete") val canDelete: Int? = null,
    @SerializedName("can_export") val canExport: Int? = null
)

data class LoginResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("user") val user: UserDto
)

data class RefreshResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null
)

/** Body for POST /api/users (Super Admin). */
data class UserCreateRequest(
    @SerializedName("personnel_id") val personnelId: Int,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("role_id") val roleId: Int
)

/**
 * Body for PUT /api/users/{id}. A null password is sent as JSON null and
 * ignored by the backend (isset check), keeping the current password.
 */
data class UserUpdateRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String?,
    @SerializedName("role_id") val roleId: Int,
    @SerializedName("is_active") val isActive: Int
)
