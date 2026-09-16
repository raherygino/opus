package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Perquisition
import com.gsoft.opus.domain.model.PerquisitionAttachment

/** Input data for creating/updating a perquisition. */
data class PerquisitionFormData(
    val numero: String?,
    val numeroTtr: String?,
    val substitut: String?,
    val affaire: String,
    val motif: String?
)

interface PerquisitionRepository {
    suspend fun getPerquisitionList(search: String? = null): Resource<List<Perquisition>>
    suspend fun getPerquisition(id: Int): Resource<Perquisition>
    suspend fun peekNextNumber(): Resource<String>
    suspend fun createPerquisition(data: PerquisitionFormData): Resource<Perquisition>
    suspend fun updatePerquisition(id: Int, data: PerquisitionFormData): Resource<Perquisition>
    suspend fun deletePerquisition(id: Int): Resource<Unit>

    suspend fun getPerquisitionAttachments(perquisitionId: Int): Resource<List<PerquisitionAttachment>>
    suspend fun addPerquisitionAttachment(perquisitionId: Int, title: String, file: UploadFile): Resource<PerquisitionAttachment>
    suspend fun updatePerquisitionAttachmentTitle(perquisitionId: Int, attachId: Int, title: String): Resource<PerquisitionAttachment>
    suspend fun deletePerquisitionAttachment(perquisitionId: Int, attachId: Int): Resource<Unit>
}
