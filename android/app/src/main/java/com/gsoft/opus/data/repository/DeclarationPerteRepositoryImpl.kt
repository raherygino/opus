package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.DeclarationPerteRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.DeclarationPerte
import com.gsoft.opus.domain.model.DeclarationPerteAttachment
import com.gsoft.opus.domain.repository.DeclarationPerteFormData
import com.gsoft.opus.domain.repository.DeclarationPerteRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeclarationPerteRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : DeclarationPerteRepository {

    companion object {
        private const val TAG = "DeclarationPerteRepo"
    }

    private fun DeclarationPerteFormData.toRequest() = DeclarationPerteRequest(
        dateDeclaration = dateDeclaration,
        heureDeclaration = heureDeclaration,
        identiteDeclarant = identiteDeclarant.trim(),
        natureObjet = natureObjet.trim(),
        descriptionObjet = descriptionObjet.trim(),
        datePerte = datePerte,
        lieuPerte = lieuPerte.trim(),
        numeroAttestation = numeroAttestation.trim(),
        nomAgent = nomAgent.trim()
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

    override suspend fun getDeclarationPerteList(
        search: String?,
        dateFrom: String?,
        dateTo: String?
    ): Resource<List<DeclarationPerte>> {
        return try {
            val result = apiService.getDeclarationPerteList(
                search?.takeIf { it.isNotBlank() },
                dateFrom?.takeIf { it.isNotBlank() },
                dateTo?.takeIf { it.isNotBlank() }
            ).extract("Impossible de charger la liste des déclarations de perte")
            result.map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getDeclarationPerteList"))
        }
    }

    override suspend fun getDeclarationPerte(id: Int): Resource<DeclarationPerte> {
        return try {
            apiService.getDeclarationPerte(id).extract("Déclaration de perte introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getDeclarationPerte"))
        }
    }

    override suspend fun createDeclarationPerte(data: DeclarationPerteFormData): Resource<DeclarationPerte> {
        return try {
            apiService.createDeclarationPerte(data.toRequest())
                .extract("Impossible d'enregistrer la déclaration de perte")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "createDeclarationPerte"))
        }
    }

    override suspend fun updateDeclarationPerte(id: Int, data: DeclarationPerteFormData): Resource<DeclarationPerte> {
        return try {
            apiService.updateDeclarationPerte(id, data.toRequest())
                .extract("Impossible de mettre à jour la déclaration de perte")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateDeclarationPerte"))
        }
    }

    override suspend fun deleteDeclarationPerte(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteDeclarationPerte(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette déclaration de perte", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteDeclarationPerte"))
        }
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(declarationId: Int): Resource<List<DeclarationPerteAttachment>> {
        return try {
            apiService.getDeclarationPerteAttachments(declarationId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getAttachments"))
        }
    }

    override suspend fun addAttachment(declarationId: Int, title: String, file: UploadFile): Resource<DeclarationPerteAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createDeclarationPerteAttachment(declarationId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "addAttachment"))
        }
    }

    override suspend fun updateAttachmentTitle(declarationId: Int, attachId: Int, title: String): Resource<DeclarationPerteAttachment> {
        return try {
            apiService.updateDeclarationPerteAttachmentTitle(declarationId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateAttachmentTitle"))
        }
    }

    override suspend fun deleteAttachment(declarationId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteDeclarationPerteAttachment(declarationId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteAttachment"))
        }
    }
}
