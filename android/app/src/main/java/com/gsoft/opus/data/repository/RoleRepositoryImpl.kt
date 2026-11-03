package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.ModulePermissionFlagsDto
import com.gsoft.opus.data.api.dto.RoleCreateRequest
import com.gsoft.opus.data.api.dto.RolePermissionsUpdateRequest
import com.gsoft.opus.data.api.dto.RoleUpdateRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Permission
import com.gsoft.opus.domain.model.Role
import com.gsoft.opus.domain.repository.ModulePermissions
import com.gsoft.opus.domain.repository.RoleFormData
import com.gsoft.opus.domain.repository.RoleRepository
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RoleRepository {

    companion object {
        private const val TAG = "RoleRepo"
    }

    private fun <T> retrofit2.Response<com.gsoft.opus.data.api.dto.ApiResponse<T>>.extract(defaultError: String): Resource<T> {
        return if (isSuccessful && body()?.success == true) {
            val data = body()!!.data
            if (data != null) Resource.success(data)
            else Resource.error(body()?.message ?: defaultError, code())
        } else {
            val errors = body()?.errors?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }
            Resource.error(errors ?: body()?.message ?: defaultError, code())
        }
    }

    private fun failureMessage(e: Exception, what: String): String {
        return if (e is IOException) {
            "Erreur réseau. Vérifiez votre connexion."
        } else {
            Log.e(TAG, "$what failed", e)
            "Une erreur inattendue s'est produite."
        }
    }

    /**
     * Derive the role code from its display name — mirrors the desktop
     * `code = name.toUpperCase().replace(/\s+/g,"_").replace(/[^A-Z0-9_]/g,"")`.
     */
    private fun generateCode(name: String): String =
        name.uppercase(Locale.ROOT)
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^A-Z0-9_]"), "")

    override suspend fun getRoles(): Resource<List<Role>> {
        return try {
            apiService.getRoleList()
                .extract("Impossible de charger la liste des rôles")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRoles"))
        }
    }

    override suspend fun getRole(id: Int): Resource<Role> {
        return try {
            apiService.getRole(id)
                .extract("Rôle introuvable")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRole"))
        }
    }

    override suspend fun createRole(data: RoleFormData): Resource<Role> {
        return try {
            apiService.createRole(
                RoleCreateRequest(
                    code = generateCode(data.name),
                    name = data.name.trim(),
                    description = data.description?.trim()?.ifBlank { null }
                )
            ).extract("Impossible de créer le rôle")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createRole"))
        }
    }

    override suspend fun updateRole(id: Int, data: RoleFormData): Resource<Role> {
        return try {
            apiService.updateRole(
                id,
                RoleUpdateRequest(
                    name = data.name.trim(),
                    description = data.description?.trim()?.ifBlank { null }
                )
            ).extract("Impossible de mettre à jour le rôle")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateRole"))
        }
    }

    override suspend fun deleteRole(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRole(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer ce rôle", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteRole"))
        }
    }

    override suspend fun updateRolePermissions(
        id: Int,
        permissions: Map<String, ModulePermissions>
    ): Resource<List<Permission>> {
        return try {
            apiService.updateRolePermissions(
                id,
                RolePermissionsUpdateRequest(
                    permissions = permissions.mapValues { (_, p) ->
                        ModulePermissionFlagsDto(
                            canView = if (p.canView) 1 else 0,
                            canCreate = if (p.canCreate) 1 else 0,
                            canEdit = if (p.canEdit) 1 else 0,
                            canDelete = if (p.canDelete) 1 else 0,
                            canExport = if (p.canExport) 1 else 0
                        )
                    }
                )
            ).extract("Impossible de mettre à jour les permissions")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateRolePermissions"))
        }
    }
}
