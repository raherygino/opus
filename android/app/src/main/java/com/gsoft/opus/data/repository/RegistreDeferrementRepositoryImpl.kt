package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.RegistreDeferrementRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.RegistreDeferrement
import com.gsoft.opus.domain.model.RegistreDeferrementAttachment
import com.gsoft.opus.domain.repository.RegistreDeferrementFormData
import com.gsoft.opus.domain.repository.RegistreDeferrementRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistreDeferrementRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RegistreDeferrementRepository {

    companion object {
        private const val TAG = "RegistreDeferrementRepo"
    }

    private fun RegistreDeferrementFormData.toRequest() = RegistreDeferrementRequest(
        numero = numero,
        dateHeureDeferrement = dateHeureDeferrement,
        personneNom = personneNom,
        dateLieuNaissance = dateLieuNaissance,
        infraction = infraction,
        numeroDossier = numeroDossier,
        autorite = autorite,
        destination = destination,
        escorte = escorte,
        suiteDonnee = suiteDonnee,
        observations = observations
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

    override suspend fun getDeferrementList(search: String?): Resource<List<RegistreDeferrement>> {
        return try {
            apiService.getRegistreDeferrementList(search)
                .extract("Impossible de charger le registre de déferrement")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getDeferrementList"))
        }
    }

    override suspend fun getDeferrement(id: Int): Resource<RegistreDeferrement> {
        return try {
            apiService.getRegistreDeferrement(id).extract("Entrée introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getDeferrement"))
        }
    }

    override suspend fun peekNextNumber(): Resource<String> {
        return try {
            apiService.getRegistreDeferrementNextNumber()
                .extract("Impossible de charger le numéro")
                .map { it.numero }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "peekNextNumber"))
        }
    }

    override suspend fun createDeferrement(data: RegistreDeferrementFormData): Resource<RegistreDeferrement> {
        return try {
            apiService.createRegistreDeferrement(data.toRequest())
                .extract("Impossible d'enregistrer l'entrée")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createDeferrement"))
        }
    }

    override suspend fun updateDeferrement(id: Int, data: RegistreDeferrementFormData): Resource<RegistreDeferrement> {
        return try {
            apiService.updateRegistreDeferrement(id, data.toRequest())
                .extract("Impossible de modifier l'entrée")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateDeferrement"))
        }
    }

    override suspend fun deleteDeferrement(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRegistreDeferrement(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteDeferrement"))
        }
    }

    override suspend fun getDeferrementAttachments(deferrementId: Int): Resource<List<RegistreDeferrementAttachment>> {
        return try {
            apiService.getRegistreDeferrementAttachments(deferrementId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getDeferrementAttachments"))
        }
    }

    override suspend fun addDeferrementAttachment(deferrementId: Int, title: String, file: UploadFile): Resource<RegistreDeferrementAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createRegistreDeferrementAttachment(deferrementId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addDeferrementAttachment"))
        }
    }

    override suspend fun updateDeferrementAttachmentTitle(deferrementId: Int, attachId: Int, title: String): Resource<RegistreDeferrementAttachment> {
        return try {
            apiService.updateRegistreDeferrementAttachmentTitle(deferrementId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateDeferrementAttachmentTitle"))
        }
    }

    override suspend fun deleteDeferrementAttachment(deferrementId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRegistreDeferrementAttachment(deferrementId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteDeferrementAttachment"))
        }
    }
}
