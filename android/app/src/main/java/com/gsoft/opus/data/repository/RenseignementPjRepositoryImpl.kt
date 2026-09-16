package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.RenseignementPjRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.RenseignementPj
import com.gsoft.opus.domain.model.RenseignementPjAttachment
import com.gsoft.opus.domain.repository.RenseignementPjFormData
import com.gsoft.opus.domain.repository.RenseignementPjRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RenseignementPjRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RenseignementPjRepository {

    companion object {
        private const val TAG = "RenseignementPjRepo"
    }

    private fun RenseignementPjFormData.toRequest() = RenseignementPjRequest(
        natureInfraction = natureInfraction,
        dateLieuFaits = dateLieuFaits,
        circonstances = circonstances,
        prejudices = prejudices
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

    override suspend fun getRenseignementPjList(search: String?): Resource<List<RenseignementPj>> {
        return try {
            apiService.getRenseignementPjList(search)
                .extract("Impossible de charger les renseignements")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRenseignementPjList"))
        }
    }

    override suspend fun getRenseignementPj(id: Int): Resource<RenseignementPj> {
        return try {
            apiService.getRenseignementPj(id).extract("Renseignement introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRenseignementPj"))
        }
    }

    override suspend fun createRenseignementPj(data: RenseignementPjFormData): Resource<RenseignementPj> {
        return try {
            apiService.createRenseignementPj(data.toRequest())
                .extract("Impossible d'enregistrer le renseignement")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createRenseignementPj"))
        }
    }

    override suspend fun updateRenseignementPj(id: Int, data: RenseignementPjFormData): Resource<RenseignementPj> {
        return try {
            apiService.updateRenseignementPj(id, data.toRequest())
                .extract("Impossible de modifier le renseignement")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateRenseignementPj"))
        }
    }

    override suspend fun deleteRenseignementPj(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRenseignementPj(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteRenseignementPj"))
        }
    }

    override suspend fun getRenseignementPjAttachments(renseignementId: Int): Resource<List<RenseignementPjAttachment>> {
        return try {
            apiService.getRenseignementPjAttachments(renseignementId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRenseignementPjAttachments"))
        }
    }

    override suspend fun addRenseignementPjAttachment(renseignementId: Int, title: String, file: UploadFile): Resource<RenseignementPjAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createRenseignementPjAttachment(renseignementId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addRenseignementPjAttachment"))
        }
    }

    override suspend fun updateRenseignementPjAttachmentTitle(renseignementId: Int, attachId: Int, title: String): Resource<RenseignementPjAttachment> {
        return try {
            apiService.updateRenseignementPjAttachmentTitle(renseignementId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateRenseignementPjAttachmentTitle"))
        }
    }

    override suspend fun deleteRenseignementPjAttachment(renseignementId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRenseignementPjAttachment(renseignementId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteRenseignementPjAttachment"))
        }
    }
}
