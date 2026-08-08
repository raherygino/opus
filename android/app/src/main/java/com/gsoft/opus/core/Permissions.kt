package com.gsoft.opus.core

import com.gsoft.opus.domain.model.User

enum class PermissionAction { VIEW, CREATE, EDIT, DELETE, EXPORT }

/**
 * Mirrors the desktop `hasPermission` helper: SUPER_ADMIN bypasses the
 * permission table, otherwise the role permission for the given module
 * must explicitly allow the action.
 */
fun hasPermission(user: User?, module: String, action: PermissionAction): Boolean {
    if (user == null) return false
    if (user.roleCode == "SUPER_ADMIN") return true
    val perm = user.permissions.find { it.module == module } ?: return false
    return when (action) {
        PermissionAction.VIEW -> perm.canView
        PermissionAction.CREATE -> perm.canCreate
        PermissionAction.EDIT -> perm.canEdit
        PermissionAction.DELETE -> perm.canDelete
        PermissionAction.EXPORT -> perm.canExport
    }
}
