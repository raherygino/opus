package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.CorrespondanceRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Correspondance
import com.gsoft.opus.domain.model.CorrespondanceAttachment
import com.gsoft.opus.domain.repository.CorrespondanceFormData
import com.gsoft.opus.domain.repository.CorrespondanceRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorrespondanceRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : CorrespondanceRepository {

    companion object {
        private const val TAG = "CorrespondanceRepo"
    }

    private fun CorrespondanceFormData.toRequest() = CorrespondanceRequest(
        dateCorrespondance = dateCorrespondance,
        heureEnregistrement = heureEnregistrement,
        sens = sens,
        reference = reference.trim(),
        emetteurDestinataire = emetteurDestinataire.trim(),
        objet = objet.trim(),
        statut = statut?.takeIf { it.isNotBlank() }
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

    override suspend fun getCorrespondanceList(
        sens: String?,
        statut: String?,
        search: String?,
        dateFrom: String?,
        dateTo: String?
    ): Resource<List<Correspondance>> {
        return try {
            val result = apiService.getCorrespondanceList(
                sens?.takeIf { it.isNotBlank() },
                statut?.takeIf { it.isNotBlank() },
                search?.takeIf { it.isNotBlank() },
                dateFrom?.takeIf { it.isNotBlank() },
                dateTo?.takeIf { it.isNotBlank() }
            ).extract("Impossible de charger la liste des correspondances")
            result.map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getCorrespondanceList"))
        }
    }

    override suspend fun getCorrespondance(id: Int): Resource<Correspondance> {
        return try {
            apiService.getCorrespondance(id).extract("Correspondance introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getCorrespondance"))
        }
    }

    override suspend fun createCorrespondance(data: CorrespondanceFormData): Resource<Correspondance> {
        return try {
            apiService.createCorrespondance(data.toRequest())
                .extract("Impossible d'enregistrer la correspondance")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "createCorrespondance"))
        }
    }

    override suspend fun updateCorrespondance(id: Int, data: CorrespondanceFormData): Resource<Correspondance> {
        return try {
            apiService.updateCorrespondance(id, data.toRequest())
                .extract("Impossible de mettre à jour la correspondance")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateCorrespondance"))
        }
    }

    override suspend fun deleteCorrespondance(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteCorrespondance(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette correspondance", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteCorrespondance"))
        }
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(correspondanceId: Int): Resource<List<CorrespondanceAttachment>> {
        return try {
            apiService.getCorrespondanceAttachments(correspondanceId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getAttachments"))
        }
    }

    override suspend fun addAttachment(correspondanceId: Int, title: String, file: UploadFile): Resource<CorrespondanceAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createCorrespondanceAttachment(correspondanceId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "addAttachment"))
        }
    }

    override suspend fun updateAttachmentTitle(correspondanceId: Int, attachId: Int, title: String): Resource<CorrespondanceAttachment> {
        return try {
            apiService.updateCorrespondanceAttachmentTitle(correspondanceId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateAttachmentTitle"))
        }
    }

    override suspend fun deleteAttachment(correspondanceId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteCorrespondanceAttachment(correspondanceId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteAttachment"))
        }
    }
}
