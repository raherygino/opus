package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.RegistreDeferrement
import com.gsoft.opus.domain.model.RegistreDeferrementAttachment

/** Input data for creating/updating a registre de déferrement entry. */
data class RegistreDeferrementFormData(
    val numero: String?,
    val dateHeureDeferrement: String,
    val personneNom: String,
    val dateLieuNaissance: String?,
    val infraction: String?,
    val numeroDossier: String?,
    val autorite: String?,
    val destination: String?,
    val escorte: String?,
    val suiteDonnee: String?,
    val observations: String?
)

interface RegistreDeferrementRepository {
    suspend fun getDeferrementList(search: String? = null): Resource<List<RegistreDeferrement>>
    suspend fun getDeferrement(id: Int): Resource<RegistreDeferrement>
    suspend fun peekNextNumber(): Resource<String>
    suspend fun createDeferrement(data: RegistreDeferrementFormData): Resource<RegistreDeferrement>
    suspend fun updateDeferrement(id: Int, data: RegistreDeferrementFormData): Resource<RegistreDeferrement>
    suspend fun deleteDeferrement(id: Int): Resource<Unit>

    suspend fun getDeferrementAttachments(deferrementId: Int): Resource<List<RegistreDeferrementAttachment>>
    suspend fun addDeferrementAttachment(deferrementId: Int, title: String, file: UploadFile): Resource<RegistreDeferrementAttachment>
    suspend fun updateDeferrementAttachmentTitle(deferrementId: Int, attachId: Int, title: String): Resource<RegistreDeferrementAttachment>
    suspend fun deleteDeferrementAttachment(deferrementId: Int, attachId: Int): Resource<Unit>
}
