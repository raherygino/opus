package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.RegistreEnquete
import com.gsoft.opus.domain.model.RegistreEnqueteAttachment

/** Input data for creating/updating a registre d'enquête entry. */
data class RegistreEnqueteFormData(
    val numero: String?,
    val dateOuverture: String,
    val numeroDossier: String?,
    val natureInfraction: String,
    val dateLieuFaits: String?,
    val plaignant: String?,
    val miseEnCause: String?,
    val enqueteurPersonnelId: Int?,
    val opjPersonnelId: Int?,
    val statut: String?,
    val observations: String?
)

interface RegistreEnqueteRepository {
    suspend fun getEnqueteList(search: String? = null): Resource<List<RegistreEnquete>>
    suspend fun getEnquete(id: Int): Resource<RegistreEnquete>
    suspend fun peekNextNumber(): Resource<String>
    suspend fun createEnquete(data: RegistreEnqueteFormData): Resource<RegistreEnquete>
    suspend fun updateEnquete(id: Int, data: RegistreEnqueteFormData): Resource<RegistreEnquete>
    suspend fun deleteEnquete(id: Int): Resource<Unit>

    suspend fun getEnqueteAttachments(enqueteId: Int): Resource<List<RegistreEnqueteAttachment>>
    suspend fun addEnqueteAttachment(enqueteId: Int, title: String, file: UploadFile): Resource<RegistreEnqueteAttachment>
    suspend fun updateEnqueteAttachmentTitle(enqueteId: Int, attachId: Int, title: String): Resource<RegistreEnqueteAttachment>
    suspend fun deleteEnqueteAttachment(enqueteId: Int, attachId: Int): Resource<Unit>
}
