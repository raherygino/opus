package com.gsoft.opus.data.api.dto

import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.Permission
import com.gsoft.opus.domain.model.User

fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    roleId = roleId,
    roleCode = roleCode,
    roleName = roleName,
    personnelId = personnelId,
    isActive = (isActive ?: 1) == 1,
    permissions = permissions?.map { it.toDomain() } ?: emptyList(),
    firstName = firstname,
    lastName = lastname,
    photo = photo,
    grade = grade,
    affectation = affectation
)

fun PermissionDto.toDomain(): Permission = Permission(
    id = id,
    module = module ?: "",
    canView = (canView ?: 0) == 1,
    canCreate = (canCreate ?: 0) == 1,
    canEdit = (canEdit ?: 0) == 1,
    canDelete = (canDelete ?: 0) == 1,
    canExport = (canExport ?: 0) == 1
)

fun LoginResponseDto.toDomain(): AuthResult = AuthResult(
    accessToken = accessToken,
    refreshToken = refreshToken,
    user = user.toDomain()
)
