package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.DashboardStats
import com.gsoft.opus.domain.repository.DashboardRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : DashboardRepository {

    companion object {
        private const val TAG = "DashboardRepo"
    }

    override suspend fun getStats(): Resource<DashboardStats> {
        return try {
            val response = apiService.getDashboardStats()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                if (data != null) Resource.success(data.toDomain())
                else Resource.error(response.body()?.message ?: "Statistiques indisponibles", response.code())
            } else {
                Resource.error(response.body()?.message ?: "Statistiques indisponibles", response.code())
            }
        } catch (e: Exception) {
            if (e is IOException) {
                Resource.error("Erreur réseau. Vérifiez votre connexion.")
            } else {
                Log.e(TAG, "getStats failed", e)
                Resource.error("Une erreur inattendue s'est produite.")
            }
        }
    }
}
