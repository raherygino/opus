package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.CodeSecretRequest
import com.gsoft.opus.data.api.dto.CodeSecretResultDto
import com.gsoft.opus.data.api.dto.PersonnelRequest
import com.gsoft.opus.data.api.dto.VerifyCodeSecretRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.PersonnelAttachment
import com.gsoft.opus.domain.repository.PersonnelFormData
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonnelRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : PersonnelRepository {

    companion object {
        private const val TAG = "PersonnelRepo"
    }

    private fun PersonnelFormData.toRequest() = PersonnelRequest(
        im = im,
        grade = grade,
        lastname = lastname,
        firstname = firstname,
        affectation = affectation?.takeIf { it.isNotBlank() },
        phone = phone?.takeIf { it.isNotBlank() },
        address = address?.takeIf { it.isNotBlank() }
    )

    private fun <T> retrofit2.Response<com.gsoft.opus.data.api.dto.ApiResponse<T>>.extract(defaultError: String): Resource<T> {
        return if (isSuccessful && body()?.success == true) {
            val data = body()!!.data
            if (data != null) Resource.success(data)
            else Resource.error(body()?.message ?: defaultError, code())
        } else {
            val errors = body()?.errors?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }
            Resource.error(errors ?: body()?.message ?: defaultError, code())
        }
    }

    private fun failureMessage(e: Exception, tag: String, what: String): String {
        return if (e is IOException) {
            "Erreur réseau. Vérifiez votre connexion."
        } else {
            Log.e(tag, "$what failed", e)
            "Une erreur inattendue s'est produite."
        }
    }

    override suspend fun getPersonnelList(search: String?): Resource<List<Personnel>> {
        return try {
            val result = apiService.getPersonnelList(search?.takeIf { it.isNotBlank() })
                .extract("Impossible de charger la liste du personnel")
            result.map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getPersonnelList"))
        }
    }

    override suspend fun getPersonnel(id: Int): Resource<Personnel> {
        return try {
            apiService.getPersonnel(id).extract("Personnel introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getPersonnel"))
        }
    }

    override suspend fun createPersonnel(data: PersonnelFormData): Resource<Personnel> {
        return try {
            apiService.createPersonnel(data.toRequest()).extract("Impossible de créer le personnel").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "createPersonnel"))
        }
    }

    override suspend fun updatePersonnel(id: Int, data: PersonnelFormData): Resource<Personnel> {
        return try {
            apiService.updatePersonnel(id, data.toRequest()).extract("Impossible de mettre à jour le personnel").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updatePersonnel"))
        }
    }

    override suspend fun deletePersonnel(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePersonnel(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer ce personnel", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deletePersonnel"))
        }
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(personnelId: Int): Resource<List<PersonnelAttachment>> {
        return try {
            apiService.getPersonnelAttachments(personnelId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getAttachments"))
        }
    }

    override suspend fun addAttachment(personnelId: Int, title: String, file: UploadFile): Resource<PersonnelAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createPersonnelAttachment(personnelId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "addAttachment"))
        }
    }

    override suspend fun updateAttachmentTitle(personnelId: Int, attachId: Int, title: String): Resource<PersonnelAttachment> {
        return try {
            apiService.updatePersonnelAttachmentTitle(personnelId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateAttachmentTitle"))
        }
    }

    override suspend fun deleteAttachment(personnelId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePersonnelAttachment(personnelId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteAttachment"))
        }
    }

    // ─── Photo ──────────────────────────────────────────────────────

    override suspend fun uploadPhoto(personnelId: Int, photo: UploadFile, thumbnail: UploadFile?): Resource<Personnel> {
        return try {
            val photoPart = MultipartBody.Part.createFormData(
                "photo", photo.fileName,
                photo.bytes.toRequestBody(photo.mimeType?.toMediaTypeOrNull())
            )
            val thumbPart = thumbnail?.let {
                MultipartBody.Part.createFormData(
                    "thumbnail", it.fileName,
                    it.bytes.toRequestBody(it.mimeType?.toMediaTypeOrNull())
                )
            }
            apiService.uploadPersonnelPhoto(personnelId, photoPart, thumbPart)
                .extract("Impossible d'enregistrer la photo")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "uploadPhoto"))
        }
    }

    override suspend fun deletePhoto(personnelId: Int): Resource<Personnel> {
        return try {
            apiService.deletePersonnelPhoto(personnelId)
                .extract("Impossible de supprimer la photo")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deletePhoto"))
        }
    }

    // ─── Code secret (Armement identity verification) ──────────────

    override suspend fun setCodeSecret(personnelId: Int, code: String?): Resource<Boolean> {
        return try {
            apiService.setPersonnelCodeSecret(personnelId, CodeSecretRequest(code))
                .extract("Impossible d'enregistrer le code secret")
                .map { it.hasCodeSecret ?: false }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "setCodeSecret"))
        }
    }

    override suspend fun verifyCodeSecret(personnelId: Int, code: String): Resource<Boolean> {
        return try {
            apiService.verifyPersonnelCodeSecret(personnelId, VerifyCodeSecretRequest(code))
                .extract("Impossible de vérifier le code secret")
                .map { it.verified ?: false }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "verifyCodeSecret"))
        }
    }
}
