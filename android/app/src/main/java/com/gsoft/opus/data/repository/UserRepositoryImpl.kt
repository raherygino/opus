package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.UserCreateRequest
import com.gsoft.opus.data.api.dto.UserUpdateRequest
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.User
import com.gsoft.opus.domain.repository.UserFormData
import com.gsoft.opus.domain.repository.UserRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {

    companion object {
        private const val TAG = "UserRepo"
    }

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

    override suspend fun getUsers(): Resource<List<User>> {
        return try {
            apiService.getUserList()
                .extract("Impossible de charger la liste des utilisateurs")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getUsers"))
        }
    }

    override suspend fun getUser(id: Int): Resource<User> {
        return try {
            apiService.getUser(id)
                .extract("Utilisateur introuvable")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getUser"))
        }
    }

    override suspend fun createUser(data: UserFormData): Resource<User> {
        return try {
            apiService.createUser(
                UserCreateRequest(
                    personnelId = data.personnelId ?: 0,
                    username = data.username.trim(),
                    password = data.password ?: "",
                    roleId = data.roleId
                )
            ).extract("Impossible de créer l'utilisateur")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "createUser"))
        }
    }

    override suspend fun updateUser(id: Int, data: UserFormData): Resource<User> {
        return try {
            apiService.updateUser(
                id,
                UserUpdateRequest(
                    username = data.username.trim(),
                    password = data.password?.takeIf { it.isNotBlank() },
                    roleId = data.roleId,
                    isActive = if (data.isActive == true) 1 else 0
                )
            ).extract("Impossible de mettre à jour l'utilisateur")
                .map { it.toDomain() }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "updateUser"))
        }
    }

    override suspend fun deleteUser(id: Int): Resource<Unit> {
        return try {
            val response = apiService.deleteUser(id)
            if (response.isSuccessful && response.body()?.success == true) Resource.success(Unit)
            else Resource.error(response.body()?.message ?: "Impossible de supprimer cet utilisateur", response.code())
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "deleteUser"))
        }
    }

    override suspend fun getAvailablePersonnel(): Resource<List<Personnel>> {
        return try {
            apiService.getAvailablePersonnel()
                .extract("Impossible de charger les agents disponibles")
                .map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Resource.error(failureMessage(e, "getAvailablePersonnel"))
        }
    }
}
