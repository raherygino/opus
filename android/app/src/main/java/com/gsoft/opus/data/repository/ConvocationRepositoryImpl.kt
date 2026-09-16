package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.ConvocationRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Convocation
import com.gsoft.opus.domain.model.ConvocationAttachment
import com.gsoft.opus.domain.repository.ConvocationFormData
import com.gsoft.opus.domain.repository.ConvocationRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConvocationRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ConvocationRepository {

    companion object {
        private const val TAG = "ConvocationRepo"
    }

    private fun ConvocationFormData.toRequest() = ConvocationRequest(
        type = type,
        dateConvocation = dateConvocation,
        numero = numero?.takeIf { it.isNotBlank() },
        nom = nom,
        adresse = adresse,
        infraction = infraction,
        personneAccuseRecu = personneAccuseRecu,
        numeroDossier = numeroDossier,
        observation = observation
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

    override suspend fun getConvocationList(
        type: String?,
        search: String?,
        dateFrom: String?,
        dateTo: String?
    ): Resource<List<Convocation>> {
        return try {
            apiService.getConvocationList(type, search, dateFrom, dateTo)
                .extract("Impossible de charger les convocations")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getConvocationList"))
        }
    }

    override suspend fun getConvocation(id: Int): Resource<Convocation> {
        return try {
            apiService.getConvocation(id).extract("Convocation introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getConvocation"))
        }
    }

    override suspend fun createConvocation(data: ConvocationFormData): Resource<Convocation> {
        return try {
            apiService.createConvocation(data.toRequest())
                .extract("Impossible d'enregistrer la convocation")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createConvocation"))
        }
    }

    override suspend fun updateConvocation(id: Int, data: ConvocationFormData): Resource<Convocation> {
        return try {
            apiService.updateConvocation(id, data.toRequest())
                .extract("Impossible de modifier la convocation")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateConvocation"))
        }
    }

    override suspend fun deleteConvocation(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteConvocation(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette convocation", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteConvocation"))
        }
    }

    override suspend fun peekConvocationNumber(type: String): Resource<String> {
        return try {
            when (val result = apiService.getConvocationNextNumber(type).extract("Impossible de récupérer le numéro suggéré")) {
                is Resource.Success -> Resource.success(result.data.numero ?: "")
                is Resource.Error -> Resource.error(result.message, result.code)
                is Resource.Loading -> Resource.loading()
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "peekConvocationNumber"))
        }
    }

    override suspend fun getConvocationAttachments(convocationId: Int): Resource<List<ConvocationAttachment>> {
        return try {
            apiService.getConvocationAttachments(convocationId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getConvocationAttachments"))
        }
    }

    override suspend fun addConvocationAttachment(convocationId: Int, title: String, file: UploadFile): Resource<ConvocationAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createConvocationAttachment(convocationId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addConvocationAttachment"))
        }
    }

    override suspend fun updateConvocationAttachmentTitle(convocationId: Int, attachId: Int, title: String): Resource<ConvocationAttachment> {
        return try {
            apiService.updateConvocationAttachmentTitle(convocationId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateConvocationAttachmentTitle"))
        }
    }

    override suspend fun deleteConvocationAttachment(convocationId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteConvocationAttachment(convocationId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteConvocationAttachment"))
        }
    }
}
