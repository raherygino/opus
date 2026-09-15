package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Mandat
import com.gsoft.opus.domain.model.MandatAttachment

/** Input data for creating/updating a mandat. */
data class MandatFormData(
    val numero: String?,
    val type: String,
    val autorite: String?,
    val personneNom: String,
    val dateLieuNaissance: String?,
    val motif: String?,
    val qualificationInfraction: String?,
    val opjExecution: String?,
    val dateHeureExecution: String?,
    val lieuExecution: String?,
    val observations: String?
)

interface MandatRepository {
    suspend fun getMandatList(search: String? = null, type: String? = null): Resource<List<Mandat>>
    suspend fun getMandat(id: Int): Resource<Mandat>
    suspend fun peekMandatNumber(): Resource<String>
    suspend fun createMandat(data: MandatFormData): Resource<Mandat>
    suspend fun updateMandat(id: Int, data: MandatFormData): Resource<Mandat>
    suspend fun deleteMandat(id: Int): Resource<Unit>

    suspend fun getMandatAttachments(mandatId: Int): Resource<List<MandatAttachment>>
    suspend fun addMandatAttachment(mandatId: Int, title: String, file: UploadFile): Resource<MandatAttachment>
    suspend fun updateMandatAttachmentTitle(mandatId: Int, attachId: Int, title: String): Resource<MandatAttachment>
    suspend fun deleteMandatAttachment(mandatId: Int, attachId: Int): Resource<Unit>
}
