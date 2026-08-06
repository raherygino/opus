package com.gsoft.opus.domain.repository

interface DeviceTokenRepository {
    suspend fun registerToken(token: String, deviceName: String?): Boolean
    suspend fun unregisterToken(token: String): Boolean
    suspend fun unregisterAll(): Boolean
}
