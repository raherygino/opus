package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.ComportementRejectRequest
import com.gsoft.opus.data.api.dto.ComportementRequest
import com.gsoft.opus.data.api.dto.MouvementRequest
import com.gsoft.opus.data.api.dto.MouvementRetourRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Comportement
import com.gsoft.opus.domain.model.Mouvement
import com.gsoft.opus.domain.model.MouvementAttachment
import com.gsoft.opus.domain.repository.ComportementFormData
import com.gsoft.opus.domain.repository.ComportementRepository
import com.gsoft.opus.domain.repository.MouvementFormData
import com.gsoft.opus.domain.repository.MouvementRepository
import com.gsoft.opus.domain.repository.UploadFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MouvementRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MouvementRepository {

    companion object {
        private const val TAG = "MouvementRepo"
    }

    private fun failureMessage(e: Exception, what: String): String {
        return if (e is IOException) {
            "Erreur réseau. Vérifiez votre connexion."
        } else {
            Log.e(TAG, "$what failed", e)
            "Une erreur inattendue s'est produite."
        }
    }

    override suspend fun getMouvementList(personnelId: Int?): Resource<List<Mouvement>> {
        return try {
            val response = apiService.getMouvementList(personnelId = personnelId)
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.success(response.body()!!.data?.map { it.toDomain() } ?: emptyList())
            } else {
                Resource.error(response.body()?.message ?: "Impossible de charger les mouvements", response.code())
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getMouvementList"))
        }
    }

    override suspend fun createMouvement(data: MouvementFormData): Resource<Mouvement> {
        return try {
            val request = MouvementRequest(
                personnelId = data.personnelId,
                im = data.im,
                grade = data.grade,
                service = data.service,
                nom = data.nom,
                prenoms = data.prenoms,
                typeMouvement = data.typeMouvement,
                dateDepart = data.dateDepart?.takeIf { it.isNotBlank() },
                days = data.days,
                dateRetour = data.dateRetour?.takeIf { it.isNotBlank() }
            )
            val response = apiService.createMouvement(request)
            if (response.isSuccessful && response.body()?.success == true && response.body()!!.data != null) {
                Resource.success(response.body()!!.data!!.toDomain())
            } else {
                val errors = response.body()?.errors?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }
                Resource.error(errors ?: response.body()?.message ?: "Impossible d'ajouter le mouvement", response.code())
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createMouvement"))
        }
    }

    override suspend fun confirmRetour(mouvementId: Int, dateRetour: String?): Resource<Mouvement> {
        return try {
            val response = apiService.updateMouvement(
                mouvementId,
                MouvementRetourRequest(dateRetour = dateRetour?.takeIf { it.isNotBlank() })
            )
            if (response.isSuccessful && response.body()?.success == true && response.body()!!.data != null) {
                Resource.success(response.body()!!.data!!.toDomain())
            } else {
                Resource.error(response.body()?.message ?: "Impossible d'enregistrer le retour", response.code())
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "confirmRetour"))
        }
    }

    override suspend fun deleteMouvement(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteMouvement(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer ce mouvement", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteMouvement"))
        }
    }

    // ─── Attachments ────────────────────────────────────────────────

    override suspend fun getAttachments(mouvementId: Int): Resource<List<MouvementAttachment>> {
        return try {
            val response = apiService.getMouvementAttachments(mouvementId)
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.success(response.body()!!.data?.map { it.toDomain() } ?: emptyList())
            } else {
                Resource.error(response.body()?.message ?: "Impossible de charger les pièces jointes", response.code())
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getAttachments"))
        }
    }

    override suspend fun addAttachment(mouvementId: Int, title: String, file: UploadFile): Resource<MouvementAttachment> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.bytes.toRequestBody(file.mimeType?.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.fileName, fileBody)
            val response = apiService.createMouvementAttachment(mouvementId, titleBody, part)
            if (response.isSuccessful && response.body()?.success == true && response.body()!!.data != null) {
                Resource.success(response.body()!!.data!!.toDomain())
            } else {
                Resource.error(response.body()?.message ?: "Impossible d'ajouter la pièce jointe", response.code())
            }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "addAttachment"))
        }
    }

    override suspend fun deleteAttachment(mouvementId: Int, attachId: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteMouvementAttachment(mouvementId, attachId)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer la pièce jointe", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteAttachment"))
        }
    }
}

