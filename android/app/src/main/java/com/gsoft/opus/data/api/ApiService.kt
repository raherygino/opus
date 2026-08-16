package com.gsoft.opus.data.api

import com.gsoft.opus.data.api.dto.ApiResponse
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.ComportementDto
import com.gsoft.opus.data.api.dto.DeviceTokenRequestDto
import com.gsoft.opus.data.api.dto.DeviceTokenResponseDto
import com.gsoft.opus.data.api.dto.LoginRequestDto
import com.gsoft.opus.data.api.dto.LoginResponseDto
import com.gsoft.opus.data.api.dto.MouvementAttachmentDto
import com.gsoft.opus.data.api.dto.MouvementDto
import com.gsoft.opus.data.api.dto.MouvementRequest
import com.gsoft.opus.data.api.dto.MouvementRetourRequest
import com.gsoft.opus.data.api.dto.NotificationDto
import com.gsoft.opus.data.api.dto.PersonnelAttachmentDto
import com.gsoft.opus.data.api.dto.PersonnelDto
import com.gsoft.opus.data.api.dto.PersonnelRequest
import com.gsoft.opus.data.api.dto.QrAuthApproveResponseDto
import com.gsoft.opus.data.api.dto.QrAuthRequestDto
import com.gsoft.opus.data.api.dto.QrAuthRequestResponseDto
import com.gsoft.opus.data.api.dto.QrAuthScanResponseDto
import com.gsoft.opus.data.api.dto.QrAuthStatusResponseDto
import com.gsoft.opus.data.api.dto.RefreshResponseDto
import com.gsoft.opus.data.api.dto.RefreshTokenRequestDto
import com.gsoft.opus.data.api.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponse<LoginResponseDto>>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequestDto): Response<ApiResponse<RefreshResponseDto>>

    @GET("api/auth/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<ApiResponse<UserDto>>

    @GET("api/health")
    suspend fun healthCheck(): Response<ApiResponse<Nothing>>

    // ─── QR Auth (scan-to-log-in) ───────────────────────────────────

    @POST("api/qr-auth/request")
    suspend fun createQrAuthRequest(@Body request: QrAuthRequestDto): Response<ApiResponse<QrAuthRequestResponseDto>>

    @GET("api/qr-auth/{code}")
    suspend fun getQrAuthStatus(@Path("code") code: String): Response<ApiResponse<QrAuthStatusResponseDto>>

    @POST("api/qr-auth/{code}/scan")
    suspend fun scanQrAuth(@Path("code") code: String): Response<ApiResponse<QrAuthScanResponseDto>>

    @POST("api/qr-auth/{code}/approve")
    suspend fun approveQrAuth(@Path("code") code: String): Response<ApiResponse<QrAuthApproveResponseDto>>

    @POST("api/qr-auth/{code}/reject")
    suspend fun rejectQrAuth(@Path("code") code: String): Response<ApiResponse<Nothing>>

    @POST("api/qr-auth/{code}/cancel")
    suspend fun cancelQrAuth(@Path("code") code: String): Response<ApiResponse<Nothing>>

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

    // ─── Personnel ──────────────────────────────────────────────────

    @GET("api/personnel")
    suspend fun getPersonnelList(@Query("search") search: String? = null): Response<ApiResponse<List<PersonnelDto>>>

    @GET("api/personnel/{id}")
    suspend fun getPersonnel(@Path("id") id: Int): Response<ApiResponse<PersonnelDto>>

    @POST("api/personnel")
    suspend fun createPersonnel(@Body request: PersonnelRequest): Response<ApiResponse<PersonnelDto>>

    @PUT("api/personnel/{id}")
    suspend fun updatePersonnel(@Path("id") id: Int, @Body request: PersonnelRequest): Response<ApiResponse<PersonnelDto>>

    @DELETE("api/personnel/{id}")
    suspend fun deletePersonnel(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Personnel Attachments ──────────────────────────────────────

    @GET("api/personnel/{id}/attachments")
    suspend fun getPersonnelAttachments(@Path("id") id: Int): Response<ApiResponse<List<PersonnelAttachmentDto>>>

    @Multipart
    @POST("api/personnel/{id}/attachments")
    suspend fun createPersonnelAttachment(
        @Path("id") id: Int,
        @Part("title") title: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<PersonnelAttachmentDto>>

    @PUT("api/personnel/{id}/attachments/{attachId}")
    suspend fun updatePersonnelAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<PersonnelAttachmentDto>>

    @DELETE("api/personnel/{id}/attachments/{attachId}")
    suspend fun deletePersonnelAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Personnel Photo ────────────────────────────────────────────

    @Multipart
    @POST("api/personnel/{id}/photo")
    suspend fun uploadPersonnelPhoto(
        @Path("id") id: Int,
        @Part photo: MultipartBody.Part,
        @Part thumbnail: MultipartBody.Part? = null
    ): Response<ApiResponse<PersonnelDto>>

    @DELETE("api/personnel/{id}/photo")
    suspend fun deletePersonnelPhoto(@Path("id") id: Int): Response<ApiResponse<PersonnelDto>>

    // ─── Mouvements ─────────────────────────────────────────────────

    @GET("api/mouvements")
    suspend fun getMouvementList(
        @Query("personnel_id") personnelId: Int? = null,
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<MouvementDto>>>

    @POST("api/mouvements")
    suspend fun createMouvement(@Body request: MouvementRequest): Response<ApiResponse<MouvementDto>>

    @PUT("api/mouvements/{id}")
    suspend fun updateMouvement(@Path("id") id: Int, @Body request: MouvementRetourRequest): Response<ApiResponse<MouvementDto>>

    @DELETE("api/mouvements/{id}")
    suspend fun deleteMouvement(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Mouvement Attachments ──────────────────────────────────────

    @GET("api/mouvements/{id}/attachments")
    suspend fun getMouvementAttachments(@Path("id") id: Int): Response<ApiResponse<List<MouvementAttachmentDto>>>

    @Multipart
    @POST("api/mouvements/{id}/attachments")
    suspend fun createMouvementAttachment(
        @Path("id") id: Int,
        @Part("title") title: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<MouvementAttachmentDto>>

    @DELETE("api/mouvements/{id}/attachments/{attachId}")
    suspend fun deleteMouvementAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Comportements ──────────────────────────────────────────────

    @GET("api/comportements")
    suspend fun getComportementList(@Query("personnel_id") personnelId: Int? = null): Response<ApiResponse<List<ComportementDto>>>
}
