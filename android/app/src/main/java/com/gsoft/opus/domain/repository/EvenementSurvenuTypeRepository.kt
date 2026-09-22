package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource

/** A type label row from the evenement_survenu_type catalog. */
data class EvenementSurvenuType(
    val id: Int,
    val label: String
)

/** Input for creating/renaming an event type. */
data class EvenementSurvenuTypeFormData(
    val label: String
)

interface EvenementSurvenuTypeRepository {
    suspend fun getTypes(): Resource<List<EvenementSurvenuType>>
    suspend fun createType(data: EvenementSurvenuTypeFormData): Resource<EvenementSurvenuType>
    suspend fun updateType(id: Int, data: EvenementSurvenuTypeFormData): Resource<EvenementSurvenuType>
    suspend fun deleteType(id: Int): Resource<Unit>
}
