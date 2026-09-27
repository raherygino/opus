package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Rapport
import com.gsoft.opus.domain.repository.RapportRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RapportRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RapportRepository {

    companion object {
        private const val TAG = "RapportRepo"
    }

    override suspend fun getRapport(type: String, date: String): Resource<Rapport> {
        return try {
            val response = apiService.getRapport(type, date)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                if (data != null) Resource.success(data.toDomain())
                else Resource.error(response.body()?.message ?: "Rapport indisponible", response.code())
            } else {
                Resource.error(response.body()?.message ?: "Rapport indisponible", response.code())
            }
        } catch (e: Exception) {
            if (e is IOException) {
                Resource.error("Erreur réseau. Vérifiez votre connexion.")
            } else {
                Log.e(TAG, "getRapport failed", e)
                Resource.error("Une erreur inattendue s'est produite.")
            }
        }
    }
}
