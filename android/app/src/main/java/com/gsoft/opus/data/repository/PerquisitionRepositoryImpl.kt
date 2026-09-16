package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.PerquisitionRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Perquisition
import com.gsoft.opus.domain.model.PerquisitionAttachment
import com.gsoft.opus.domain.repository.PerquisitionFormData
import com.gsoft.opus.domain.repository.PerquisitionRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerquisitionRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : PerquisitionRepository {

    companion object {
        private const val TAG = "PerquisitionRepo"
    }

    private fun PerquisitionFormData.toRequest() = PerquisitionRequest(
        numero = numero,
        numeroTtr = numeroTtr,
        substitut = substitut,
        affaire = affaire,
        motif = motif
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

    override suspend fun getPerquisitionList(search: String?): Resource<List<Perquisition>> {
        return try {
            apiService.getPerquisitionList(search)
                .extract("Impossible de charger les perquisitions")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPerquisitionList"))
        }
    }

    override suspend fun getPerquisition(id: Int): Resource<Perquisition> {
        return try {
            apiService.getPerquisition(id).extract("Perquisition introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPerquisition"))
        }
    }

    override suspend fun peekNextNumber(): Resource<String> {
        return try {
            apiService.getPerquisitionNextNumber()
                .extract("Impossible de charger le numéro")
                .map { it.numero }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "peekNextNumber"))
        }
    }

    override suspend fun createPerquisition(data: PerquisitionFormData): Resource<Perquisition> {
        return try {
            apiService.createPerquisition(data.toRequest())
                .extract("Impossible d'enregistrer la perquisition")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createPerquisition"))
        }
    }

    override suspend fun updatePerquisition(id: Int, data: PerquisitionFormData): Resource<Perquisition> {
        return try {
            apiService.updatePerquisition(id, data.toRequest())
                .extract("Impossible de modifier la perquisition")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updatePerquisition"))
        }
    }

    override suspend fun deletePerquisition(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePerquisition(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deletePerquisition"))
        }
    }

    override suspend fun getPerquisitionAttachments(perquisitionId: Int): Resource<List<PerquisitionAttachment>> {
        return try {
            apiService.getPerquisitionAttachments(perquisitionId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPerquisitionAttachments"))
        }
    }

    override suspend fun addPerquisitionAttachment(perquisitionId: Int, title: String, file: UploadFile): Resource<PerquisitionAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createPerquisitionAttachment(perquisitionId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addPerquisitionAttachment"))
        }
    }

    override suspend fun updatePerquisitionAttachmentTitle(perquisitionId: Int, attachId: Int, title: String): Resource<PerquisitionAttachment> {
        return try {
            apiService.updatePerquisitionAttachmentTitle(perquisitionId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updatePerquisitionAttachmentTitle"))
        }
    }

    override suspend fun deletePerquisitionAttachment(perquisitionId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePerquisitionAttachment(perquisitionId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deletePerquisitionAttachment"))
        }
    }
}
