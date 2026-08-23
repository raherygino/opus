package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Passation
import com.gsoft.opus.domain.model.PassationAttachment
import com.gsoft.opus.domain.model.VerifiedIdentity

/** Input data for creating/updating a passation record. */
data class PassationFormData(
    val datePassation: String,
    val heurePassation: String,
    val chefMontantUserId: Int,
    val chefMontantGrade: String,
    val chefMontantLastname: String,
    val instructionsAutorite: String,
    val incidentsSurvenus: String
)

interface PassationRepository {
    suspend fun getPassationList(
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Resource<List<Passation>>
    suspend fun getPassation(id: Int): Resource<Passation>
    suspend fun createPassation(data: PassationFormData): Resource<Passation>
    suspend fun updatePassation(id: Int, data: PassationFormData): Resource<Passation>
    suspend fun deletePassation(id: Int): Resource<Unit>

    suspend fun getAttachments(passationId: Int): Resource<List<PassationAttachment>>
    suspend fun addAttachment(passationId: Int, title: String, file: UploadFile): Resource<PassationAttachment>
    suspend fun updateAttachmentTitle(passationId: Int, attachId: Int, title: String): Resource<PassationAttachment>
    suspend fun deleteAttachment(passationId: Int, attachId: Int): Resource<Unit>

    /** Verify the chef de poste montant credentials without creating a session. */
    suspend fun verifyIdentity(username: String, password: String): Resource<VerifiedIdentity>
}
