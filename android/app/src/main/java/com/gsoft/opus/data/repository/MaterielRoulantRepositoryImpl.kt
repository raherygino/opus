package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.MaterielRoulantRequest
import com.gsoft.opus.data.api.dto.ReintegrationMaterielRoulantRequest
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.MaterielRoulant
import com.gsoft.opus.domain.model.MaterielRoulantAttachment
import com.gsoft.opus.domain.repository.MaterielRoulantFormData
import com.gsoft.opus.domain.repository.MaterielRoulantRepository
import com.gsoft.opus.domain.repository.ReintegrationMaterielRoulantData
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterielRoulantRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MaterielRoulantRepository {

    companion object {
        private const val TAG = "MaterielRoulantRepo"
    }

    private fun <T> Response<com.gsoft.opus.data.api.dto.ApiResponse<T>>.extract(defaultError: String): Resource<T> {
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

    override suspend fun getMaterielRoulantList(
        search: String?,
        statut: String?,
        typeMateriel: String?
    ): Resource<List<MaterielRoulant>> = try {
        apiService.getMaterielRoulantList(
            search?.takeIf { it.isNotBlank() },
            statut?.takeIf { it.isNotBlank() },
            typeMateriel?.takeIf { it.isNotBlank() }
        ).extract("Impossible de charger les matériels roulants")
            .map { list -> list.map { it.toDomain() } }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getMaterielRoulantList"))
    }

    override suspend fun getMaterielRoulant(id: Int): Resource<MaterielRoulant> = try {
        apiService.getMaterielRoulant(id).extract("Matériel roulant introuvable").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getMaterielRoulant"))
    }

    private fun MaterielRoulantFormData.toRequest() = MaterielRoulantRequest(
        datePerception = datePerception,
        heurePerception = heurePerception,
        typeMateriel = typeMateriel,
        numeroImmatriculation = numeroImmatriculation?.trim()?.ifBlank { null },
        descriptionVehicule = descriptionVehicule?.trim()?.ifBlank { null },
        agentConducteurPersonnelId = agentConducteurPersonnelId,
        chefDeBordPersonnelId = chefDeBordPersonnelId,
        kilometrageDepart = kilometrageDepart?.trim()?.ifBlank { null },
        niveauCarburantDepart = niveauCarburantDepart?.trim()?.ifBlank { null },
        codeSecret = codeSecret?.trim()?.ifBlank { null },
        signatureSvg = signatureSvg
    )

    override suspend fun createMaterielRoulant(data: MaterielRoulantFormData): Resource<MaterielRoulant> = try {
        apiService.createMaterielRoulant(data.toRequest())
            .extract("Impossible d'enregistrer la perception").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "createMaterielRoulant"))
    }

    override suspend fun updateMaterielRoulant(id: Int, data: MaterielRoulantFormData): Resource<MaterielRoulant> = try {
        apiService.updateMaterielRoulant(id, data.toRequest())
            .extract("Impossible de modifier la perception").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "updateMaterielRoulant"))
    }

    override suspend fun reintegrateMaterielRoulant(
        id: Int,
        data: ReintegrationMaterielRoulantData
    ): Resource<MaterielRoulant> = try {
        apiService.reintegrateMaterielRoulant(
            id,
            ReintegrationMaterielRoulantRequest(
                dateReintegration = data.dateReintegration,
                heureReintegration = data.heureReintegration,
                kilometrageRetour = data.kilometrageRetour.trim(),
                niveauCarburantRetour = data.niveauCarburantRetour?.trim()?.ifBlank { null },
                observationsTechniques = data.observationsTechniques?.trim()?.ifBlank { null },
                defaillances = data.defaillances?.trim()?.ifBlank { null }
            )
        ).extract("Erreur lors de la réintégration").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "reintegrateMaterielRoulant"))
    }

    override suspend fun deleteMaterielRoulant(id: Int): Resource<Unit> = try {
        apiService.deleteMaterielRoulant(id).extract("Impossible de supprimer le matériel roulant")
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "deleteMaterielRoulant"))
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(materielRoulantId: Int): Resource<List<MaterielRoulantAttachment>> {
        return try {
            apiService.getMaterielRoulantAttachments(materielRoulantId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getAttachments"))
        }
    }

    override suspend fun addAttachment(materielRoulantId: Int, title: String, file: UploadFile): Resource<MaterielRoulantAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createMaterielRoulantAttachment(materielRoulantId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addAttachment"))
        }
    }

    override suspend fun updateAttachmentTitle(materielRoulantId: Int, attachId: Int, title: String): Resource<MaterielRoulantAttachment> {
        return try {
            apiService.updateMaterielRoulantAttachmentTitle(materielRoulantId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateAttachmentTitle"))
        }
    }

    override suspend fun deleteAttachment(materielRoulantId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteMaterielRoulantAttachment(materielRoulantId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteAttachment"))
        }
    }
}
