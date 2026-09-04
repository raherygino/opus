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

    fun validateConsommation(quantite: String, stockDisponible: Int): String? {
        val q = quantite.trim()
        if (q.isEmpty() || q.toIntOrNull() == null) {
            return "La quantité doit être un nombre entier positif"
        }
        val qInt = q.toInt()
        if (qInt <= 0) return "La quantité doit être un nombre entier positif"
        if (qInt > stockDisponible) {
            return "Stock insuffisant. Stock actuel : $stockDisponible"
        }
        return null
    }
}
