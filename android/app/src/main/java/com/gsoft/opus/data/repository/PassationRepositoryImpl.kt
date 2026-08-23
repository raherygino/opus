package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.PassationRequest
import com.gsoft.opus.data.api.dto.VerifyIdentityRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Passation
import com.gsoft.opus.domain.model.PassationAttachment
import com.gsoft.opus.domain.model.VerifiedIdentity
import com.gsoft.opus.domain.repository.PassationFormData
import com.gsoft.opus.domain.repository.PassationRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassationRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : PassationRepository {

    companion object {
        private const val TAG = "PassationRepo"
    }

    private fun PassationFormData.toRequest() = PassationRequest(
        datePassation = datePassation,
        heurePassation = heurePassation,
        chefMontantUserId = chefMontantUserId,
        chefMontantGrade = chefMontantGrade.trim(),
        chefMontantLastname = chefMontantLastname.trim(),
        instructionsAutorite = instructionsAutorite.trim(),
        incidentsSurvenus = incidentsSurvenus.trim()
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

    override suspend fun getPassationList(
        search: String?,
        dateFrom: String?,
        dateTo: String?
    ): Resource<List<Passation>> {
        return try {
            val result = apiService.getPassationList(
                search?.takeIf { it.isNotBlank() },
                dateFrom?.takeIf { it.isNotBlank() },
                dateTo?.takeIf { it.isNotBlank() }
            ).extract("Impossible de charger la liste des passations")
            result.map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getPassationList"))
        }
    }

    override suspend fun getPassation(id: Int): Resource<Passation> {
        return try {
            apiService.getPassation(id).extract("Passation introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getPassation"))
        }
    }

    override suspend fun createPassation(data: PassationFormData): Resource<Passation> {
        return try {
            apiService.createPassation(data.toRequest())
                .extract("Impossible d'enregistrer la passation")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "createPassation"))
        }
    }

    override suspend fun updatePassation(id: Int, data: PassationFormData): Resource<Passation> {
        return try {
            apiService.updatePassation(id, data.toRequest())
                .extract("Impossible de mettre à jour la passation")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updatePassation"))
        }
    }

    override suspend fun deletePassation(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePassation(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette passation", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deletePassation"))
        }
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(passationId: Int): Resource<List<PassationAttachment>> {
        return try {
            apiService.getPassationAttachments(passationId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getAttachments"))
        }
    }

    override suspend fun addAttachment(passationId: Int, title: String, file: UploadFile): Resource<PassationAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createPassationAttachment(passationId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "addAttachment"))
        }
    }

    override suspend fun updateAttachmentTitle(passationId: Int, attachId: Int, title: String): Resource<PassationAttachment> {
        return try {
            apiService.updatePassationAttachmentTitle(passationId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateAttachmentTitle"))
        }
    }

    override suspend fun deleteAttachment(passationId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePassationAttachment(passationId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteAttachment"))
        }
    }

    // ─── Chef montant credential verification ───────────────────────

    override suspend fun verifyIdentity(username: String, password: String): Resource<VerifiedIdentity> {
        return try {
            apiService.verifyIdentity(VerifyIdentityRequest(username, password))
                .extract("Identifiant ou mot de passe invalide")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "verifyIdentity"))
        }
    }
}
