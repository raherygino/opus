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
    @SerializedName("permissions") val permissions: List<PermissionDto>? = null
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
    @SerializedName("access_token") val accessToken: String
)
