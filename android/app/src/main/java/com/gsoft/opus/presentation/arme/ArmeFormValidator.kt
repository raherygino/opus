package com.gsoft.opus.presentation.arme

object ArmeFormValidator {

    fun validateArme(typeArmeId: Int, matricule: String): String? {
        if (typeArmeId <= 0) return "Le type d'arme est requis"
        if (matricule.isBlank()) return "Le matricule est requis"
        return null
    }

    fun validateTypeArme(nom: String): String? {
        if (nom.isBlank()) return "Le nom du type d'arme est requis"
        return null
    }
}
