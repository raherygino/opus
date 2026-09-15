package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.ObjetSaisiRequest
import com.gsoft.opus.data.api.dto.ObjetTrouveRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.ObjetSaisi
import com.gsoft.opus.domain.model.ObjetSaisiAttachment
import com.gsoft.opus.domain.model.ObjetTrouve
import com.gsoft.opus.domain.model.ObjetTrouveAttachment
import com.gsoft.opus.domain.repository.ObjetSaisiFormData
import com.gsoft.opus.domain.repository.ObjetSaisiRepository
import com.gsoft.opus.domain.repository.ObjetTrouveFormData
import com.gsoft.opus.domain.repository.ObjetTrouveRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjetSaisiRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ObjetSaisiRepository {

    companion object {
        private const val TAG = "ObjetSaisiRepo"
    }

    private fun ObjetSaisiFormData.toRequest() = ObjetSaisiRequest(
        numeroDossier = numeroDossier,
        motif = motif,
        typeObjet = typeObjet,
        proprietaire = proprietaire
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

    override suspend fun getObjetSaisiList(typeObjet: String?, search: String?): Resource<List<ObjetSaisi>> {
        return try {
            apiService.getObjetSaisiList(typeObjet, search)
                .extract("Impossible de charger les objets saisis")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getObjetSaisiList"))
        }
    }

    override suspend fun getObjetSaisi(id: Int): Resource<ObjetSaisi> {
        return try {
            apiService.getObjetSaisi(id).extract("Objet saisi introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getObjetSaisi"))
        }
    }

    override suspend fun createObjetSaisi(data: ObjetSaisiFormData): Resource<ObjetSaisi> {
        return try {
            apiService.createObjetSaisi(data.toRequest())
                .extract("Impossible d'enregistrer l'objet saisi")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createObjetSaisi"))
        }
    }

    override suspend fun updateObjetSaisi(id: Int, data: ObjetSaisiFormData): Resource<ObjetSaisi> {
        return try {
            apiService.updateObjetSaisi(id, data.toRequest())
                .extract("Impossible de modifier l'objet saisi")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateObjetSaisi"))
        }
    }

    override suspend fun deleteObjetSaisi(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteObjetSaisi(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteObjetSaisi"))
        }
    }

    override suspend fun getObjetSaisiAttachments(objetId: Int): Resource<List<ObjetSaisiAttachment>> {
        return try {
            apiService.getObjetSaisiAttachments(objetId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getObjetSaisiAttachments"))
        }
    }

    override suspend fun addObjetSaisiAttachment(objetId: Int, title: String, file: UploadFile): Resource<ObjetSaisiAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createObjetSaisiAttachment(objetId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addObjetSaisiAttachment"))
        }
    }

    override suspend fun updateObjetSaisiAttachmentTitle(objetId: Int, attachId: Int, title: String): Resource<ObjetSaisiAttachment> {
        return try {
            apiService.updateObjetSaisiAttachmentTitle(objetId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateObjetSaisiAttachmentTitle"))
        }
    }

    override suspend fun deleteObjetSaisiAttachment(objetId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteObjetSaisiAttachment(objetId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteObjetSaisiAttachment"))
        }
    }
}

@Singleton
class ObjetTrouveRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ObjetTrouveRepository {

    companion object {
        private const val TAG = "ObjetTrouveRepo"
    }

    private fun ObjetTrouveFormData.toRequest() = ObjetTrouveRequest(
        affaire = affaire,
        motifDecouverte = motifDecouverte,
        restitution = restitution
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

    override suspend fun getObjetTrouveList(motifDecouverte: String?, restitution: Boolean?, search: String?): Resource<List<ObjetTrouve>> {
        return try {
            apiService.getObjetTrouveList(motifDecouverte, restitution, search)
                .extract("Impossible de charger les objets trouvés")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getObjetTrouveList"))
        }
    }

    override suspend fun getObjetTrouve(id: Int): Resource<ObjetTrouve> {
        return try {
            apiService.getObjetTrouve(id).extract("Objet trouvé introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getObjetTrouve"))
        }
    }

    override suspend fun createObjetTrouve(data: ObjetTrouveFormData): Resource<ObjetTrouve> {
        return try {
            apiService.createObjetTrouve(data.toRequest())
                .extract("Impossible d'enregistrer l'objet trouvé")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createObjetTrouve"))
        }
    }

    override suspend fun updateObjetTrouve(id: Int, data: ObjetTrouveFormData): Resource<ObjetTrouve> {
        return try {
            apiService.updateObjetTrouve(id, data.toRequest())
                .extract("Impossible de modifier l'objet trouvé")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateObjetTrouve"))
        }
    }

    override suspend fun deleteObjetTrouve(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteObjetTrouve(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteObjetTrouve"))
        }
    }

    override suspend fun getObjetTrouveAttachments(objetId: Int): Resource<List<ObjetTrouveAttachment>> {
        return try {
            apiService.getObjetTrouveAttachments(objetId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getObjetTrouveAttachments"))
        }
    }

    override suspend fun addObjetTrouveAttachment(objetId: Int, title: String, file: UploadFile): Resource<ObjetTrouveAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createObjetTrouveAttachment(objetId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addObjetTrouveAttachment"))
        }
    }

    override suspend fun updateObjetTrouveAttachmentTitle(objetId: Int, attachId: Int, title: String): Resource<ObjetTrouveAttachment> {
        return try {
            apiService.updateObjetTrouveAttachmentTitle(objetId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateObjetTrouveAttachmentTitle"))
        }
    }

    override suspend fun deleteObjetTrouveAttachment(objetId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteObjetTrouveAttachment(objetId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteObjetTrouveAttachment"))
        }
    }
}
