package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Comportement
import com.gsoft.opus.domain.model.Mouvement
import com.gsoft.opus.domain.model.MouvementAttachment
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.PersonnelAttachment

/** Input data for creating/updating a personnel record. */
data class PersonnelFormData(
    val im: String,
    val grade: String,
    val lastname: String,
    val firstname: String,
    val affectation: String?,
    val phone: String?,
    val address: String?
)

/** Input data for creating a mouvement. */
data class MouvementFormData(
    val personnelId: Int,
    val im: String,
    val grade: String?,
    val service: String?,
    val nom: String?,
    val prenoms: String?,
    val typeMouvement: String,
    val dateDepart: String?,
    val days: Int?,
    val dateRetour: String?
)

/** Input data for creating a comportement record. */
data class ComportementFormData(
    val personnelId: Int,
    val im: String,
    val grade: String?,
    val service: String?,
    val nom: String?,
    val prenoms: String?,
    val type: String,
    val dateComportement: String,
    val motif: String,
    val decision: String?
)

/** A file picked from the device, ready for multipart upload. */
data class UploadFile(
    val fileName: String,
    val mimeType: String?,
    val bytes: ByteArray
)

interface PersonnelRepository {
    suspend fun getPersonnelList(search: String? = null): Resource<List<Personnel>>
    suspend fun getPersonnel(id: Int): Resource<Personnel>
    suspend fun createPersonnel(data: PersonnelFormData): Resource<Personnel>
    suspend fun updatePersonnel(id: Int, data: PersonnelFormData): Resource<Personnel>
    suspend fun deletePersonnel(id: Int): Resource<Unit>

    suspend fun getAttachments(personnelId: Int): Resource<List<PersonnelAttachment>>
    suspend fun addAttachment(personnelId: Int, title: String, file: UploadFile): Resource<PersonnelAttachment>
    suspend fun updateAttachmentTitle(personnelId: Int, attachId: Int, title: String): Resource<PersonnelAttachment>
    suspend fun deleteAttachment(personnelId: Int, attachId: Int): Resource<Unit>

    suspend fun uploadPhoto(personnelId: Int, photo: UploadFile, thumbnail: UploadFile?): Resource<Personnel>
    suspend fun deletePhoto(personnelId: Int): Resource<Personnel>
}

interface MouvementRepository {
    suspend fun getMouvementList(personnelId: Int? = null): Resource<List<Mouvement>>
    suspend fun createMouvement(data: MouvementFormData): Resource<Mouvement>
    suspend fun confirmRetour(mouvementId: Int, dateRetour: String?): Resource<Mouvement>
    suspend fun deleteMouvement(id: Int): Resource<Unit>

    suspend fun getAttachments(mouvementId: Int): Resource<List<MouvementAttachment>>
    suspend fun addAttachment(mouvementId: Int, title: String, file: UploadFile): Resource<MouvementAttachment>
    suspend fun deleteAttachment(mouvementId: Int, attachId: Int): Resource<Unit>
}

interface ComportementRepository {
    suspend fun getComportementList(
        personnelId: Int? = null,
        status: String? = null
    ): Resource<List<Comportement>>
    suspend fun createComportement(data: ComportementFormData): Resource<Comportement>
    suspend fun confirmComportement(id: Int): Resource<Comportement>
    suspend fun rejectComportement(id: Int, reason: String?): Resource<Comportement>
    suspend fun deleteComportement(id: Int): Resource<Unit>
}
