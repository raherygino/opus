package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.SituationGavRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.SituationGav
import com.gsoft.opus.domain.model.SituationGavAttachment
import com.gsoft.opus.domain.repository.SituationGavFormData
import com.gsoft.opus.domain.repository.SituationGavRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SituationGavRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : SituationGavRepository {

    companion object {
        private const val TAG = "SituationGavRepo"
    }

    private fun SituationGavFormData.toRequest() = SituationGavRequest(
        gardeAVueId = gardeAVueId,
        dateControle = dateControle,
        agentControleId = agentControleId,
        etatGeneral = etatGeneral,
        observations = observations,
        mesuresPrises = mesuresPrises
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
                val msg = errorBody()?.string() ?: defaultError
                Resource.error(msg, code())
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

    override suspend fun getSituationGavList(
        search: String?,
        dateFrom: String?,
        dateTo: String?,
        gardeAVueId: Int?
    ): Resource<List<SituationGav>> {
        return try {
            apiService.getSituationGavList(search, dateFrom, dateTo, gardeAVueId)
                .extract("Impossible de charger les situations GAV")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getSituationGavList"))
        }
    }

    override suspend fun getSituationGav(id: Int): Resource<SituationGav> {
        return try {
            apiService.getSituationGav(id).extract("Situation GAV introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getSituationGav"))
        }
    }

    override suspend fun createSituationGav(data: SituationGavFormData): Resource<SituationGav> {
        return try {
            apiService.createSituationGav(data.toRequest())
                .extract("Impossible d'enregistrer la situation GAV")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createSituationGav"))
        }
    }

    override suspend fun updateSituationGav(id: Int, data: SituationGavFormData): Resource<SituationGav> {
        return try {
            apiService.updateSituationGav(id, data.toRequest())
                .extract("Impossible de modifier la situation GAV")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateSituationGav"))
        }
    }

    override suspend fun deleteSituationGav(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteSituationGav(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette situation GAV", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteSituationGav"))
        }
    }

    override suspend fun getSituationGavAttachments(situationGavId: Int): Resource<List<SituationGavAttachment>> {
        return try {
            apiService.getSituationGavAttachments(situationGavId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getSituationGavAttachments"))
        }
    }

    override suspend fun addSituationGavAttachment(situationGavId: Int, title: String, file: UploadFile): Resource<SituationGavAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createSituationGavAttachment(situationGavId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addSituationGavAttachment"))
        }
    }

    override suspend fun updateSituationGavAttachmentTitle(situationGavId: Int, attachId: Int, title: String): Resource<SituationGavAttachment> {
        return try {
            apiService.updateSituationGavAttachmentTitle(situationGavId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateSituationGavAttachmentTitle"))
        }
    }

    override suspend fun deleteSituationGavAttachment(situationGavId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteSituationGavAttachment(situationGavId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteSituationGavAttachment"))
        }
    }
}
