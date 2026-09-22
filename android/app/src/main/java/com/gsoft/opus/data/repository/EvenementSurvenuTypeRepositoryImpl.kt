package com.gsoft.opus.data.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.ApiResponse
import com.gsoft.opus.data.api.dto.EvenementSurvenuTypeDto
import com.gsoft.opus.data.api.dto.EvenementSurvenuTypeRequest
import com.gsoft.opus.domain.repository.EvenementSurvenuType
import com.gsoft.opus.domain.repository.EvenementSurvenuTypeFormData
import com.gsoft.opus.domain.repository.EvenementSurvenuTypeRepository
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvenementSurvenuTypeRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : EvenementSurvenuTypeRepository {

    override suspend fun getTypes(): Resource<List<EvenementSurvenuType>> {
        return try {
            val response = apiService.getEvenementSurvenuTypes()
            handleListResponse(response)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Erreur réseau")
        }
    }

    override suspend fun createType(data: EvenementSurvenuTypeFormData): Resource<EvenementSurvenuType> {
        return try {
            val response = apiService.createEvenementSurvenuType(EvenementSurvenuTypeRequest(data.label.trim()))
            handleSingleResponse(response)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Erreur réseau")
        }
    }

    override suspend fun updateType(id: Int, data: EvenementSurvenuTypeFormData): Resource<EvenementSurvenuType> {
        return try {
            val response = apiService.updateEvenementSurvenuType(id, EvenementSurvenuTypeRequest(data.label.trim()))
            handleSingleResponse(response)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Erreur réseau")
        }
    }

    override suspend fun deleteType(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteEvenementSurvenuType(id)
            if (response.isSuccessful) {
                Resource.success(Unit)
            } else {
                Resource.error(parseErrorMessage(response), response.code())
            }
        } catch (e: Exception) {
            Resource.error(e.message ?: "Erreur réseau")
        }
    }

    private fun handleListResponse(response: Response<ApiResponse<List<EvenementSurvenuTypeDto>>>): Resource<List<EvenementSurvenuType>> {
        return if (response.isSuccessful) {
            val data = response.body()?.data?.map { it.toDomain() } ?: emptyList()
            Resource.success(data)
        } else {
            Resource.error(parseErrorMessage(response), response.code())
        }
    }

    private fun handleSingleResponse(response: Response<ApiResponse<EvenementSurvenuTypeDto>>): Resource<EvenementSurvenuType> {
        return if (response.isSuccessful) {
            val dto = response.body()?.data
            if (dto != null) Resource.success(dto.toDomain())
            else Resource.error("Réponse invalide")
        } else {
            Resource.error(parseErrorMessage(response), response.code())
        }
    }

    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
            val errors = json.get("errors")?.asJsonObject
            if (errors != null && errors.size() > 0) {
                errors.entrySet().firstOrNull()?.value?.asString
                    ?: json.get("message")?.asString ?: "Erreur"
            } else {
                json.get("message")?.asString ?: "Erreur"
            }
        } catch (_: Exception) {
            "Erreur"
        }
    }
}

private fun EvenementSurvenuTypeDto.toDomain(): EvenementSurvenuType =
    EvenementSurvenuType(id = id, label = label)
