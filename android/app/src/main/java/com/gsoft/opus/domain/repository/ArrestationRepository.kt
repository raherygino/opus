package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Arrestation
import com.gsoft.opus.domain.model.ArrestationAttachment

/** Input data for creating/updating an arrestation. */
data class ArrestationFormData(
    val numero: String?,
    val dateHeureArrestation: String,
    val personneNom: String,
    val lieuArrestation: String?,
    val motif: String?,
    val policiers: String?,
    val numeroDossier: String?,
    val observations: String?
)

interface ArrestationRepository {
    suspend fun getArrestationList(search: String? = null): Resource<List<Arrestation>>
    suspend fun getArrestation(id: Int): Resource<Arrestation>
    suspend fun peekArrestationNumber(): Resource<String>
    suspend fun createArrestation(data: ArrestationFormData): Resource<Arrestation>
    suspend fun updateArrestation(id: Int, data: ArrestationFormData): Resource<Arrestation>
    suspend fun deleteArrestation(id: Int): Resource<Unit>

    suspend fun getArrestationAttachments(arrestationId: Int): Resource<List<ArrestationAttachment>>
    suspend fun addArrestationAttachment(arrestationId: Int, title: String, file: UploadFile): Resource<ArrestationAttachment>
    suspend fun updateArrestationAttachmentTitle(arrestationId: Int, attachId: Int, title: String): Resource<ArrestationAttachment>
    suspend fun deleteArrestationAttachment(arrestationId: Int, attachId: Int): Resource<Unit>
}
