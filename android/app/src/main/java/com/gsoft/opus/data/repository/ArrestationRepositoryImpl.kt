package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.ArrestationRequest
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Arrestation
import com.gsoft.opus.domain.model.ArrestationAttachment
import com.gsoft.opus.domain.repository.ArrestationFormData
import com.gsoft.opus.domain.repository.ArrestationRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArrestationRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ArrestationRepository {

    companion object {
        private const val TAG = "ArrestationRepo"
    }

    private fun ArrestationFormData.toRequest() = ArrestationRequest(
        numero = numero,
        dateHeureArrestation = dateHeureArrestation,
        personneNom = personneNom,
        lieuArrestation = lieuArrestation,
        motif = motif,
        policiers = policiers,
        numeroDossier = numeroDossier,
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

    override suspend fun getArrestationList(search: String?): Resource<List<Arrestation>> {
        return try {
            apiService.getArrestationList(search)
                .extract("Impossible de charger les arrestations")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getArrestationList"))
        }
    }

    override suspend fun getArrestation(id: Int): Resource<Arrestation> {
        return try {
            apiService.getArrestation(id).extract("Arrestation introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getArrestation"))
        }
    }

    override suspend fun peekArrestationNumber(): Resource<String> {
        return try {
            apiService.peekArrestationNumber()
                .extract("Impossible de récupérer le numéro suggéré")
                .map { it.numero }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "peekArrestationNumber"))
        }
    }

    override suspend fun createArrestation(data: ArrestationFormData): Resource<Arrestation> {
        return try {
            apiService.createArrestation(data.toRequest())
                .extract("Impossible d'enregistrer l'arrestation")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createArrestation"))
        }
    }

    override suspend fun updateArrestation(id: Int, data: ArrestationFormData): Resource<Arrestation> {
        return try {
            apiService.updateArrestation(id, data.toRequest())
                .extract("Impossible de modifier l'arrestation")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateArrestation"))
        }
    }

    override suspend fun deleteArrestation(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteArrestation(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteArrestation"))
        }
    }

    override suspend fun getArrestationAttachments(arrestationId: Int): Resource<List<ArrestationAttachment>> {
        return try {
            apiService.getArrestationAttachments(arrestationId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getArrestationAttachments"))
        }
    }

    override suspend fun addArrestationAttachment(arrestationId: Int, title: String, file: UploadFile): Resource<ArrestationAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createArrestationAttachment(arrestationId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addArrestationAttachment"))
        }
    }

    override suspend fun updateArrestationAttachmentTitle(arrestationId: Int, attachId: Int, title: String): Resource<ArrestationAttachment> {
        return try {
            apiService.updateArrestationAttachmentTitle(arrestationId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateArrestationAttachmentTitle"))
        }
    }

    override suspend fun deleteArrestationAttachment(arrestationId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteArrestationAttachment(arrestationId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteArrestationAttachment"))
        }
    }
}
