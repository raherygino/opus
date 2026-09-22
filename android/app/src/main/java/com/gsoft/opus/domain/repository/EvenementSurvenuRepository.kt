package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.EvenementSurvenu
import com.gsoft.opus.domain.model.EvenementSurvenuAttachment

/** Input data for creating/updating an évènement survenu. */
data class EvenementSurvenuFormData(
    val dateEvenement: String,
    val heureEvenement: String,
    val typeEvenement: String,
    val lieuExact: String,
    val auteursPresumes: String? = null,
    val victimes: String? = null,
    val temoins: String? = null,
    val mesuresPrises: String? = null,
    /** GPS position captured at record time (mobile only). */
    val latitude: Double? = null,
    val longitude: Double? = null
)

interface EvenementSurvenuRepository {
    suspend fun getEvenementList(search: String? = null): Resource<List<EvenementSurvenu>>
    suspend fun getEvenement(id: Int): Resource<EvenementSurvenu>
    suspend fun createEvenement(data: EvenementSurvenuFormData): Resource<EvenementSurvenu>
    suspend fun updateEvenement(id: Int, data: EvenementSurvenuFormData): Resource<EvenementSurvenu>
    suspend fun deleteEvenement(id: Int): Resource<Unit>

    suspend fun getAttachments(evenementId: Int): Resource<List<EvenementSurvenuAttachment>>
    suspend fun addAttachment(evenementId: Int, title: String, file: UploadFile): Resource<EvenementSurvenuAttachment>
    suspend fun updateAttachmentTitle(evenementId: Int, attachId: Int, title: String): Resource<EvenementSurvenuAttachment>
    suspend fun deleteAttachment(evenementId: Int, attachId: Int): Resource<Unit>
}
