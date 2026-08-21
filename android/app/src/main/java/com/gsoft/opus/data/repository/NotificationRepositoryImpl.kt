package com.gsoft.opus.data.repository

import android.util.Log
import com.gsoft.opus.core.Resource
import com.gsoft.opus.data.api.ApiService
import com.gsoft.opus.data.api.dto.toDomain
import com.gsoft.opus.domain.model.AppNotification
import com.gsoft.opus.domain.repository.NotificationRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : NotificationRepository {

    companion object {
        private const val TAG = "NotificationRepo"
    }

    override suspend fun getNotifications(): Resource<List<AppNotification>> {
        return try {
            val response = apiService.getNotifications()
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.success(response.body()!!.data?.map { it.toDomain() } ?: emptyList())
            } else {
                Resource.error(response.body()?.message ?: "Failed to load notifications", response.code())
            }
        } catch (e: IOException) {
            Resource.error("Network error. Check your connection.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load notifications", e)
            Resource.error("An unexpected error occurred.")
        }
    }

    override suspend fun getUnreadCount(): Resource<Int> {
        return try {
            val response = apiService.getUnreadCount()
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.success(response.body()!!.data?.count ?: 0)
            } else {
                Resource.error(response.body()?.message ?: "Failed to load unread count", response.code())
            }
        } catch (e: IOException) {
            Resource.error("Network error. Check your connection.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load unread count", e)
            Resource.error("An unexpected error occurred.")
        }
    }

    override suspend fun markAsRead(id: Int): Boolean {
        return try {
            val response = apiService.markNotificationAsRead(id)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.w(TAG, "Mark as read failed for id=$id: ${e.message}")
            false
        }
    }

    override suspend fun markAllAsRead(): Boolean {
        return try {
            val response = apiService.markAllNotificationsAsRead()
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.w(TAG, "Mark all as read failed: ${e.message}")
            false
        }
    }

    override suspend fun delete(id: Int): Boolean {
        return try {
            val response = apiService.deleteNotification(id)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.w(TAG, "Delete failed for id=$id: ${e.message}")
            false
        }
    }
}
