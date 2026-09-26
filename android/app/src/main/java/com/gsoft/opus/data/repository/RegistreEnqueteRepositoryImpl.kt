package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.RegistreEnqueteRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.RegistreEnquete
import com.gsoft.opus.domain.model.RegistreEnqueteAttachment
import com.gsoft.opus.domain.repository.RegistreEnqueteFormData
import com.gsoft.opus.domain.repository.RegistreEnqueteRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistreEnqueteRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RegistreEnqueteRepository {

    companion object {
        private const val TAG = "RegistreEnqueteRepo"
    }

    private fun RegistreEnqueteFormData.toRequest() = RegistreEnqueteRequest(
        numero = numero,
        dateOuverture = dateOuverture,
        numeroDossier = numeroDossier,
        natureInfraction = natureInfraction,
        dateLieuFaits = dateLieuFaits,
        plaignant = plaignant,
        miseEnCause = miseEnCause,
        enqueteurPersonnelId = enqueteurPersonnelId,
        opjPersonnelId = opjPersonnelId,
        statut = statut,
        observations = observations
    )

    private fun <T> Response<com.gsoft.opus.data.api.dto.ApiResponse<T>>.extract(defaultError: String): Resource<T> {
        return try {
            if (isSuccessful) {
                val body = body()
                if (body?.success == true && body.data != null) {
                    Resource.success(body.data)
                } else {
                    Resource.error(body?.message ?: defaultError, code())
                }
            } else {
                Resource.error(errorBody()?.string() ?: defaultError, code())
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            Resource.error("Erreur réseau: ${e.message ?: "connexion impossible"}")
        }
    }

    private fun failureMessage(e: Exception, op: String): String {
        Log.e(TAG, "$op failed", e)
        return e.message ?: "Une erreur est survenue"
    }

    override suspend fun getEnqueteList(search: String?): Resource<List<RegistreEnquete>> {
        return try {
            apiService.getRegistreEnqueteList(search)
                .extract("Impossible de charger le registre d'enquête")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getEnqueteList"))
        }
    }

    override suspend fun getEnquete(id: Int): Resource<RegistreEnquete> {
        return try {
            apiService.getRegistreEnquete(id).extract("Entrée introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getEnquete"))
        }
    }

    override suspend fun peekNextNumber(): Resource<String> {
        return try {
            apiService.getRegistreEnqueteNextNumber()
                .extract("Impossible de charger le numéro")
                .map { it.numero }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "peekNextNumber"))
        }
    }

    override suspend fun createEnquete(data: RegistreEnqueteFormData): Resource<RegistreEnquete> {
        return try {
            apiService.createRegistreEnquete(data.toRequest())
                .extract("Impossible d'enregistrer l'entrée")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createEnquete"))
        }
    }

    override suspend fun updateEnquete(id: Int, data: RegistreEnqueteFormData): Resource<RegistreEnquete> {
        return try {
            apiService.updateRegistreEnquete(id, data.toRequest())
                .extract("Impossible de modifier l'entrée")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateEnquete"))
        }
    }

    override suspend fun deleteEnquete(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRegistreEnquete(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteEnquete"))
        }
    }

    override suspend fun getEnqueteAttachments(enqueteId: Int): Resource<List<RegistreEnqueteAttachment>> {
        return try {
            apiService.getRegistreEnqueteAttachments(enqueteId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getEnqueteAttachments"))
        }
    }

    override suspend fun addEnqueteAttachment(enqueteId: Int, title: String, file: UploadFile): Resource<RegistreEnqueteAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createRegistreEnqueteAttachment(enqueteId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addEnqueteAttachment"))
        }
    }

    override suspend fun updateEnqueteAttachmentTitle(enqueteId: Int, attachId: Int, title: String): Resource<RegistreEnqueteAttachment> {
        return try {
            apiService.updateRegistreEnqueteAttachmentTitle(enqueteId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateEnqueteAttachmentTitle"))
        }
    }

    override suspend fun deleteEnqueteAttachment(enqueteId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRegistreEnqueteAttachment(enqueteId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteEnqueteAttachment"))
        }
    }
}
