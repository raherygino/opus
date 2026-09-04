package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.ArmeRequest
import com.gsoft.opus.data.api.dto.ConsommationRequest
import com.gsoft.opus.data.api.dto.TypeArmeRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Arme
import com.gsoft.opus.domain.model.ArmeMunitionsConsommation
import com.gsoft.opus.domain.model.TypeArme
import com.gsoft.opus.domain.repository.ArmeFormData
import com.gsoft.opus.domain.repository.ArmeRepository
import com.gsoft.opus.domain.repository.ConsommationData
import com.gsoft.opus.domain.repository.TypeArmeFormData
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArmeRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ArmeRepository {

    companion object {
        private const val TAG = "ArmeRepo"
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

    // ─── TypeArme ────────────────────────────────────────────────────

    override suspend fun getTypeArmeList(search: String?): Resource<List<TypeArme>> = try {
        apiService.getTypeArmeList(search?.takeIf { it.isNotBlank() })
            .extract("Impossible de charger les types d'armes")
            .map { list -> list.map { it.toDomain() } }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getTypeArmeList"))
    }

    override suspend fun getTypeArme(id: Int): Resource<TypeArme> = try {
        apiService.getTypeArme(id).extract("Type d'arme introuvable").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getTypeArme"))
    }

    override suspend fun createTypeArme(data: TypeArmeFormData): Resource<TypeArme> = try {
        apiService.createTypeArme(
            TypeArmeRequest(nom = data.nom.trim(), description = data.description?.trim(), munitionsStock = data.munitionsStock)
        ).extract("Impossible de créer le type d'arme").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "createTypeArme"))
    }

    override suspend fun updateTypeArme(id: Int, data: TypeArmeFormData): Resource<TypeArme> = try {
        apiService.updateTypeArme(
            id,
            TypeArmeRequest(nom = data.nom.trim(), description = data.description?.trim(), munitionsStock = data.munitionsStock)
        ).extract("Impossible de modifier le type d'arme").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "updateTypeArme"))
    }

    override suspend fun deleteTypeArme(id: Int): Resource<Unit> = try {
        val response = apiService.deleteTypeArme(id)
        if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
        else Resource.error(response.body()?.message ?: "Impossible de supprimer ce type d'arme", response.code())
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "deleteTypeArme"))
    }

    // ─── Arme ────────────────────────────────────────────────────────

    override suspend fun getArmeList(typeArmeId: Int?, search: String?): Resource<List<Arme>> = try {
        apiService.getArmeList(
            typeArmeId?.takeIf { it > 0 },
            search?.takeIf { it.isNotBlank() }
        ).extract("Impossible de charger les armes")
            .map { list -> list.map { it.toDomain() } }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getArmeList"))
    }

    override suspend fun getArme(id: Int): Resource<Arme> = try {
        apiService.getArme(id).extract("Arme introuvable").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getArme"))
    }

    override suspend fun createArme(data: ArmeFormData): Resource<Arme> = try {
        apiService.createArme(
            ArmeRequest(
                typeArmeId = data.typeArmeId,
                matricule = data.matricule.trim(),
                munitionsStock = data.munitionsStock
            )
        ).extract("Impossible de créer l'arme").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "createArme"))
    }

    override suspend fun updateArme(id: Int, data: ArmeFormData): Resource<Arme> = try {
        apiService.updateArme(
            id,
            ArmeRequest(
                typeArmeId = data.typeArmeId,
                matricule = data.matricule.trim(),
                munitionsStock = data.munitionsStock
            )
        ).extract("Impossible de modifier l'arme").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "updateArme"))
    }

    override suspend fun deleteArme(id: Int): Resource<Unit> = try {
        val response = apiService.deleteArme(id)
        if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
        else Resource.error(response.body()?.message ?: "Impossible de supprimer cette arme", response.code())
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "deleteArme"))
    }

    // ─── Consommation ────────────────────────────────────────────────

    override suspend fun recordConsommation(armeId: Int, data: ConsommationData): Resource<Arme> = try {
        apiService.recordConsommation(
            armeId,
            ConsommationRequest(
                agentId = data.agentId,
                quantite = data.quantite,
                armementId = data.armementId
            )
        ).extract("Impossible d'enregistrer la consommation").map { it.toDomain() }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "recordConsommation"))
    }

    override suspend fun getConsommations(armeId: Int): Resource<List<ArmeMunitionsConsommation>> = try {
        apiService.getArmeConsommations(armeId)
            .extract("Impossible de charger l'historique des consommations")
            .map { list -> list.map { it.toDomain() } }
    } catch (e: Exception) {
        Resource.error(failureMessage(e, "getConsommations"))
    }
}
