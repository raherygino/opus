package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.SituationGav
import com.gsoft.opus.domain.model.SituationGavAttachment
import com.gsoft.opus.domain.repository.UploadFile

/** Input data for creating/updating a situation GAV (contrôle d'une personne en GAV). */
data class SituationGavFormData(
    val gardeAVueId: Int,
    val dateControle: String,
    val agentControleId: Int?,
    val etatGeneral: String?,
    val observations: String?,
    val mesuresPrises: String?
)

interface SituationGavRepository {
    suspend fun getSituationGavList(
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        gardeAVueId: Int? = null
    ): Resource<List<SituationGav>>

    suspend fun getSituationGav(id: Int): Resource<SituationGav>
    suspend fun createSituationGav(data: SituationGavFormData): Resource<SituationGav>
    suspend fun updateSituationGav(id: Int, data: SituationGavFormData): Resource<SituationGav>
    suspend fun deleteSituationGav(id: Int): Resource<Unit>

    suspend fun getSituationGavAttachments(situationGavId: Int): Resource<List<SituationGavAttachment>>
    suspend fun addSituationGavAttachment(situationGavId: Int, title: String, file: UploadFile): Resource<SituationGavAttachment>
    suspend fun updateSituationGavAttachmentTitle(situationGavId: Int, attachId: Int, title: String): Resource<SituationGavAttachment>
    suspend fun deleteSituationGavAttachment(situationGavId: Int, attachId: Int): Resource<Unit>
}
