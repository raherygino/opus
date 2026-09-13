package com.gsoft.opus.data.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.ApiResponse
import com.gsoft.opus.data.api.dto.MainCouranteCategorieDto
import com.gsoft.opus.data.api.dto.MainCouranteCategorieRequest
import com.gsoft.opus.domain.repository.MainCouranteCategorie
import com.gsoft.opus.domain.repository.MainCouranteCategorieFormData
import com.gsoft.opus.domain.repository.MainCouranteCategorieRepository
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainCouranteCategorieRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MainCouranteCategorieRepository {

    override suspend fun getCategories(): Resource<List<MainCouranteCategorie>> {
        return try {
            val response = apiService.getMainCouranteCategories()
            handleListResponse(response)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Erreur réseau")
        }
    }

    override suspend fun createCategory(data: MainCouranteCategorieFormData): Resource<MainCouranteCategorie> {
        return try {
            val response = apiService.createMainCouranteCategorie(MainCouranteCategorieRequest(data.label.trim()))
            handleSingleResponse(response)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Erreur réseau")
        }
    }

    override suspend fun updateCategory(id: Int, data: MainCouranteCategorieFormData): Resource<MainCouranteCategorie> {
        return try {
            val response = apiService.updateMainCouranteCategorie(id, MainCouranteCategorieRequest(data.label.trim()))
            handleSingleResponse(response)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Erreur réseau")
        }
    }

    override suspend fun deleteCategory(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteMainCouranteCategorie(id)
            if (response.isSuccessful) {
                Resource.success(Unit)
            } else {
                Resource.error(parseErrorMessage(response), response.code())
            }
        } catch (e: Exception) {
            Resource.error(e.message ?: "Erreur réseau")
        }
    }

    private fun handleListResponse(response: Response<ApiResponse<List<MainCouranteCategorieDto>>>): Resource<List<MainCouranteCategorie>> {
        return if (response.isSuccessful) {
            val data = response.body()?.data?.map { it.toDomain() } ?: emptyList()
            Resource.success(data)
        } else {
            Resource.error(parseErrorMessage(response), response.code())
        }
    }

    private fun handleSingleResponse(response: Response<ApiResponse<MainCouranteCategorieDto>>): Resource<MainCouranteCategorie> {
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

private fun MainCouranteCategorieDto.toDomain(): MainCouranteCategorie =
    MainCouranteCategorie(id = id, label = label)
