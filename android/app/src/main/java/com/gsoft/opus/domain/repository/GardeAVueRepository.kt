package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.GardeAVue
import com.gsoft.opus.domain.model.GardeAVueAttachment
import com.gsoft.opus.domain.repository.UploadFile

/** Input data for creating/updating a garde à vue. */
data class GardeAVueFormData(
    val nom: String,
    val prenoms: String? = null,
    val dateNaissance: String?,
    val adresse: String?,
    val enqueteurPermance: String?,
    val opjGav: String?,
    val motif: String?,
    val etatSante: String?,
    val droitsNotifies: String?,
    val personneContacter: String?,
    val debutGav: String?,
    val finGav: String?,
    val prolongationGav: String?
)

interface GardeAVueRepository {
    suspend fun getGardeAVueList(
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Resource<List<GardeAVue>>

    suspend fun getGardeAVue(id: Int): Resource<GardeAVue>
    suspend fun createGardeAVue(data: GardeAVueFormData): Resource<GardeAVue>
    suspend fun updateGardeAVue(id: Int, data: GardeAVueFormData): Resource<GardeAVue>
    suspend fun deleteGardeAVue(id: Int): Resource<Unit>

    suspend fun getGardeAVueAttachments(gardeAVueId: Int): Resource<List<GardeAVueAttachment>>
    suspend fun addGardeAVueAttachment(gardeAVueId: Int, title: String, file: UploadFile): Resource<GardeAVueAttachment>
    suspend fun updateGardeAVueAttachmentTitle(gardeAVueId: Int, attachId: Int, title: String): Resource<GardeAVueAttachment>
    suspend fun deleteGardeAVueAttachment(gardeAVueId: Int, attachId: Int): Resource<Unit>
}
