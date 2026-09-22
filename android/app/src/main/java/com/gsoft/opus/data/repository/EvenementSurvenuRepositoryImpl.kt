package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.EvenementSurvenuRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.EvenementSurvenu
import com.gsoft.opus.domain.model.EvenementSurvenuAttachment
import com.gsoft.opus.domain.repository.EvenementSurvenuFormData
import com.gsoft.opus.domain.repository.EvenementSurvenuRepository
import com.gsoft.opus.domain.repository.UploadFile
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class EvenementSurvenuRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : EvenementSurvenuRepository {

    companion object {
        private const val TAG = "EvenementRepo"
    }

    private fun EvenementSurvenuFormData.toRequest() = EvenementSurvenuRequest(
        dateEvenement = dateEvenement,
        heureEvenement = heureEvenement,
        typeEvenement = typeEvenement,
        lieuExact = lieuExact.trim(),
        auteursPresumes = auteursPresumes?.trim()?.ifBlank { null },
        victimes = victimes?.trim()?.ifBlank { null },
        temoins = temoins?.trim()?.ifBlank { null },
        mesuresPrises = mesuresPrises?.trim()?.ifBlank { null },
        latitude = latitude,
        longitude = longitude
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

    private fun failureMessage(e: Exception, what: String): String {
        return if (e is IOException) {
            "Erreur réseau. Vérifiez votre connexion."
        } else {
            Log.e(TAG, "$what failed", e)
            "Une erreur inattendue s'est produite."
        }
    }

    override suspend fun getEvenementList(search: String?): Resource<List<EvenementSurvenu>> {
        return try {
            apiService.getEvenementSurvenuList(search?.takeIf { it.isNotBlank() })
                .extract("Impossible de charger les évènements")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getEvenementList"))
        }
    }

    override suspend fun getEvenement(id: Int): Resource<EvenementSurvenu> {
        return try {
            apiService.getEvenementSurvenu(id)
                .extract("Évènement introuvable")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getEvenement"))
        }
    }

    override suspend fun createEvenement(data: EvenementSurvenuFormData): Resource<EvenementSurvenu> {
        return try {
            apiService.createEvenementSurvenu(data.toRequest())
                .extract("Impossible d'enregistrer l'évènement")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createEvenement"))
        }
    }

    override suspend fun updateEvenement(id: Int, data: EvenementSurvenuFormData): Resource<EvenementSurvenu> {
        return try {
            apiService.updateEvenementSurvenu(id, data.toRequest())
                .extract("Impossible de mettre à jour l'évènement")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateEvenement"))
        }
    }

    override suspend fun deleteEvenement(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteEvenementSurvenu(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer l'évènement", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteEvenement"))
        }
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(evenementId: Int): Resource<List<EvenementSurvenuAttachment>> {
        return try {
            apiService.getEvenementSurvenuAttachments(evenementId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getAttachments"))
        }
    }

    override suspend fun addAttachment(evenementId: Int, title: String, file: UploadFile): Resource<EvenementSurvenuAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createEvenementSurvenuAttachment(evenementId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addAttachment"))
        }
    }

    override suspend fun updateAttachmentTitle(evenementId: Int, attachId: Int, title: String): Resource<EvenementSurvenuAttachment> {
        return try {
            apiService.updateEvenementSurvenuAttachmentTitle(evenementId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateAttachmentTitle"))
        }
    }

    override suspend fun deleteAttachment(evenementId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteEvenementSurvenuAttachment(evenementId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteAttachment"))
        }
    }
}
