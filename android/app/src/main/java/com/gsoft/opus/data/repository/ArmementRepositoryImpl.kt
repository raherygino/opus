package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.ArmementRequest
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.ReintegrationRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Armement
import com.gsoft.opus.domain.model.ArmementAttachment
import com.gsoft.opus.domain.repository.ArmementFormData
import com.gsoft.opus.domain.repository.ArmementRepository
import com.gsoft.opus.domain.repository.ReintegrationData
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArmementRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ArmementRepository {

    companion object {
        private const val TAG = "ArmementRepo"
    }

    private fun ArmementFormData.toRequest() = ArmementRequest(
        datePerception = datePerception,
        heurePerception = heurePerception,
        agentPreneurPersonnelId = agentPreneurPersonnelId,
        armeId = armeId,
        typeArme = typeArme.trim(),
        matriculeArme = matriculeArme.trim(),
        munitions = munitions,
        secteurMission = secteurMission.trim(),
        etatPerception = etatPerception.trim(),
        codeSecret = codeSecret,
        signatureSvg = signatureSvg,
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

    private fun failureMessage(e: Exception, tag: String, what: String): String {
        return if (e is IOException) {
            "Erreur réseau. Vérifiez votre connexion."
        } else {
            Log.e(tag, "$what failed", e)
            "Une erreur inattendue s'est produite."
        }
    }

    override suspend fun getArmementList(
        search: String?,
        dateFrom: String?,
        dateTo: String?,
        statut: String?
    ): Resource<List<Armement>> {
        return try {
            val result = apiService.getArmementList(
                search?.takeIf { it.isNotBlank() },
                dateFrom?.takeIf { it.isNotBlank() },
                dateTo?.takeIf { it.isNotBlank() },
                statut?.takeIf { it.isNotBlank() }
            ).extract("Impossible de charger la liste des armements")
            result.map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getArmementList"))
        }
    }

    override suspend fun getArmement(id: Int): Resource<Armement> {
        return try {
            apiService.getArmement(id).extract("Armement introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getArmement"))
        }
    }

    override suspend fun createArmement(data: ArmementFormData): Resource<Armement> {
        return try {
            apiService.createArmement(data.toRequest())
                .extract("Impossible d'enregistrer la perception")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "createArmement"))
        }
    }

    override suspend fun updateArmement(id: Int, data: ArmementFormData): Resource<Armement> {
        return try {
            apiService.updateArmement(id, data.toRequest())
                .extract("Impossible de mettre à jour la perception")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateArmement"))
        }
    }

    override suspend fun reintegrateArmement(id: Int, data: ReintegrationData): Resource<Armement> {
        return try {
            apiService.reintegrateArmement(
                id,
                ReintegrationRequest(
                    heureReintegration = data.heureReintegration,
                    dateReintegration = data.dateReintegration,
                    etatReintegration = data.etatReintegration.trim(),
                    munitionsConsommees = data.munitionsConsommees,
                    reintegrationLatitude = data.reintegrationLatitude,
                    reintegrationLongitude = data.reintegrationLongitude
                )
            )
                .extract("Impossible de réintégrer l'arme")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "reintegrateArmement"))
        }
    }

    override suspend fun deleteArmement(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteArmement(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cet armement", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteArmement"))
        }
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(armementId: Int): Resource<List<ArmementAttachment>> {
        return try {
            apiService.getArmementAttachments(armementId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "getAttachments"))
        }
    }

    override suspend fun addAttachment(armementId: Int, title: String, file: UploadFile): Resource<ArmementAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createArmementAttachment(armementId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "addAttachment"))
        }
    }

    override suspend fun updateAttachmentTitle(armementId: Int, attachId: Int, title: String): Resource<ArmementAttachment> {
        return try {
            apiService.updateArmementAttachmentTitle(armementId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "updateAttachmentTitle"))
        }
    }

    override suspend fun deleteAttachment(armementId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteArmementAttachment(armementId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, TAG, "deleteAttachment"))
        }
    }
}
