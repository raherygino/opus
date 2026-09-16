package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.PersonneRechercheePhotoCaptionRequest
import com.gsoft.opus.data.api.dto.PersonneRechercheeRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.PersonneRecherchee
import com.gsoft.opus.domain.model.PersonneRechercheePhoto
import com.gsoft.opus.domain.repository.PersonneRechercheeFormData
import com.gsoft.opus.domain.repository.PersonneRechercheeRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonneRechercheeRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : PersonneRechercheeRepository {

    companion object {
        private const val TAG = "PersonneRechercheeRepo"
    }

    private fun PersonneRechercheeFormData.toRequest() = PersonneRechercheeRequest(
        nom = nom,
        adresse = adresse,
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

    override suspend fun getPersonneRechercheeList(search: String?): Resource<List<PersonneRecherchee>> {
        return try {
            apiService.getPersonneRechercheeList(search)
                .extract("Impossible de charger les personnes recherchées")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPersonneRechercheeList"))
        }
    }

    override suspend fun getPersonneRecherchee(id: Int): Resource<PersonneRecherchee> {
        return try {
            apiService.getPersonneRecherchee(id)
                .extract("Personne recherchée introuvable")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPersonneRecherchee"))
        }
    }

    override suspend fun createPersonneRecherchee(data: PersonneRechercheeFormData): Resource<PersonneRecherchee> {
        return try {
            apiService.createPersonneRecherchee(data.toRequest())
                .extract("Impossible d'enregistrer la personne recherchée")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createPersonneRecherchee"))
        }
    }

    override suspend fun updatePersonneRecherchee(id: Int, data: PersonneRechercheeFormData): Resource<PersonneRecherchee> {
        return try {
            apiService.updatePersonneRecherchee(id, data.toRequest())
                .extract("Impossible de modifier la personne recherchée")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updatePersonneRecherchee"))
        }
    }

    override suspend fun deletePersonneRecherchee(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePersonneRecherchee(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deletePersonneRecherchee"))
        }
    }

    override suspend fun getPhotos(personneId: Int): Resource<List<PersonneRechercheePhoto>> {
        return try {
            apiService.getPersonneRechercheePhotos(personneId)
                .extract("Impossible de charger les photos")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPhotos"))
        }
    }

    override suspend fun addPhoto(
        personneId: Int,
        caption: String?,
        captureSource: String?,
        file: UploadFile
    ): Resource<PersonneRechercheePhoto> {
        return try {
            val captionBody = (caption ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
            val sourceBody = (captureSource ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createPersonneRechercheePhoto(personneId, captionBody, sourceBody, part)
                .extract("Impossible d'ajouter la photo")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addPhoto"))
        }
    }

    override suspend fun updatePhotoCaption(personneId: Int, photoId: Int, caption: String?): Resource<PersonneRechercheePhoto> {
        return try {
            apiService.updatePersonneRechercheePhotoCaption(
                personneId,
                photoId,
                PersonneRechercheePhotoCaptionRequest(caption)
            ).extract("Impossible de modifier la photo")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updatePhotoCaption"))
        }
    }

    override suspend fun deletePhoto(personneId: Int, photoId: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePersonneRechercheePhoto(personneId, photoId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la photo", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deletePhoto"))
        }
    }
}
