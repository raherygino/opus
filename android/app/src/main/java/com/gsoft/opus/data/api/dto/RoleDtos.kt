package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName
import com.gsoft.opus.domain.model.Role

data class RoleDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("permissions") val permissions: List<PermissionDto>? = null
)

/** Response from GET /api/roles/{id}/permissions. */
data class RolePermissionsResultDto(
    @SerializedName("role") val role: RoleDto? = null,
    @SerializedName("permissions") val permissions: List<PermissionDto>? = null
)

data class RoleCreateRequest(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?
)

data class RoleUpdateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?
)

/** The five action flags of one module, as sent to PUT /api/roles/{id}/permissions. */
data class ModulePermissionFlagsDto(
    @SerializedName("can_view") val canView: Int,
    @SerializedName("can_create") val canCreate: Int,
    @SerializedName("can_edit") val canEdit: Int,
    @SerializedName("can_delete") val canDelete: Int,
    @SerializedName("can_export") val canExport: Int
)

data class RolePermissionsUpdateRequest(
    @SerializedName("permissions") val permissions: Map<String, ModulePermissionFlagsDto>
)

fun RoleDto.toDomain(): Role = Role(
    id = id,
    code = code,
    name = name,
    description = description,
    permissions = permissions?.map { it.toDomain() } ?: emptyList()
)
