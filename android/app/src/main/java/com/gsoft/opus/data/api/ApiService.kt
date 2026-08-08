package com.gsoft.opus.data.api

import com.gsoft.opus.data.api.dto.ApiResponse
import com.gsoft.opus.data.api.dto.DeviceTokenRequestDto
import com.gsoft.opus.data.api.dto.DeviceTokenResponseDto
import com.gsoft.opus.data.api.dto.LoginRequestDto
import com.gsoft.opus.data.api.dto.LoginResponseDto
import com.gsoft.opus.data.api.dto.NotificationDto
import com.gsoft.opus.data.api.dto.RefreshResponseDto
import com.gsoft.opus.data.api.dto.RefreshTokenRequestDto
import com.gsoft.opus.data.api.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponse<LoginResponseDto>>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequestDto): Response<ApiResponse<RefreshResponseDto>>

    @GET("api/auth/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<ApiResponse<UserDto>>

    @GET("api/health")
    suspend fun healthCheck(): Response<ApiResponse<Nothing>>

    // ─── FCM Device Token Registration ──────────────────────────────

    @POST("api/devices/register")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequestDto): Response<ApiResponse<DeviceTokenResponseDto>>

    @POST("api/devices/unregister")
    suspend fun unregisterDeviceToken(@Body request: DeviceTokenRequestDto): Response<ApiResponse<Nothing>>

    @DELETE("api/devices")
    suspend fun unregisterAllDevices(): Response<ApiResponse<Nothing>>

    // ─── Notifications ──────────────────────────────────────────────

    @GET("api/notifications")
    suspend fun getNotifications(): Response<ApiResponse<List<NotificationDto>>>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<ApiResponse<Nothing>>

    @DELETE("api/notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): Response<ApiResponse<Nothing>>
}
