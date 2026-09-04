package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Arme
import com.gsoft.opus.domain.model.ArmeMunitionsConsommation
import com.gsoft.opus.domain.model.TypeArme

data class TypeArmeFormData(
    val nom: String,
    val description: String? = null,
    val munitionsStock: Int = 0
)

data class ArmeFormData(
    val typeArmeId: Int,
    val matricule: String,
    val munitionsStock: Int = 0
)

data class ConsommationData(
    val agentId: Int? = null,
    val quantite: Int,
    val armementId: Int? = null
)

interface ArmeRepository {
    // TypeArme
    suspend fun getTypeArmeList(search: String? = null): Resource<List<TypeArme>>
    suspend fun getTypeArme(id: Int): Resource<TypeArme>
    suspend fun createTypeArme(data: TypeArmeFormData): Resource<TypeArme>
    suspend fun updateTypeArme(id: Int, data: TypeArmeFormData): Resource<TypeArme>
    suspend fun deleteTypeArme(id: Int): Resource<Unit>

    // Arme
    suspend fun getArmeList(typeArmeId: Int? = null, search: String? = null): Resource<List<Arme>>
    suspend fun getArme(id: Int): Resource<Arme>
    suspend fun createArme(data: ArmeFormData): Resource<Arme>
    suspend fun updateArme(id: Int, data: ArmeFormData): Resource<Arme>
    suspend fun deleteArme(id: Int): Resource<Unit>

    // Consommation
    suspend fun recordConsommation(armeId: Int, data: ConsommationData): Resource<Arme>
    suspend fun getConsommations(armeId: Int): Resource<List<ArmeMunitionsConsommation>>
}
