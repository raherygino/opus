package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.GardeAVueRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.GardeAVue
import com.gsoft.opus.domain.model.GardeAVueAttachment
import com.gsoft.opus.domain.repository.GardeAVueFormData
import com.gsoft.opus.domain.repository.GardeAVueRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GardeAVueRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : GardeAVueRepository {

    companion object {
        private const val TAG = "GardeAVueRepo"
    }

    private fun GardeAVueFormData.toRequest() = GardeAVueRequest(
        nom = nom,
        prenoms = prenoms,
        dateNaissance = dateNaissance,
        adresse = adresse,
        enqueteurPermance = enqueteurPermance,
        opjGav = opjGav,
        motif = motif,
        etatSante = etatSante,
        droitsNotifies = droitsNotifies,
        personneContacter = personneContacter,
        debutGav = debutGav,
        finGav = finGav,
        prolongationGav = prolongationGav
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

    override suspend fun getGardeAVueList(
        search: String?,
        dateFrom: String?,
        dateTo: String?
    ): Resource<List<GardeAVue>> {
        return try {
            apiService.getGardeAVueList(search, dateFrom, dateTo)
                .extract("Impossible de charger les gardes à vue")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getGardeAVueList"))
        }
    }

    override suspend fun getGardeAVue(id: Int): Resource<GardeAVue> {
        return try {
            apiService.getGardeAVue(id).extract("Garde à vue introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getGardeAVue"))
        }
    }

    override suspend fun createGardeAVue(data: GardeAVueFormData): Resource<GardeAVue> {
        return try {
            apiService.createGardeAVue(data.toRequest())
                .extract("Impossible d'enregistrer la garde à vue")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createGardeAVue"))
        }
    }

    override suspend fun updateGardeAVue(id: Int, data: GardeAVueFormData): Resource<GardeAVue> {
        return try {
            apiService.updateGardeAVue(id, data.toRequest())
                .extract("Impossible de modifier la garde à vue")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateGardeAVue"))
        }
    }

    override suspend fun deleteGardeAVue(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteGardeAVue(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette garde à vue", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteGardeAVue"))
        }
    }

    override suspend fun getGardeAVueAttachments(gardeAVueId: Int): Resource<List<GardeAVueAttachment>> {
        return try {
            apiService.getGardeAVueAttachments(gardeAVueId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getGardeAVueAttachments"))
        }
    }

    override suspend fun addGardeAVueAttachment(gardeAVueId: Int, title: String, file: UploadFile): Resource<GardeAVueAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createGardeAVueAttachment(gardeAVueId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addGardeAVueAttachment"))
        }
    }

    override suspend fun updateGardeAVueAttachmentTitle(gardeAVueId: Int, attachId: Int, title: String): Resource<GardeAVueAttachment> {
        return try {
            apiService.updateGardeAVueAttachmentTitle(gardeAVueId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateGardeAVueAttachmentTitle"))
        }
    }

    override suspend fun deleteGardeAVueAttachment(gardeAVueId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteGardeAVueAttachment(gardeAVueId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteGardeAVueAttachment"))
        }
    }
}
