package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Requisition
import com.gsoft.opus.domain.model.RequisitionAttachment

/** Input data for creating/updating a requisition. */
data class RequisitionFormData(
    val type: String,
    val dateRequisition: String,
    val numero: String?,
    val numeroTtr: String?,
    val nomSubstitut: String?,
    val affaire: String,
    val numeroDossier: String?,
    val opj: String?
)

interface RequisitionRepository {
    suspend fun getRequisitionList(
        type: String? = null,
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Resource<List<Requisition>>

    suspend fun getRequisition(id: Int): Resource<Requisition>
    suspend fun createRequisition(data: RequisitionFormData): Resource<Requisition>
    suspend fun updateRequisition(id: Int, data: RequisitionFormData): Resource<Requisition>
    suspend fun deleteRequisition(id: Int): Resource<Unit>
    suspend fun peekRequisitionNumber(): Resource<String>

    suspend fun getRequisitionAttachments(requisitionId: Int): Resource<List<RequisitionAttachment>>
    suspend fun addRequisitionAttachment(requisitionId: Int, title: String, file: UploadFile): Resource<RequisitionAttachment>
    suspend fun updateRequisitionAttachmentTitle(requisitionId: Int, attachId: Int, title: String): Resource<RequisitionAttachment>
    suspend fun deleteRequisitionAttachment(requisitionId: Int, attachId: Int): Resource<Unit>
}
