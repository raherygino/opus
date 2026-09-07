package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.AffectationMaterielLigneRequest
import com.gsoft.opus.data.api.dto.AffectationMaterielRequest
import com.gsoft.opus.data.api.dto.ReintegrationMaterielRequest
import com.gsoft.opus.data.api.dto.TypeMaterielRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.AffectationMateriel
import com.gsoft.opus.domain.model.TypeMateriel
import com.gsoft.opus.domain.repository.AffectationMaterielFormData
import com.gsoft.opus.domain.repository.MaterielRepository
import com.gsoft.opus.domain.repository.ReintegrationMaterielData
import com.gsoft.opus.domain.repository.TypeMaterielFormData
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterielRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MaterielRepository {

    companion object {
        private const val TAG = "MaterielRepo"
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

    // ─── TypeMateriel ────────────────────────────────────────────────

    override suspend fun getTypeMaterielList(search: String?): Resource<List<TypeMateriel>> = try {
        apiService.getTypeMaterielList(search?.takeIf { it.isNotBlank() })
            .extract("Impossible de charger les types de matériel")
            .map { list -> list.map { it.toDomain() } }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getTypeMaterielList"))
    }

    override suspend fun getTypeMateriel(id: Int): Resource<TypeMateriel> = try {
        apiService.getTypeMateriel(id).extract("Type de matériel introuvable").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getTypeMateriel"))
    }

    override suspend fun createTypeMateriel(data: TypeMaterielFormData): Resource<TypeMateriel> = try {
        apiService.createTypeMateriel(
            TypeMaterielRequest(nom = data.nom.trim(), description = data.description?.trim())
        ).extract("Impossible de créer le type de matériel").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "createTypeMateriel"))
    }

    override suspend fun updateTypeMateriel(id: Int, data: TypeMaterielFormData): Resource<TypeMateriel> = try {
        apiService.updateTypeMateriel(
            id,
            TypeMaterielRequest(nom = data.nom.trim(), description = data.description?.trim())
        ).extract("Impossible de modifier le type de matériel").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "updateTypeMateriel"))
    }

    override suspend fun deleteTypeMateriel(id: Int): Resource<Unit> = try {
        apiService.deleteTypeMateriel(id).extract("Impossible de supprimer le type de matériel")
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "deleteTypeMateriel"))
    }

    // ─── AffectationMateriel ─────────────────────────────────────────

    override suspend fun getAffectationMaterielList(
        search: String?,
        statut: String?,
        agentPersonnelId: Int?
    ): Resource<List<AffectationMateriel>> = try {
        apiService.getAffectationMaterielList(
            search?.takeIf { it.isNotBlank() },
            statut?.takeIf { it.isNotBlank() },
            agentPersonnelId
        ).extract("Impossible de charger les affectations")
            .map { list -> list.map { it.toDomain() } }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getAffectationMaterielList"))
    }

    override suspend fun getAffectationMateriel(id: Int): Resource<AffectationMateriel> = try {
        apiService.getAffectationMateriel(id).extract("Affectation introuvable").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getAffectationMateriel"))
    }

    private fun AffectationMaterielFormData.toRequest() = AffectationMaterielRequest(
        agentPersonnelId = agentPersonnelId,
        datePerception = datePerception,
        heurePerception = heurePerception,
        observations = observations?.trim()?.ifBlank { null },
        lignes = lignes.map {
            AffectationMaterielLigneRequest(
                typeMaterielId = it.typeMaterielId,
                numeroMateriel = it.numeroMateriel.trim(),
                etatEmport = it.etatEmport?.trim()?.ifBlank { null }
            )
        },
        codeSecret = codeSecret?.trim()?.ifBlank { null },
        signatureSvg = signatureSvg
    )

    override suspend fun createAffectationMateriel(data: AffectationMaterielFormData): Resource<AffectationMateriel> = try {
        apiService.createAffectationMateriel(data.toRequest())
            .extract("Impossible de créer l'affectation").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "createAffectationMateriel"))
    }

    override suspend fun updateAffectationMateriel(id: Int, data: AffectationMaterielFormData): Resource<AffectationMateriel> = try {
        apiService.updateAffectationMateriel(id, data.toRequest())
            .extract("Impossible de modifier l'affectation").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "updateAffectationMateriel"))
    }

    override suspend fun reintegrateAffectationMateriel(id: Int, data: ReintegrationMaterielData): Resource<AffectationMateriel> = try {
        apiService.reintegrateAffectationMateriel(
            id,
            ReintegrationMaterielRequest(
                dateReintegration = data.dateReintegration,
                heureReintegration = data.heureReintegration,
                ligneEtats = data.ligneEtats.mapValues { it.value.trim() }
            )
        ).extract("Erreur lors de la réintégration").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "reintegrateAffectationMateriel"))
    }

    override suspend fun deleteAffectationMateriel(id: Int): Resource<Unit> = try {
        apiService.deleteAffectationMateriel(id).extract("Impossible de supprimer l'affectation")
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "deleteAffectationMateriel"))
    }
}
