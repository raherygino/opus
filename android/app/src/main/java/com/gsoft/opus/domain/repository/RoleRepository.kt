package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Permission
import com.gsoft.opus.domain.model.Role

/** Input data for creating/updating a role. */
data class RoleFormData(
    val name: String,
    val description: String?
)

/** The five action flags of a single module's permission row. */
data class ModulePermissions(
    val canView: Boolean = false,
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val canExport: Boolean = false
) {
    val allGranted: Boolean
        get() = canView && canCreate && canEdit && canDelete && canExport

    companion object {
        val ALL = ModulePermissions(true, true, true, true, true)
        val NONE = ModulePermissions()
    }
}

interface RoleRepository {
    suspend fun getRoles(): Resource<List<Role>>
    suspend fun getRole(id: Int): Resource<Role>
    suspend fun createRole(data: RoleFormData): Resource<Role>
    suspend fun updateRole(id: Int, data: RoleFormData): Resource<Role>
    suspend fun deleteRole(id: Int): Resource<Unit>
    suspend fun updateRolePermissions(
        id: Int,
        permissions: Map<String, ModulePermissions>
    ): Resource<List<Permission>>
}
