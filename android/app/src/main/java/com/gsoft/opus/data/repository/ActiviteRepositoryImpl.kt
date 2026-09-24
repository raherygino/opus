package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.ActiviteRequest
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Activite
import com.gsoft.opus.domain.model.ActiviteAttachment
import com.gsoft.opus.domain.repository.ActiviteFormData
import com.gsoft.opus.domain.repository.ActiviteRepository
import com.gsoft.opus.domain.repository.UploadFile
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class ActiviteRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ActiviteRepository {

    companion object {
        private const val TAG = "ActiviteRepo"
    }

    private fun ActiviteFormData.toRequest() = ActiviteRequest(
        dateActivite = dateActivite,
        heureActivite = heureActivite,
        // Itineraries: trimmed as-is — null means the mode was not selected,
        // an empty string means it was selected without a detailed itinerary.
        patrouilleDiurneMotoriseeItineraire = patrouilleDiurneMotoriseeItineraire?.trim(),
        patrouilleDiurnePedestreItineraire = patrouilleDiurnePedestreItineraire?.trim(),
        patrouilleDiurnePorteeItineraire = patrouilleDiurnePorteeItineraire?.trim(),
        patrouilleNocturneMotoriseeItineraire = patrouilleNocturneMotoriseeItineraire?.trim(),
        patrouilleNocturnePedestreItineraire = patrouilleNocturnePedestreItineraire?.trim(),
        patrouilleNocturnePorteeItineraire = patrouilleNocturnePorteeItineraire?.trim(),
        operationCiblee = operationCiblee?.trim()?.ifBlank { null },
        faitsConstates = faitsConstates?.trim()?.ifBlank { null },
        compteRenduHierarchie = compteRenduHierarchie?.trim()?.ifBlank { null },
        conduiteATenir = conduiteATenir?.trim()?.ifBlank { null },
        natureIntervention = natureIntervention?.trim()?.ifBlank { null },
        suitesDonnees = suitesDonnees?.trim()?.ifBlank { null },
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

    override suspend fun getActiviteList(search: String?): Resource<List<Activite>> {
        return try {
            apiService.getActiviteList(search?.takeIf { it.isNotBlank() })
                .extract("Impossible de charger les activités")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getActiviteList"))
        }
    }

    override suspend fun getActivite(id: Int): Resource<Activite> {
        return try {
            apiService.getActivite(id)
                .extract("Activité introuvable")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getActivite"))
        }
    }

    override suspend fun createActivite(data: ActiviteFormData): Resource<Activite> {
        return try {
            apiService.createActivite(data.toRequest())
                .extract("Impossible d'enregistrer l'activité")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createActivite"))
        }
    }

    override suspend fun updateActivite(id: Int, data: ActiviteFormData): Resource<Activite> {
        return try {
            apiService.updateActivite(id, data.toRequest())
                .extract("Impossible de mettre à jour l'activité")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateActivite"))
        }
    }

    override suspend fun deleteActivite(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteActivite(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer l'activité", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteActivite"))
        }
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(activiteId: Int): Resource<List<ActiviteAttachment>> {
        return try {
            apiService.getActiviteAttachments(activiteId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getAttachments"))
        }
    }

    override suspend fun addAttachment(activiteId: Int, title: String, file: UploadFile): Resource<ActiviteAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createActiviteAttachment(activiteId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addAttachment"))
        }
    }

    override suspend fun updateAttachmentTitle(activiteId: Int, attachId: Int, title: String): Resource<ActiviteAttachment> {
        return try {
            apiService.updateActiviteAttachmentTitle(activiteId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateAttachmentTitle"))
        }
    }

    override suspend fun deleteAttachment(activiteId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteActiviteAttachment(activiteId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteAttachment"))
        }
    }
}
