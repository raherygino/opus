package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.PlainteEntreeRequest
import com.gsoft.opus.data.api.dto.PlainteSortieRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.PlainteEntree
import com.gsoft.opus.domain.model.PlainteEntreeAttachment
import com.gsoft.opus.domain.model.PlainteEntreeSummary
import com.gsoft.opus.domain.model.PlainteSortie
import com.gsoft.opus.domain.model.PlainteSortieAttachment
import com.gsoft.opus.domain.repository.PlainteEntreeFormData
import com.gsoft.opus.domain.repository.PlainteRepository
import com.gsoft.opus.domain.repository.PlainteSortieFormData
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlainteRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : PlainteRepository {

    companion object {
        private const val TAG = "PlainteRepo"
    }

    private fun PlainteEntreeFormData.toRequest() = PlainteEntreeRequest(
        type = type,
        datePlainte = datePlainte,
        numeroDossier = numeroDossier?.takeIf { it.isNotBlank() },
        numeroSt = numeroSt,
        opjPersonnelId = opjPersonnelId,
        enqueteurPersonnelId = enqueteurPersonnelId,
        partieCivile = partieCivile,
        miseEnCause = miseEnCause,
        adressePc = adressePc,
        infraction = infraction,
        prejudice = prejudice,
        lieuInfraction = lieuInfraction,
        heureInfraction = heureInfraction,
        observation = observation
    )

    private fun PlainteSortieFormData.toRequest() = PlainteSortieRequest(
        plainteEntreeId = plainteEntreeId,
        nature = nature,
        dateSortie = dateSortie,
        numero = numero?.takeIf { it.isNotBlank() },
        numeroTtr = numeroTtr,
        nomSubstitut = nomSubstitut,
        dateDeferrement = dateDeferrement,
        observation = observation
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

    private fun failureMessage(e: Exception, what: String): String {
        return if (e is IOException) {
            "Erreur réseau. Vérifiez votre connexion."
        } else {
            Log.e(TAG, "$what failed", e)
            "Une erreur inattendue s'est produite."
        }
    }

    // ── ENTRÉE ──────────────────────────────────────────────────────

    override suspend fun getPlainteEntreeList(
        type: String?,
        search: String?,
        dateFrom: String?,
        dateTo: String?
    ): Resource<List<PlainteEntree>> {
        return try {
            apiService.getPlainteEntreeList(
                type?.takeIf { it.isNotBlank() },
                search?.takeIf { it.isNotBlank() },
                dateFrom?.takeIf { it.isNotBlank() },
                dateTo?.takeIf { it.isNotBlank() }
            ).extract("Impossible de charger les plaintes").map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPlainteEntreeList"))
        }
    }

    override suspend fun getPlainteEntree(id: Int): Resource<PlainteEntree> {
        return try {
            apiService.getPlainteEntree(id).extract("Plainte introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPlainteEntree"))
        }
    }

    override suspend fun createPlainteEntree(data: PlainteEntreeFormData): Resource<PlainteEntree> {
        return try {
            apiService.createPlainteEntree(data.toRequest())
                .extract("Impossible d'enregistrer la plainte")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createPlainteEntree"))
        }
    }

    override suspend fun updatePlainteEntree(id: Int, data: PlainteEntreeFormData): Resource<PlainteEntree> {
        return try {
            apiService.updatePlainteEntree(id, data.toRequest())
                .extract("Impossible de mettre à jour la plainte")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updatePlainteEntree"))
        }
    }

    override suspend fun deletePlainteEntree(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePlainteEntree(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette plainte", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deletePlainteEntree"))
        }
    }

    override suspend fun getEntreesWithoutSortie(): Resource<List<PlainteEntreeSummary>> {
        return try {
            apiService.getPlaintesEntreeWithoutSortie()
                .extract("Impossible de charger les plaintes sans sortie")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getEntreesWithoutSortie"))
        }
    }

    override suspend fun peekEntreeNumber(type: String): Resource<String> {
        return try {
            when (val result = apiService.getPlainteEntreeNextNumber(type).extract("Impossible de récupérer le numéro suggéré")) {
                is Resource.Success -> Resource.success(result.data.numeroDossier ?: "")
                is Resource.Error -> Resource.error(result.message, result.code)
                is Resource.Loading -> Resource.loading()
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "peekEntreeNumber"))
        }
    }

    override suspend fun getEntreeAttachments(plainteEntreeId: Int): Resource<List<PlainteEntreeAttachment>> {
        return try {
            apiService.getPlainteEntreeAttachments(plainteEntreeId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getEntreeAttachments"))
        }
    }

    override suspend fun addEntreeAttachment(plainteEntreeId: Int, title: String, file: UploadFile): Resource<PlainteEntreeAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createPlainteEntreeAttachment(plainteEntreeId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addEntreeAttachment"))
        }
    }

    override suspend fun updateEntreeAttachmentTitle(plainteEntreeId: Int, attachId: Int, title: String): Resource<PlainteEntreeAttachment> {
        return try {
            apiService.updatePlainteEntreeAttachmentTitle(plainteEntreeId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateEntreeAttachmentTitle"))
        }
    }

    override suspend fun deleteEntreeAttachment(plainteEntreeId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePlainteEntreeAttachment(plainteEntreeId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteEntreeAttachment"))
        }
    }

    // ── SORTIE ──────────────────────────────────────────────────────

    override suspend fun getPlainteSortieList(
        nature: String?,
        entreeId: Int?,
        search: String?,
        dateFrom: String?,
        dateTo: String?
    ): Resource<List<PlainteSortie>> {
        return try {
            apiService.getPlainteSortieList(
                nature?.takeIf { it.isNotBlank() },
                entreeId,
                search?.takeIf { it.isNotBlank() },
                dateFrom?.takeIf { it.isNotBlank() },
                dateTo?.takeIf { it.isNotBlank() }
            ).extract("Impossible de charger les sorties").map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPlainteSortieList"))
        }
    }

    override suspend fun getPlainteSortie(id: Int): Resource<PlainteSortie> {
        return try {
            apiService.getPlainteSortie(id).extract("Sortie introuvable").map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getPlainteSortie"))
        }
    }

    override suspend fun createPlainteSortie(data: PlainteSortieFormData): Resource<PlainteSortie> {
        return try {
            apiService.createPlainteSortie(data.toRequest())
                .extract("Impossible d'enregistrer la sortie")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createPlainteSortie"))
        }
    }

    override suspend fun updatePlainteSortie(id: Int, data: PlainteSortieFormData): Resource<PlainteSortie> {
        return try {
            apiService.updatePlainteSortie(id, data.toRequest())
                .extract("Impossible de mettre à jour la sortie")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updatePlainteSortie"))
        }
    }

    override suspend fun deletePlainteSortie(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePlainteSortie(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cette sortie", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deletePlainteSortie"))
        }
    }

    override suspend fun peekSortieNumber(): Resource<String> {
        return try {
            when (val result = apiService.getPlainteSortieNextNumber().extract("Impossible de récupérer le numéro suggéré")) {
                is Resource.Success -> Resource.success(result.data.numero ?: "")
                is Resource.Error -> Resource.error(result.message, result.code)
                is Resource.Loading -> Resource.loading()
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "peekSortieNumber"))
        }
    }

    override suspend fun getSortieAttachments(plainteSortieId: Int): Resource<List<PlainteSortieAttachment>> {
        return try {
            apiService.getPlainteSortieAttachments(plainteSortieId)
                .extract("Impossible de charger les pièces jointes")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getSortieAttachments"))
        }
    }

    override suspend fun addSortieAttachment(plainteSortieId: Int, title: String, file: UploadFile): Resource<PlainteSortieAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            apiService.createPlainteSortieAttachment(plainteSortieId, titleBody, part)
                .extract("Impossible d'ajouter la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addSortieAttachment"))
        }
    }

    override suspend fun updateSortieAttachmentTitle(plainteSortieId: Int, attachId: Int, title: String): Resource<PlainteSortieAttachment> {
        return try {
            apiService.updatePlainteSortieAttachmentTitle(plainteSortieId, attachId, AttachmentTitleRequest(title))
                .extract("Impossible de modifier la pièce jointe")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateSortieAttachmentTitle"))
        }
    }

    override suspend fun deleteSortieAttachment(plainteSortieId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deletePlainteSortieAttachment(plainteSortieId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteSortieAttachment"))
        }
    }
}
