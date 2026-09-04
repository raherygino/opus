package com.gsoft.opus.data.api

import com.gsoft.opus.data.api.dto.ApiResponse
import com.gsoft.opus.data.api.dto.ArmementAttachmentDto
import com.gsoft.opus.data.api.dto.ArmementDto
import com.gsoft.opus.data.api.dto.ArmementRequest
import com.gsoft.opus.data.api.dto.ArmeDto
import com.gsoft.opus.data.api.dto.ArmeMunitionsConsommationDto
import com.gsoft.opus.data.api.dto.ArmeRequest
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.ConsommationRequest
import com.gsoft.opus.data.api.dto.TypeArmeDto
import com.gsoft.opus.data.api.dto.TypeArmeRequest
import com.gsoft.opus.data.api.dto.CodeSecretRequest
import com.gsoft.opus.data.api.dto.CodeSecretResultDto
import com.gsoft.opus.data.api.dto.ComportementDto
import com.gsoft.opus.data.api.dto.ComportementRejectRequest
import com.gsoft.opus.data.api.dto.ComportementRequest
import com.gsoft.opus.data.api.dto.CorrespondanceAttachmentDto
import com.gsoft.opus.data.api.dto.CorrespondanceDto
import com.gsoft.opus.data.api.dto.CorrespondanceRequest
import com.gsoft.opus.data.api.dto.DeclarationPerteAttachmentDto
import com.gsoft.opus.data.api.dto.DeclarationPerteDto
import com.gsoft.opus.data.api.dto.DeclarationPerteRequest
import com.gsoft.opus.data.api.dto.DeviceTokenRequestDto
import com.gsoft.opus.data.api.dto.DeviceTokenResponseDto
import com.gsoft.opus.data.api.dto.LoginRequestDto
import com.gsoft.opus.data.api.dto.LoginResponseDto
import com.gsoft.opus.data.api.dto.MouvementAttachmentDto
import com.gsoft.opus.data.api.dto.MouvementDto
import com.gsoft.opus.data.api.dto.MouvementRequest
import com.gsoft.opus.data.api.dto.MouvementRetourRequest
import com.gsoft.opus.data.api.dto.NotificationDto
import com.gsoft.opus.data.api.dto.UnreadCountDto
import com.gsoft.opus.data.api.dto.PassationAttachmentDto
import com.gsoft.opus.data.api.dto.PassationDto
import com.gsoft.opus.data.api.dto.PassationRequest
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
import com.gsoft.opus.data.api.dto.ReintegrationRequest
import com.gsoft.opus.data.api.dto.UserDto
import com.gsoft.opus.data.api.dto.VerifyCodeSecretRequest
import com.gsoft.opus.data.api.dto.VerifyIdentityRequest
import com.gsoft.opus.data.api.dto.VerifiedIdentityDto
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

    @POST("api/auth/verify")
    suspend fun verifyIdentity(@Body request: VerifyIdentityRequest): Response<ApiResponse<VerifiedIdentityDto>>

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

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<UnreadCountDto>>

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

    // ─── Personnel Code Secret (Armement identity verification) ──────

    @POST("api/personnel/{id}/code-secret")
    suspend fun setPersonnelCodeSecret(
        @Path("id") id: Int,
        @Body request: CodeSecretRequest
    ): Response<ApiResponse<CodeSecretResultDto>>

    @POST("api/personnel/{id}/verify-code-secret")
    suspend fun verifyPersonnelCodeSecret(
        @Path("id") id: Int,
        @Body request: VerifyCodeSecretRequest
    ): Response<ApiResponse<CodeSecretResultDto>>

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
    suspend fun getComportementList(
        @Query("personnel_id") personnelId: Int? = null,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<ComportementDto>>>

    @GET("api/comportements/{id}")
    suspend fun getComportement(@Path("id") id: Int): Response<ApiResponse<ComportementDto>>

    @POST("api/comportements")
    suspend fun createComportement(@Body request: ComportementRequest): Response<ApiResponse<ComportementDto>>

    @PUT("api/comportements/{id}/confirm")
    suspend fun confirmComportement(@Path("id") id: Int): Response<ApiResponse<ComportementDto>>

    @PUT("api/comportements/{id}/reject")
    suspend fun rejectComportement(
        @Path("id") id: Int,
        @Body request: ComportementRejectRequest
    ): Response<ApiResponse<ComportementDto>>

    @DELETE("api/comportements/{id}")
    suspend fun deleteComportement(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Correspondances ────────────────────────────────────────────

    @GET("api/correspondances")
    suspend fun getCorrespondanceList(
        @Query("sens") sens: String? = null,
        @Query("statut") statut: String? = null,
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<CorrespondanceDto>>>

    @GET("api/correspondances/{id}")
    suspend fun getCorrespondance(@Path("id") id: Int): Response<ApiResponse<CorrespondanceDto>>

    @POST("api/correspondances")
    suspend fun createCorrespondance(@Body request: CorrespondanceRequest): Response<ApiResponse<CorrespondanceDto>>

    @PUT("api/correspondances/{id}")
    suspend fun updateCorrespondance(@Path("id") id: Int, @Body request: CorrespondanceRequest): Response<ApiResponse<CorrespondanceDto>>

    @DELETE("api/correspondances/{id}")
    suspend fun deleteCorrespondance(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Correspondance Attachments ─────────────────────────────────

    @GET("api/correspondances/{id}/attachments")
    suspend fun getCorrespondanceAttachments(@Path("id") id: Int): Response<ApiResponse<List<CorrespondanceAttachmentDto>>>

    @Multipart
    @POST("api/correspondances/{id}/attachments")
    suspend fun createCorrespondanceAttachment(
        @Path("id") id: Int,
        @Part("title") title: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<CorrespondanceAttachmentDto>>

    @PUT("api/correspondances/{id}/attachments/{attachId}")
    suspend fun updateCorrespondanceAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<CorrespondanceAttachmentDto>>

    @DELETE("api/correspondances/{id}/attachments/{attachId}")
    suspend fun deleteCorrespondanceAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Déclarations de perte ──────────────────────────────────────

    @GET("api/declarations-perte")
    suspend fun getDeclarationPerteList(
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<DeclarationPerteDto>>>

    @GET("api/declarations-perte/{id}")
    suspend fun getDeclarationPerte(@Path("id") id: Int): Response<ApiResponse<DeclarationPerteDto>>

    @POST("api/declarations-perte")
    suspend fun createDeclarationPerte(@Body request: DeclarationPerteRequest): Response<ApiResponse<DeclarationPerteDto>>

    @PUT("api/declarations-perte/{id}")
    suspend fun updateDeclarationPerte(@Path("id") id: Int, @Body request: DeclarationPerteRequest): Response<ApiResponse<DeclarationPerteDto>>

    @DELETE("api/declarations-perte/{id}")
    suspend fun deleteDeclarationPerte(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Déclaration de perte Attachments ───────────────────────────

    @GET("api/declarations-perte/{id}/attachments")
    suspend fun getDeclarationPerteAttachments(@Path("id") id: Int): Response<ApiResponse<List<DeclarationPerteAttachmentDto>>>

    @Multipart
    @POST("api/declarations-perte/{id}/attachments")
    suspend fun createDeclarationPerteAttachment(
        @Path("id") id: Int,
        @Part("title") title: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<DeclarationPerteAttachmentDto>>

    @PUT("api/declarations-perte/{id}/attachments/{attachId}")
    suspend fun updateDeclarationPerteAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<DeclarationPerteAttachmentDto>>

    @DELETE("api/declarations-perte/{id}/attachments/{attachId}")
    suspend fun deleteDeclarationPerteAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Passations ─────────────────────────────────────────────────

    @GET("api/passations")
    suspend fun getPassationList(
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<PassationDto>>>

    @GET("api/passations/{id}")
    suspend fun getPassation(@Path("id") id: Int): Response<ApiResponse<PassationDto>>

    @POST("api/passations")
    suspend fun createPassation(@Body request: PassationRequest): Response<ApiResponse<PassationDto>>

    @PUT("api/passations/{id}")
    suspend fun updatePassation(@Path("id") id: Int, @Body request: PassationRequest): Response<ApiResponse<PassationDto>>

    @DELETE("api/passations/{id}")
    suspend fun deletePassation(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Passation Attachments ──────────────────────────────────────

    @GET("api/passations/{id}/attachments")
    suspend fun getPassationAttachments(@Path("id") id: Int): Response<ApiResponse<List<PassationAttachmentDto>>>

    @Multipart
    @POST("api/passations/{id}/attachments")
    suspend fun createPassationAttachment(
        @Path("id") id: Int,
        @Part("title") title: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<PassationAttachmentDto>>

    @PUT("api/passations/{id}/attachments/{attachId}")
    suspend fun updatePassationAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<PassationAttachmentDto>>

    @DELETE("api/passations/{id}/attachments/{attachId}")
    suspend fun deletePassationAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Armements ──────────────────────────────────────────────────

    @GET("api/armements")
    suspend fun getArmementList(
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("statut") statut: String? = null
    ): Response<ApiResponse<List<ArmementDto>>>

    @GET("api/armements/{id}")
    suspend fun getArmement(@Path("id") id: Int): Response<ApiResponse<ArmementDto>>

    @POST("api/armements")
    suspend fun createArmement(@Body request: ArmementRequest): Response<ApiResponse<ArmementDto>>

    @PUT("api/armements/{id}")
    suspend fun updateArmement(@Path("id") id: Int, @Body request: ArmementRequest): Response<ApiResponse<ArmementDto>>

    @POST("api/armements/{id}/reintegration")
    suspend fun reintegrateArmement(@Path("id") id: Int, @Body request: ReintegrationRequest): Response<ApiResponse<ArmementDto>>

    @DELETE("api/armements/{id}")
    suspend fun deleteArmement(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Armement Attachments ───────────────────────────────────────

    @GET("api/armements/{id}/attachments")
    suspend fun getArmementAttachments(@Path("id") id: Int): Response<ApiResponse<List<ArmementAttachmentDto>>>

    @Multipart
    @POST("api/armements/{id}/attachments")
    suspend fun createArmementAttachment(
        @Path("id") id: Int,
        @Part("title") title: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<ArmementAttachmentDto>>

    @PUT("api/armements/{id}/attachments/{attachId}")
    suspend fun updateArmementAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<ArmementAttachmentDto>>

    @DELETE("api/armements/{id}/attachments/{attachId}")
    suspend fun deleteArmementAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── TypeArme (weapon type catalog) ──────────────────────────────

    @GET("api/types-armes")
    suspend fun getTypeArmeList(@Query("search") search: String? = null): Response<ApiResponse<List<TypeArmeDto>>>

    @GET("api/types-armes/{id}")
    suspend fun getTypeArme(@Path("id") id: Int): Response<ApiResponse<TypeArmeDto>>

    @POST("api/types-armes")
    suspend fun createTypeArme(@Body request: TypeArmeRequest): Response<ApiResponse<TypeArmeDto>>

    @PUT("api/types-armes/{id}")
    suspend fun updateTypeArme(@Path("id") id: Int, @Body request: TypeArmeRequest): Response<ApiResponse<TypeArmeDto>>

    @DELETE("api/types-armes/{id}")
    suspend fun deleteTypeArme(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Arme (individual weapon instances + ammunition stock) ───────

    @GET("api/armes")
    suspend fun getArmeList(
        @Query("type_arme_id") typeArmeId: Int? = null,
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<ArmeDto>>>

    @GET("api/armes/{id}")
    suspend fun getArme(@Path("id") id: Int): Response<ApiResponse<ArmeDto>>

    @POST("api/armes")
    suspend fun createArme(@Body request: ArmeRequest): Response<ApiResponse<ArmeDto>>

    @PUT("api/armes/{id}")
    suspend fun updateArme(@Path("id") id: Int, @Body request: ArmeRequest): Response<ApiResponse<ArmeDto>>

    @DELETE("api/armes/{id}")
    suspend fun deleteArme(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    @GET("api/armes/{id}/consommations")
    suspend fun getArmeConsommations(@Path("id") id: Int): Response<ApiResponse<List<ArmeMunitionsConsommationDto>>>

    @POST("api/armes/{id}/consommation")
    suspend fun recordConsommation(
        @Path("id") id: Int,
        @Body request: ConsommationRequest
    ): Response<ApiResponse<ArmeDto>>
}
