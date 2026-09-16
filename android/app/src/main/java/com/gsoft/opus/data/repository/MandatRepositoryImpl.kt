package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.MandatRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Mandat
import com.gsoft.opus.domain.model.MandatAttachment
import com.gsoft.opus.domain.repository.MandatFormData
import com.gsoft.opus.domain.repository.MandatRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MandatRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MandatRepository {

    companion object {
        private const val TAG = "MandatRepo"
    }

    private fun MandatFormData.toRequest() = MandatRequest(
        numero = numero,
        type = type,
        autorite = autorite,
        personneNom = personneNom,
        dateLieuNaissance = dateLieuNaissance,
        motif = motif,
        qualificationInfraction = qualificationInfraction,
        opjExecution = opjExecution,
        dateHeureExecution = dateHeureExecution,
        lieuExecution = lieuExecution,
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

    override suspend fun getMandatList(search: String?, type: String?): Resource<List<Mandat>> {
        return try {
            apiService.getMandatList(search, type)
                .extract("Impossible de charger les mandats")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getMandatList"))
        }
    }

    override suspend fun getMandat(id: Int): Resource<Mandat> {
        return try {
            apiService.getMandat(id).extract("Mandat introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getMandat"))
        }
    }

    override suspend fun peekMandatNumber(): Resource<String> {
        return try {
            apiService.peekMandatNumber()
                .extract("Impossible de récupérer le numéro suggéré")
                .map { it.numero }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "peekMandatNumber"))
        }
    }

    override suspend fun createMandat(data: MandatFormData): Resource<Mandat> {
        return try {
            apiService.createMandat(data.toRequest())
                .extract("Impossible d'enregistrer le mandat")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createMandat"))
        }
    }

    override suspend fun updateMandat(id: Int, data: MandatFormData): Resource<Mandat> {
        return try {
            apiService.updateMandat(id, data.toRequest())
                .extract("Impossible de modifier le mandat")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateMandat"))
        }
    }

    override suspend fun deleteMandat(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteMandat(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteMandat"))
        }
    }

    override suspend fun getMandatAttachments(mandatId: Int): Resource<List<MandatAttachment>> {
        return try {
            apiService.getMandatAttachments(mandatId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getMandatAttachments"))
        }
    }

    override suspend fun addMandatAttachment(mandatId: Int, title: String, file: UploadFile): Resource<MandatAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createMandatAttachment(mandatId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addMandatAttachment"))
        }
    }

    override suspend fun updateMandatAttachmentTitle(mandatId: Int, attachId: Int, title: String): Resource<MandatAttachment> {
        return try {
            apiService.updateMandatAttachmentTitle(mandatId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateMandatAttachmentTitle"))
        }
    }

    override suspend fun deleteMandatAttachment(mandatId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteMandatAttachment(mandatId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteMandatAttachment"))
        }
    }
}
