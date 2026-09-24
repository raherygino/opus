package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.DispositifExceptionnel

/** One "Effectif engagé" sector row. */
data class DispositifEffectifInput(
    val secteur: String,
    val chefElementContact: String? = null,
    val controleContact: String? = null,
    val materielsArmements: String? = null,
    val missions: String? = null
)

/** Input data for creating/updating a dispositif exceptionnel. */
data class DispositifExceptionnelFormData(
    val natureEvenement: String,
    val dateDebut: String,
    val dateFin: String,
    val effectifs: List<DispositifEffectifInput> = emptyList()
)

interface DispositifExceptionnelRepository {
    suspend fun getDispositifList(search: String? = null): Resource<List<DispositifExceptionnel>>
    suspend fun getDispositif(id: Int): Resource<DispositifExceptionnel>
    suspend fun createDispositif(data: DispositifExceptionnelFormData): Resource<DispositifExceptionnel>
    suspend fun updateDispositif(id: Int, data: DispositifExceptionnelFormData): Resource<DispositifExceptionnel>
    suspend fun deleteDispositif(id: Int): Resource<Unit>
}
