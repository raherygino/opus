package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource

/** A category label row from the main_courante_categorie catalog. */
data class MainCouranteCategorie(
    val id: Int,
    val label: String
)

/** Input for creating/renaming a category. */
data class MainCouranteCategorieFormData(
    val label: String
)

interface MainCouranteCategorieRepository {
    suspend fun getCategories(): Resource<List<MainCouranteCategorie>>
    suspend fun createCategory(data: MainCouranteCategorieFormData): Resource<MainCouranteCategorie>
    suspend fun updateCategory(id: Int, data: MainCouranteCategorieFormData): Resource<MainCouranteCategorie>
    suspend fun deleteCategory(id: Int): Resource<Unit>
}
