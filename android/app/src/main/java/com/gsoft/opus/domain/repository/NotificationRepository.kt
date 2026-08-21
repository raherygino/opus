package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.AppNotification

interface NotificationRepository {
    suspend fun getNotifications(): Resource<List<AppNotification>>
    suspend fun getUnreadCount(): Resource<Int>
    suspend fun markAsRead(id: Int): Boolean
    suspend fun markAllAsRead(): Boolean
    suspend fun delete(id: Int): Boolean
}