@Singleton
class ComportementRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ComportementRepository {

    companion object {
        private const val TAG = "ComportementRepo"
    }

    override suspend fun getComportementList(
        personnelId: Int?,
        status: String?
    ): Resource<List<Comportement>> {
        return try {
            val response = apiService.getComportementList(
                personnelId = personnelId,
                status = status
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.success(response.body()!!.data?.map { it.toDomain() } ?: emptyList())
            } else {
                Resource.error(response.body()?.message ?: "Impossible de charger les comportements", response.code())
            }
        } catch (e: IOException) {
            Resource.error("Erreur réseau. Vérifiez votre connexion.")
        } catch (e: Exception) {
            Log.e(TAG, "getComportementList failed", e)
            Resource.error("Une erreur inattendue s'est produite.")
        }
    }

    override suspend fun createComportement(data: ComportementFormData): Resource<Comportement> {
        return try {
            val request = ComportementRequest(
                personnelId = data.personnelId,
                im = data.im,
                grade = data.grade,
                service = data.service,
                nom = data.nom,
                prenoms = data.prenoms,
                type = data.type,
                dateComportement = data.dateComportement,
                motif = data.motif,
                decision = data.decision?.takeIf { it.isNotBlank() }
            )
            val response = apiService.createComportement(request)
            if (response.isSuccessful && response.body()?.success == true && response.body()!!.data != null) {
                Resource.success(response.body()!!.data!!.toDomain())
            } else {
                val errors = response.body()?.errors?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }
                Resource.error(errors ?: response.body()?.message ?: "Impossible d'ajouter le comportement", response.code())
            }
        } catch (e: IOException) {
            Resource.error("Erreur réseau. Vérifiez votre connexion.")
        } catch (e: Exception) {
            Log.e(TAG, "createComportement failed", e)
            Resource.error("Une erreur inattendue s'est produite.")
        }
    }

    override suspend fun confirmComportement(id: Int): Resource<Comportement> {
        return try {
            val response = apiService.confirmComportement(id)
            if (response.isSuccessful && response.body()?.success == true && response.body()!!.data != null) {
                Resource.success(response.body()!!.data!!.toDomain())
            } else {
                Resource.error(response.body()?.message ?: "Impossible de confirmer ce comportement", response.code())
            }
        } catch (e: IOException) {
            Resource.error("Erreur réseau. Vérifiez votre connexion.")
        } catch (e: Exception) {
            Log.e(TAG, "confirmComportement failed", e)
            Resource.error("Une erreur inattendue s'est produite.")
        }
    }

    override suspend fun rejectComportement(id: Int, reason: String?): Resource<Comportement> {
        return try {
            val response = apiService.rejectComportement(
                id,
                ComportementRejectRequest(reason = reason?.takeIf { it.isNotBlank() })
            )
            if (response.isSuccessful && response.body()?.success == true && response.body()!!.data != null) {
                Resource.success(response.body()!!.data!!.toDomain())
            } else {
                Resource.error(response.body()?.message ?: "Impossible de rejeter ce comportement", response.code())
            }
        } catch (e: IOException) {
            Resource.error("Erreur réseau. Vérifiez votre connexion.")
        } catch (e: Exception) {
            Log.e(TAG, "rejectComportement failed", e)
            Resource.error("Une erreur inattendue s'est produite.")
        }
    }

    override suspend fun deleteComportement(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteComportement(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer ce comportement", response.code())
        } catch (e: IOException) {
            Resource.error("Erreur réseau. Vérifiez votre connexion.")
        } catch (e: Exception) {
            Log.e(TAG, "deleteComportement failed", e)
            Resource.error("Une erreur inattendue s'est produite.")
        }
    }
}
