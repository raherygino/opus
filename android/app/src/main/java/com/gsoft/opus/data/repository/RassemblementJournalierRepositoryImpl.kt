package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.RassemblementJournalierRequest
import com.gsoft.opus.data.api.dto.RepartitionSecteurRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.RassemblementJournalier
import com.gsoft.opus.domain.repository.RassemblementJournalierFormData
import com.gsoft.opus.domain.repository.RassemblementJournalierRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RassemblementJournalierRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RassemblementJournalierRepository {

    companion object {
        private const val TAG = "RassemblementRepo"
    }

    private fun RassemblementJournalierFormData.toRequest() = RassemblementJournalierRequest(
        dateRassemblement = dateRassemblement,
        heureRassemblement = heureRassemblement,
        brigadeService = brigadeService.trim(),
        officierPermanence = officierPermanence?.trim()?.ifBlank { null },
        inspecteurPermanence = inspecteurPermanence?.trim()?.ifBlank { null },
        chefPoste = chefPoste?.trim()?.ifBlank { null },
        instructionsAutorite = instructionsAutorite?.trim()?.ifBlank { null },
        effectifTheorique = effectifTheorique,
        present = present,
        absent = absent,
        motifAbsence = motifAbsence?.trim()?.ifBlank { null },
        repartitions = repartitions.map {
            RepartitionSecteurRequest(
                type = it.type,
                secteur = it.secteur.trim(),
                effectifEngage = it.effectifEngage?.trim()?.ifBlank { null },
                chefElementContact = it.chefElementContact?.trim()?.ifBlank { null },
                controleContact = it.controleContact?.trim()?.ifBlank { null },
                materielsArmements = it.materielsArmements?.trim()?.ifBlank { null },
                missions = it.missions?.trim()?.ifBlank { null }
            )
        }
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

    override suspend fun getRassemblementList(search: String?): Resource<List<RassemblementJournalier>> {
        return try {
            apiService.getRassemblementList(search?.takeIf { it.isNotBlank() })
                .extract("Impossible de charger les rassemblements")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRassemblementList"))
        }
    }

    override suspend fun getRassemblement(id: Int): Resource<RassemblementJournalier> {
        return try {
            apiService.getRassemblement(id)
                .extract("Rassemblement journalier introuvable")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getRassemblement"))
        }
    }

    override suspend fun createRassemblement(data: RassemblementJournalierFormData): Resource<RassemblementJournalier> {
        return try {
            apiService.createRassemblement(data.toRequest())
                .extract("Impossible d'enregistrer le rassemblement")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createRassemblement"))
        }
    }

    override suspend fun updateRassemblement(id: Int, data: RassemblementJournalierFormData): Resource<RassemblementJournalier> {
        return try {
            apiService.updateRassemblement(id, data.toRequest())
                .extract("Impossible de mettre à jour le rassemblement")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateRassemblement"))
        }
    }

    override suspend fun deleteRassemblement(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteRassemblement(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer le rassemblement", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteRassemblement"))
        }
    }
}
