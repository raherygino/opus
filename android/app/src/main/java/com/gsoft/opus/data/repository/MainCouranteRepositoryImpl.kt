package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.MainCouranteRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.MainCourante
import com.gsoft.opus.domain.model.MainCouranteAttachment
import com.gsoft.opus.domain.repository.MainCouranteFormData
import com.gsoft.opus.domain.repository.MainCouranteRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainCouranteRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MainCouranteRepository {

    companion object {
        private const val TAG = "MainCouranteRepo"
    }

    private fun MainCouranteFormData.toRequest() = MainCouranteRequest(
        dateEvenement = dateEvenement,
        heureEvenement = heureEvenement,
        categorie = categorie,
        description = description.trim(),
        origine = origine
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

    override suspend fun getMainCouranteList(
        origine: String?,
        categorie: String?,
        search: String?,
        dateFrom: String?,
        dateTo: String?
    ): Resource<List<MainCourante>> {
        return try {
            val result = apiService.getMainCouranteList(
                origine?.takeIf { it.isNotBlank() },
                categorie?.takeIf { it.isNotBlank() },
                search?.takeIf { it.isNotBlank() },
                dateFrom?.takeIf { it.isNotBlank() },
                dateTo?.takeIf { it.isNotBlank() }
            ).extract("Impossible de charger la main courante")
            result.map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getMainCouranteList"))
        }
    }

    override suspend fun getMainCourante(id: Int): Resource<MainCourante> {
        return try {
            apiService.getMainCourante(id).extract("Main courante introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getMainCourante"))
        }
    }

    override suspend fun createMainCourante(data: MainCouranteFormData): Resource<MainCourante> {
        return try {
            apiService.createMainCourante(data.toRequest())
                .extract("Impossible d'enregistrer la main courante")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "createMainCourante"))
        }
    }

    override suspend fun updateMainCourante(id: Int, data: MainCouranteFormData): Resource<MainCourante> {
        return try {
            apiService.updateMainCourante(id, data.toRequest())
                .extract("Impossible de mettre à jour la main courante")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateMainCourante"))
        }
    }

    override suspend fun deleteMainCourante(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteMainCourante(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette main courante", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteMainCourante"))
        }
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(mainCouranteId: Int): Resource<List<MainCouranteAttachment>> {
        return try {
            apiService.getMainCouranteAttachments(mainCouranteId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getAttachments"))
        }
    }

    override suspend fun addAttachment(mainCouranteId: Int, title: String, file: UploadFile): Resource<MainCouranteAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createMainCouranteAttachment(mainCouranteId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "addAttachment"))
        }
    }

    override suspend fun updateAttachmentTitle(mainCouranteId: Int, attachId: Int, title: String): Resource<MainCouranteAttachment> {
        return try {
            apiService.updateMainCouranteAttachmentTitle(mainCouranteId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateAttachmentTitle"))
        }
    }

    override suspend fun deleteAttachment(mainCouranteId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteMainCouranteAttachment(mainCouranteId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteAttachment"))
        }
    }
}
