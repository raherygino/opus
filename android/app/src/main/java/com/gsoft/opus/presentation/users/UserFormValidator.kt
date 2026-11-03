package com.gsoft.opus.presentation.users

/** Mirrors backend UserValidator: username ≥ 3 chars, password ≥ 6 chars. */
object UserFormValidator {

    fun validate(
        isEdit: Boolean,
        personnelId: Int,
        username: String,
        password: String,
        roleId: Int
    ): String? {
        if (!isEdit && personnelId <= 0) return "Veuillez sélectionner un agent"
        if (username.isBlank()) return "Le nom d'utilisateur est requis"
        if (username.trim().length < 3) return "Le nom d'utilisateur doit contenir au moins 3 caractères"
        if (!isEdit && password.isBlank()) return "Le mot de passe est requis"
        if (password.isNotBlank() && password.length < 6) return "Le mot de passe doit contenir au moins 6 caractères"
        if (roleId <= 0) return "Veuillez sélectionner un rôle"
        return null
    }
}
