package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.DispositifEffectifRequest
import com.gsoft.opus.data.api.dto.DispositifExceptionnelRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.DispositifExceptionnel
import com.gsoft.opus.domain.repository.DispositifExceptionnelFormData
import com.gsoft.opus.domain.repository.DispositifExceptionnelRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DispositifExceptionnelRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : DispositifExceptionnelRepository {

    companion object {
        private const val TAG = "DispositifRepo"
    }

    private fun DispositifExceptionnelFormData.toRequest() = DispositifExceptionnelRequest(
        natureEvenement = natureEvenement.trim(),
        dateDebut = dateDebut,
        dateFin = dateFin,
        effectifs = effectifs.map {
            DispositifEffectifRequest(
                secteur = it.secteur.trim(),
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

    override suspend fun getDispositifList(search: String?): Resource<List<DispositifExceptionnel>> {
        return try {
            apiService.getDispositifExceptionnelList(search?.takeIf { it.isNotBlank() })
                .extract("Impossible de charger les dispositifs exceptionnels")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getDispositifList"))
        }
    }

    override suspend fun getDispositif(id: Int): Resource<DispositifExceptionnel> {
        return try {
            apiService.getDispositifExceptionnel(id)
                .extract("Dispositif exceptionnel introuvable")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getDispositif"))
        }
    }

    override suspend fun createDispositif(data: DispositifExceptionnelFormData): Resource<DispositifExceptionnel> {
        return try {
            apiService.createDispositifExceptionnel(data.toRequest())
                .extract("Impossible d'enregistrer le dispositif exceptionnel")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createDispositif"))
        }
    }

    override suspend fun updateDispositif(id: Int, data: DispositifExceptionnelFormData): Resource<DispositifExceptionnel> {
        return try {
            apiService.updateDispositifExceptionnel(id, data.toRequest())
                .extract("Impossible de mettre à jour le dispositif exceptionnel")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateDispositif"))
        }
    }

    override suspend fun deleteDispositif(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteDispositifExceptionnel(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer le dispositif exceptionnel", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteDispositif"))
        }
    }
}
