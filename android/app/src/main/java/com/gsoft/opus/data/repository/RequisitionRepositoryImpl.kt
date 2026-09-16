package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.RequisitionRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Requisition
import com.gsoft.opus.domain.model.RequisitionAttachment
import com.gsoft.opus.domain.repository.RequisitionFormData
import com.gsoft.opus.domain.repository.RequisitionRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequisitionRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RequisitionRepository {

    companion object {
        private const val TAG = "RequisitionRepo"
    }

    private fun RequisitionFormData.toRequest() = RequisitionRequest(
        type = type,
        dateRequisition = dateRequisition,
        numero = numero?.takeIf { it.isNotBlank() },
        numeroTtr = numeroTtr,
        nomSubstitut = nomSubstitut,
        affaire = affaire,
        numeroDossier = numeroDossier,
        opj = opj
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

    override suspend fun getRequisitionList(
        type: String?,
        search: String?,
        dateFrom: String?,
        dateTo: String?
    ): Resource<List<Requisition>> {
        return try {
            apiService.getRequisitionList(type, search, dateFrom, dateTo)
                .extract("Impossible de charger les réquisitions")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRequisitionList"))
        }
    }

    override suspend fun getRequisition(id: Int): Resource<Requisition> {
        return try {
            apiService.getRequisition(id).extract("Réquisition introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRequisition"))
        }
    }

    override suspend fun createRequisition(data: RequisitionFormData): Resource<Requisition> {
        return try {
            apiService.createRequisition(data.toRequest())
                .extract("Impossible d'enregistrer la réquisition")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createRequisition"))
        }
    }

    override suspend fun updateRequisition(id: Int, data: RequisitionFormData): Resource<Requisition> {
        return try {
            apiService.updateRequisition(id, data.toRequest())
                .extract("Impossible de modifier la réquisition")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateRequisition"))
        }
    }

    override suspend fun deleteRequisition(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRequisition(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette réquisition", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteRequisition"))
        }
    }

    override suspend fun peekRequisitionNumber(): Resource<String> {
        return try {
            when (val result = apiService.getRequisitionNextNumber().extract("Impossible de récupérer le numéro suggéré")) {
                is Resource.Success -> Resource.success(result.data.numero ?: "")
                is Resource.Error -> Resource.error(result.message, result.code)
                is Resource.Loading -> Resource.loading()
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "peekRequisitionNumber"))
        }
    }

    override suspend fun getRequisitionAttachments(requisitionId: Int): Resource<List<RequisitionAttachment>> {
        return try {
            apiService.getRequisitionAttachments(requisitionId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRequisitionAttachments"))
        }
    }

    override suspend fun addRequisitionAttachment(requisitionId: Int, title: String, file: UploadFile): Resource<RequisitionAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createRequisitionAttachment(requisitionId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addRequisitionAttachment"))
        }
    }

    override suspend fun updateRequisitionAttachmentTitle(requisitionId: Int, attachId: Int, title: String): Resource<RequisitionAttachment> {
        return try {
            apiService.updateRequisitionAttachmentTitle(requisitionId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateRequisitionAttachmentTitle"))
        }
    }

    override suspend fun deleteRequisitionAttachment(requisitionId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRequisitionAttachment(requisitionId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteRequisitionAttachment"))
        }
    }
}
