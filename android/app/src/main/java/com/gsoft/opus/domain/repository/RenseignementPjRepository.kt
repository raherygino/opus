package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.RenseignementPj
import com.gsoft.opus.domain.model.RenseignementPjAttachment

/** Input data for creating/updating a renseignement PJ. */
data class RenseignementPjFormData(
    val natureInfraction: String,
    val dateLieuFaits: String?,
    val circonstances: String?,
    val prejudices: String?
)

interface RenseignementPjRepository {
    suspend fun getRenseignementPjList(search: String? = null): Resource<List<RenseignementPj>>
    suspend fun getRenseignementPj(id: Int): Resource<RenseignementPj>
    suspend fun createRenseignementPj(data: RenseignementPjFormData): Resource<RenseignementPj>
    suspend fun updateRenseignementPj(id: Int, data: RenseignementPjFormData): Resource<RenseignementPj>
    suspend fun deleteRenseignementPj(id: Int): Resource<Unit>

    suspend fun getRenseignementPjAttachments(renseignementId: Int): Resource<List<RenseignementPjAttachment>>
    suspend fun addRenseignementPjAttachment(renseignementId: Int, title: String, file: UploadFile): Resource<RenseignementPjAttachment>
    suspend fun updateRenseignementPjAttachmentTitle(renseignementId: Int, attachId: Int, title: String): Resource<RenseignementPjAttachment>
    suspend fun deleteRenseignementPjAttachment(renseignementId: Int, attachId: Int): Resource<Unit>
}
