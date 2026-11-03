package com.gsoft.opus.presentation.roles

object RoleFormValidator {

    fun validate(name: String): String? {
        if (name.isBlank()) return "Le nom du rôle est requis"
        if (name.trim().length > 100) return "Le nom du rôle ne doit pas dépasser 100 caractères"
        return null
    }
}
