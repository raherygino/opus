package com.gsoft.opus.data.api

import com.gsoft.opus.data.api.dto.ApiResponse
import com.gsoft.opus.data.api.dto.ArmementAttachmentDto
import com.gsoft.opus.data.api.dto.ArmementDto
import com.gsoft.opus.data.api.dto.ArmementRequest
import com.gsoft.opus.data.api.dto.ArmeDto
import com.gsoft.opus.data.api.dto.ArmeMunitionsConsommationDto
import com.gsoft.opus.data.api.dto.ArmeRequest
import com.gsoft.opus.data.api.dto.AttachmentTitleRequest
import com.gsoft.opus.data.api.dto.AffectationMaterielDto
import com.gsoft.opus.data.api.dto.AffectationMaterielRequest
import com.gsoft.opus.data.api.dto.ReintegrationMaterielRequest
import com.gsoft.opus.data.api.dto.TypeMaterielDto
import com.gsoft.opus.data.api.dto.TypeMaterielRequest
import com.gsoft.opus.data.api.dto.MaterielRoulantDto
import com.gsoft.opus.data.api.dto.MaterielRoulantAttachmentDto
import com.gsoft.opus.data.api.dto.MaterielRoulantRequest
import com.gsoft.opus.data.api.dto.ReintegrationMaterielRoulantRequest
import com.gsoft.opus.data.api.dto.MainCouranteDto
import com.gsoft.opus.data.api.dto.MainCouranteAttachmentDto
import com.gsoft.opus.data.api.dto.MainCouranteCategorieDto
import com.gsoft.opus.data.api.dto.MainCouranteCategorieRequest
import com.gsoft.opus.data.api.dto.MainCouranteRequest
import com.gsoft.opus.data.api.dto.RassemblementJournalierDto
import com.gsoft.opus.data.api.dto.RassemblementJournalierRequest
import com.gsoft.opus.data.api.dto.EvenementSurvenuAttachmentDto
import com.gsoft.opus.data.api.dto.EvenementSurvenuDto
import com.gsoft.opus.data.api.dto.EvenementSurvenuRequest
import com.gsoft.opus.data.api.dto.EvenementSurvenuTypeDto
import com.gsoft.opus.data.api.dto.EvenementSurvenuTypeRequest
import com.gsoft.opus.data.api.dto.ActiviteAttachmentDto
import com.gsoft.opus.data.api.dto.ActiviteDto
import com.gsoft.opus.data.api.dto.ActiviteRequest
import com.gsoft.opus.data.api.dto.DispositifExceptionnelDto
import com.gsoft.opus.data.api.dto.DispositifExceptionnelRequest
import com.gsoft.opus.data.api.dto.PlainteEntreeDto
import com.gsoft.opus.data.api.dto.PlainteEntreeAttachmentDto
import com.gsoft.opus.data.api.dto.PlainteEntreeSummaryDto
import com.gsoft.opus.data.api.dto.PlainteEntreeRequest
import com.gsoft.opus.data.api.dto.PlainteNextNumberDto
import com.gsoft.opus.data.api.dto.PlainteSortieDto
import com.gsoft.opus.data.api.dto.PlainteSortieAttachmentDto
import com.gsoft.opus.data.api.dto.PlainteSortieRequest
import com.gsoft.opus.data.api.dto.ConvocationDto
import com.gsoft.opus.data.api.dto.ConvocationRequest
import com.gsoft.opus.data.api.dto.ConvocationAttachmentDto
import com.gsoft.opus.data.api.dto.GardeAVueDto
import com.gsoft.opus.data.api.dto.GardeAVueRequest
import com.gsoft.opus.data.api.dto.GardeAVueAttachmentDto
import com.gsoft.opus.data.api.dto.RequisitionDto
import com.gsoft.opus.data.api.dto.RequisitionRequest
import com.gsoft.opus.data.api.dto.RequisitionAttachmentDto
import com.gsoft.opus.data.api.dto.RequisitionNextNumberDto
import com.gsoft.opus.data.api.dto.PersonneRechercheeDto
import com.gsoft.opus.data.api.dto.PersonneRechercheeRequest
import com.gsoft.opus.data.api.dto.PersonneRechercheePhotoDto
import com.gsoft.opus.data.api.dto.PersonneRechercheePhotoCaptionRequest
import com.gsoft.opus.data.api.dto.ObjetSaisiDto
import com.gsoft.opus.data.api.dto.ObjetSaisiRequest
import com.gsoft.opus.data.api.dto.ObjetSaisiAttachmentDto
import com.gsoft.opus.data.api.dto.ObjetTrouveDto
import com.gsoft.opus.data.api.dto.ObjetTrouveRequest
import com.gsoft.opus.data.api.dto.PerquisitionAttachmentDto
import com.gsoft.opus.data.api.dto.PerquisitionDto
import com.gsoft.opus.data.api.dto.PerquisitionNextNumberDto
import com.gsoft.opus.data.api.dto.PerquisitionRequest
import com.gsoft.opus.data.api.dto.RenseignementPjAttachmentDto
import com.gsoft.opus.data.api.dto.RenseignementPjDto
import com.gsoft.opus.data.api.dto.RenseignementPjRequest
import com.gsoft.opus.data.api.dto.MandatAttachmentDto
import com.gsoft.opus.data.api.dto.MandatDto
import com.gsoft.opus.data.api.dto.MandatNextNumberDto
import com.gsoft.opus.data.api.dto.MandatRequest
import com.gsoft.opus.data.api.dto.ArrestationAttachmentDto
import com.gsoft.opus.data.api.dto.ArrestationDto
import com.gsoft.opus.data.api.dto.ArrestationNextNumberDto
import com.gsoft.opus.data.api.dto.ArrestationRequest
import com.gsoft.opus.data.api.dto.ObjetTrouveAttachmentDto
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
import com.gsoft.opus.data.api.dto.DashboardStatsDto
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
import com.gsoft.opus.data.api.dto.PersonnelCountDto
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
import okhttp3.RequestBody
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

    // ─── Dashboard ──────────────────────────────────────────────────

    @GET("api/dashboard/stats")
    suspend fun getDashboardStats(): Response<ApiResponse<DashboardStatsDto>>

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

    @GET("api/personnel/count")
    suspend fun getPersonnelCount(): Response<ApiResponse<PersonnelCountDto>>

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

    // ─── TypeMateriel (equipment type catalog) ──────────────────────

    @GET("api/types-materiels")
    suspend fun getTypeMaterielList(@Query("search") search: String? = null): Response<ApiResponse<List<TypeMaterielDto>>>

    @GET("api/types-materiels/{id}")
    suspend fun getTypeMateriel(@Path("id") id: Int): Response<ApiResponse<TypeMaterielDto>>

    @POST("api/types-materiels")
    suspend fun createTypeMateriel(@Body request: TypeMaterielRequest): Response<ApiResponse<TypeMaterielDto>>

    @PUT("api/types-materiels/{id}")
    suspend fun updateTypeMateriel(@Path("id") id: Int, @Body request: TypeMaterielRequest): Response<ApiResponse<TypeMaterielDto>>

    @DELETE("api/types-materiels/{id}")
    suspend fun deleteTypeMateriel(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── AffectationMateriel (equipment assignment & return) ────────

    @GET("api/affectations-materiels")
    suspend fun getAffectationMaterielList(
        @Query("search") search: String? = null,
        @Query("statut") statut: String? = null,
        @Query("agent_personnel_id") agentPersonnelId: Int? = null
    ): Response<ApiResponse<List<AffectationMaterielDto>>>

    @GET("api/affectations-materiels/{id}")
    suspend fun getAffectationMateriel(@Path("id") id: Int): Response<ApiResponse<AffectationMaterielDto>>

    @POST("api/affectations-materiels")
    suspend fun createAffectationMateriel(@Body request: AffectationMaterielRequest): Response<ApiResponse<AffectationMaterielDto>>

    @PUT("api/affectations-materiels/{id}")
    suspend fun updateAffectationMateriel(@Path("id") id: Int, @Body request: AffectationMaterielRequest): Response<ApiResponse<AffectationMaterielDto>>

    @POST("api/affectations-materiels/{id}/reintegration")
    suspend fun reintegrateAffectationMateriel(@Path("id") id: Int, @Body request: ReintegrationMaterielRequest): Response<ApiResponse<AffectationMaterielDto>>

    @DELETE("api/affectations-materiels/{id}")
    suspend fun deleteAffectationMateriel(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── MaterielRoulant (vehicle perception & reintegration — VHL / Moto) ───

    @GET("api/materiels-roulants")
    suspend fun getMaterielRoulantList(
        @Query("search") search: String? = null,
        @Query("statut") statut: String? = null,
        @Query("type_materiel") typeMateriel: String? = null
    ): Response<ApiResponse<List<MaterielRoulantDto>>>

    @GET("api/materiels-roulants/{id}")
    suspend fun getMaterielRoulant(@Path("id") id: Int): Response<ApiResponse<MaterielRoulantDto>>

    @POST("api/materiels-roulants")
    suspend fun createMaterielRoulant(@Body request: MaterielRoulantRequest): Response<ApiResponse<MaterielRoulantDto>>

    @PUT("api/materiels-roulants/{id}")
    suspend fun updateMaterielRoulant(@Path("id") id: Int, @Body request: MaterielRoulantRequest): Response<ApiResponse<MaterielRoulantDto>>

    @POST("api/materiels-roulants/{id}/reintegration")
    suspend fun reintegrateMaterielRoulant(@Path("id") id: Int, @Body request: ReintegrationMaterielRoulantRequest): Response<ApiResponse<MaterielRoulantDto>>

    @DELETE("api/materiels-roulants/{id}")
    suspend fun deleteMaterielRoulant(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── MaterielRoulant Attachments ────────────────────────────────

    @GET("api/materiels-roulants/{id}/attachments")
    suspend fun getMaterielRoulantAttachments(@Path("id") id: Int): Response<ApiResponse<List<MaterielRoulantAttachmentDto>>>

    @POST("api/materiels-roulants/{id}/attachments")
    suspend fun createMaterielRoulantAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<MaterielRoulantAttachmentDto>>

    @PUT("api/materiels-roulants/{id}/attachments/{attachId}")
    suspend fun updateMaterielRoulantAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<MaterielRoulantAttachmentDto>>

    @DELETE("api/materiels-roulants/{id}/attachments/{attachId}")
    suspend fun deleteMaterielRoulantAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Main courante (event logbook — Sédentaire > Secrétariat & Poste) ───

    @GET("api/main-courante")
    suspend fun getMainCouranteList(
        @Query("origine") origine: String? = null,
        @Query("categorie") categorie: String? = null,
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<MainCouranteDto>>>

    @GET("api/main-courante/{id}")
    suspend fun getMainCourante(@Path("id") id: Int): Response<ApiResponse<MainCouranteDto>>

    @POST("api/main-courante")
    suspend fun createMainCourante(@Body request: MainCouranteRequest): Response<ApiResponse<MainCouranteDto>>

    @PUT("api/main-courante/{id}")
    suspend fun updateMainCourante(@Path("id") id: Int, @Body request: MainCouranteRequest): Response<ApiResponse<MainCouranteDto>>

    @DELETE("api/main-courante/{id}")
    suspend fun deleteMainCourante(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Main courante Attachments ───────────────────────────────────

    @GET("api/main-courante/{id}/attachments")
    suspend fun getMainCouranteAttachments(@Path("id") id: Int): Response<ApiResponse<List<MainCouranteAttachmentDto>>>

    @Multipart
    @POST("api/main-courante/{id}/attachments")
    suspend fun createMainCouranteAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<MainCouranteAttachmentDto>>

    @PUT("api/main-courante/{id}/attachments/{attachId}")
    suspend fun updateMainCouranteAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<MainCouranteAttachmentDto>>

    @DELETE("api/main-courante/{id}/attachments/{attachId}")
    suspend fun deleteMainCouranteAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Rassemblement journalier (Service Général) ──────────────────

    @GET("api/rassemblements")
    suspend fun getRassemblementList(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<RassemblementJournalierDto>>>

    @GET("api/rassemblements/{id}")
    suspend fun getRassemblement(@Path("id") id: Int): Response<ApiResponse<RassemblementJournalierDto>>

    @POST("api/rassemblements")
    suspend fun createRassemblement(@Body request: RassemblementJournalierRequest): Response<ApiResponse<RassemblementJournalierDto>>

    @PUT("api/rassemblements/{id}")
    suspend fun updateRassemblement(@Path("id") id: Int, @Body request: RassemblementJournalierRequest): Response<ApiResponse<RassemblementJournalierDto>>

    @DELETE("api/rassemblements/{id}")
    suspend fun deleteRassemblement(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Évènements survenus (Service Général) ────────────────────

    @GET("api/evenements-survenus")
    suspend fun getEvenementSurvenuList(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<EvenementSurvenuDto>>>

    @GET("api/evenements-survenus/{id}")
    suspend fun getEvenementSurvenu(@Path("id") id: Int): Response<ApiResponse<EvenementSurvenuDto>>

    @POST("api/evenements-survenus")
    suspend fun createEvenementSurvenu(@Body request: EvenementSurvenuRequest): Response<ApiResponse<EvenementSurvenuDto>>

    @PUT("api/evenements-survenus/{id}")
    suspend fun updateEvenementSurvenu(@Path("id") id: Int, @Body request: EvenementSurvenuRequest): Response<ApiResponse<EvenementSurvenuDto>>

    @DELETE("api/evenements-survenus/{id}")
    suspend fun deleteEvenementSurvenu(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Évènements survenus Attachments ──────────────────────────

    @GET("api/evenements-survenus/{id}/attachments")
    suspend fun getEvenementSurvenuAttachments(@Path("id") id: Int): Response<ApiResponse<List<EvenementSurvenuAttachmentDto>>>

    @Multipart
    @POST("api/evenements-survenus/{id}/attachments")
    suspend fun createEvenementSurvenuAttachment(
        @Path("id") id: Int,
        @Part("title") title: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<EvenementSurvenuAttachmentDto>>

    @PUT("api/evenements-survenus/{id}/attachments/{attachId}")
    suspend fun updateEvenementSurvenuAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<EvenementSurvenuAttachmentDto>>

    @DELETE("api/evenements-survenus/{id}/attachments/{attachId}")
    suspend fun deleteEvenementSurvenuAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Évènements survenus — type catalog (user-managed labels) ────

    @GET("api/evenement-survenu-types")
    suspend fun getEvenementSurvenuTypes(): Response<ApiResponse<List<EvenementSurvenuTypeDto>>>

    @POST("api/evenement-survenu-types")
    suspend fun createEvenementSurvenuType(
        @Body request: EvenementSurvenuTypeRequest
    ): Response<ApiResponse<EvenementSurvenuTypeDto>>

    @PUT("api/evenement-survenu-types/{id}")
    suspend fun updateEvenementSurvenuType(
        @Path("id") id: Int,
        @Body request: EvenementSurvenuTypeRequest
    ): Response<ApiResponse<EvenementSurvenuTypeDto>>

    @DELETE("api/evenement-survenu-types/{id}")
    suspend fun deleteEvenementSurvenuType(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Activités (Service Général) — patrouilles et interventions ──

    @GET("api/activites")
    suspend fun getActiviteList(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<ActiviteDto>>>

    @GET("api/activites/{id}")
    suspend fun getActivite(@Path("id") id: Int): Response<ApiResponse<ActiviteDto>>

    @POST("api/activites")
    suspend fun createActivite(@Body request: ActiviteRequest): Response<ApiResponse<ActiviteDto>>

    @PUT("api/activites/{id}")
    suspend fun updateActivite(@Path("id") id: Int, @Body request: ActiviteRequest): Response<ApiResponse<ActiviteDto>>

    @DELETE("api/activites/{id}")
    suspend fun deleteActivite(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Activités Attachments ──────────────────────────────────────

    @GET("api/activites/{id}/attachments")
    suspend fun getActiviteAttachments(@Path("id") id: Int): Response<ApiResponse<List<ActiviteAttachmentDto>>>

    @Multipart
    @POST("api/activites/{id}/attachments")
    suspend fun createActiviteAttachment(
        @Path("id") id: Int,
        @Part("title") title: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<ActiviteAttachmentDto>>

    @PUT("api/activites/{id}/attachments/{attachId}")
    suspend fun updateActiviteAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<ActiviteAttachmentDto>>

    @DELETE("api/activites/{id}/attachments/{attachId}")
    suspend fun deleteActiviteAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Dispositif exceptionnel (Service Général) ──────────────────

    @GET("api/dispositifs-exceptionnels")
    suspend fun getDispositifExceptionnelList(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<DispositifExceptionnelDto>>>

    @GET("api/dispositifs-exceptionnels/{id}")
    suspend fun getDispositifExceptionnel(@Path("id") id: Int): Response<ApiResponse<DispositifExceptionnelDto>>

    @POST("api/dispositifs-exceptionnels")
    suspend fun createDispositifExceptionnel(@Body request: DispositifExceptionnelRequest): Response<ApiResponse<DispositifExceptionnelDto>>

    @PUT("api/dispositifs-exceptionnels/{id}")
    suspend fun updateDispositifExceptionnel(@Path("id") id: Int, @Body request: DispositifExceptionnelRequest): Response<ApiResponse<DispositifExceptionnelDto>>

    @DELETE("api/dispositifs-exceptionnels/{id}")
    suspend fun deleteDispositifExceptionnel(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ========================
    // Main courante Categories (user-managed label catalog)
    // ========================

    @GET("api/main-courante-categories")
    suspend fun getMainCouranteCategories(): Response<ApiResponse<List<MainCouranteCategorieDto>>>

    @POST("api/main-courante-categories")
    suspend fun createMainCouranteCategorie(
        @Body request: MainCouranteCategorieRequest
    ): Response<ApiResponse<MainCouranteCategorieDto>>

    @PUT("api/main-courante-categories/{id}")
    suspend fun updateMainCouranteCategorie(
        @Path("id") id: Int,
        @Body request: MainCouranteCategorieRequest
    ): Response<ApiResponse<MainCouranteCategorieDto>>

    @DELETE("api/main-courante-categories/{id}")
    suspend fun deleteMainCouranteCategorie(
        @Path("id") id: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Plainte ENTRÉE (Police Judiciaire — incoming complaints) ───

    @GET("api/plaintes-entree")
    suspend fun getPlainteEntreeList(
        @Query("type") type: String? = null,
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<PlainteEntreeDto>>>

    @GET("api/plaintes-entree/without-sortie")
    suspend fun getPlaintesEntreeWithoutSortie(): Response<ApiResponse<List<PlainteEntreeSummaryDto>>>

    @GET("api/plaintes-entree/next-number")
    suspend fun getPlainteEntreeNextNumber(@Query("type") type: String): Response<ApiResponse<PlainteNextNumberDto>>

    @GET("api/plaintes-entree/{id}")
    suspend fun getPlainteEntree(@Path("id") id: Int): Response<ApiResponse<PlainteEntreeDto>>

    @POST("api/plaintes-entree")
    suspend fun createPlainteEntree(@Body request: PlainteEntreeRequest): Response<ApiResponse<PlainteEntreeDto>>

    @PUT("api/plaintes-entree/{id}")
    suspend fun updatePlainteEntree(@Path("id") id: Int, @Body request: PlainteEntreeRequest): Response<ApiResponse<PlainteEntreeDto>>

    @DELETE("api/plaintes-entree/{id}")
    suspend fun deletePlainteEntree(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Plainte ENTRÉE Attachments ──────────────────────────────────

    @GET("api/plaintes-entree/{id}/attachments")
    suspend fun getPlainteEntreeAttachments(@Path("id") id: Int): Response<ApiResponse<List<PlainteEntreeAttachmentDto>>>

    @Multipart
    @POST("api/plaintes-entree/{id}/attachments")
    suspend fun createPlainteEntreeAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<PlainteEntreeAttachmentDto>>

    @PUT("api/plaintes-entree/{id}/attachments/{attachId}")
    suspend fun updatePlainteEntreeAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<PlainteEntreeAttachmentDto>>

    @DELETE("api/plaintes-entree/{id}/attachments/{attachId}")
    suspend fun deletePlainteEntreeAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ─── Plainte SORTIE (Police Judiciaire — outgoing processing) ───

    @GET("api/plaintes-sortie")
    suspend fun getPlainteSortieList(
        @Query("nature") nature: String? = null,
        @Query("entree_id") entreeId: Int? = null,
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<PlainteSortieDto>>>

    @GET("api/plaintes-sortie/next-number")
    suspend fun getPlainteSortieNextNumber(): Response<ApiResponse<PlainteNextNumberDto>>

    @GET("api/plaintes-sortie/{id}")
    suspend fun getPlainteSortie(@Path("id") id: Int): Response<ApiResponse<PlainteSortieDto>>

    @POST("api/plaintes-sortie")
    suspend fun createPlainteSortie(@Body request: PlainteSortieRequest): Response<ApiResponse<PlainteSortieDto>>

    @PUT("api/plaintes-sortie/{id}")
    suspend fun updatePlainteSortie(@Path("id") id: Int, @Body request: PlainteSortieRequest): Response<ApiResponse<PlainteSortieDto>>

    @DELETE("api/plaintes-sortie/{id}")
    suspend fun deletePlainteSortie(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // ─── Plainte SORTIE Attachments ──────────────────────────────────

    @GET("api/plaintes-sortie/{id}/attachments")
    suspend fun getPlainteSortieAttachments(@Path("id") id: Int): Response<ApiResponse<List<PlainteSortieAttachmentDto>>>

    @Multipart
    @POST("api/plaintes-sortie/{id}/attachments")
    suspend fun createPlainteSortieAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<PlainteSortieAttachmentDto>>

    @PUT("api/plaintes-sortie/{id}/attachments/{attachId}")
    suspend fun updatePlainteSortieAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body request: AttachmentTitleRequest
    ): Response<ApiResponse<PlainteSortieAttachmentDto>>

    @DELETE("api/plaintes-sortie/{id}/attachments/{attachId}")
    suspend fun deletePlainteSortieAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ========================
    // Convocation (Police Judiciaire — Convocation)
    // ========================
    @GET("api/convocations")
    suspend fun getConvocationList(
        @Query("type") type: String? = null,
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<ConvocationDto>>>

    @GET("api/convocations/next-number")
    suspend fun getConvocationNextNumber(@Query("type") type: String): Response<ApiResponse<PlainteNextNumberDto>>

    @GET("api/convocations/{id}")
    suspend fun getConvocation(@Path("id") id: Int): Response<ApiResponse<ConvocationDto>>

    @POST("api/convocations")
    suspend fun createConvocation(@Body request: ConvocationRequest): Response<ApiResponse<ConvocationDto>>

    @PUT("api/convocations/{id}")
    suspend fun updateConvocation(@Path("id") id: Int, @Body request: ConvocationRequest): Response<ApiResponse<ConvocationDto>>

    @DELETE("api/convocations/{id}")
    suspend fun deleteConvocation(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    @GET("api/convocations/{id}/attachments")
    suspend fun getConvocationAttachments(@Path("id") id: Int): Response<ApiResponse<List<ConvocationAttachmentDto>>>

    @Multipart
    @POST("api/convocations/{id}/attachments")
    suspend fun createConvocationAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<ConvocationAttachmentDto>>

    @PUT("api/convocations/{id}/attachments/{attachId}")
    suspend fun updateConvocationAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body body: AttachmentTitleRequest
    ): Response<ApiResponse<ConvocationAttachmentDto>>

    @DELETE("api/convocations/{id}/attachments/{attachId}")
    suspend fun deleteConvocationAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ========================
    // Garde à Vue (Police Judiciaire — GAV)
    // ========================
    @GET("api/garde-a-vue")
    suspend fun getGardeAVueList(
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<GardeAVueDto>>>

    @GET("api/garde-a-vue/{id}")
    suspend fun getGardeAVue(@Path("id") id: Int): Response<ApiResponse<GardeAVueDto>>

    @POST("api/garde-a-vue")
    suspend fun createGardeAVue(@Body request: GardeAVueRequest): Response<ApiResponse<GardeAVueDto>>

    @PUT("api/garde-a-vue/{id}")
    suspend fun updateGardeAVue(@Path("id") id: Int, @Body request: GardeAVueRequest): Response<ApiResponse<GardeAVueDto>>

    @DELETE("api/garde-a-vue/{id}")
    suspend fun deleteGardeAVue(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    @GET("api/garde-a-vue/{id}/attachments")
    suspend fun getGardeAVueAttachments(@Path("id") id: Int): Response<ApiResponse<List<GardeAVueAttachmentDto>>>

    @Multipart
    @POST("api/garde-a-vue/{id}/attachments")
    suspend fun createGardeAVueAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<GardeAVueAttachmentDto>>

    @PUT("api/garde-a-vue/{id}/attachments/{attachId}")
    suspend fun updateGardeAVueAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body body: AttachmentTitleRequest
    ): Response<ApiResponse<GardeAVueAttachmentDto>>

    @DELETE("api/garde-a-vue/{id}/attachments/{attachId}")
    suspend fun deleteGardeAVueAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ========================
    // Requisition (Police Judiciaire)
    // ========================
    @GET("api/requisitions")
    suspend fun getRequisitionList(
        @Query("type") type: String? = null,
        @Query("search") search: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<RequisitionDto>>>

    @GET("api/requisitions/next-number")
    suspend fun getRequisitionNextNumber(): Response<ApiResponse<RequisitionNextNumberDto>>

    @GET("api/requisitions/{id}")
    suspend fun getRequisition(@Path("id") id: Int): Response<ApiResponse<RequisitionDto>>

    @POST("api/requisitions")
    suspend fun createRequisition(@Body request: RequisitionRequest): Response<ApiResponse<RequisitionDto>>

    @PUT("api/requisitions/{id}")
    suspend fun updateRequisition(@Path("id") id: Int, @Body request: RequisitionRequest): Response<ApiResponse<RequisitionDto>>

    @DELETE("api/requisitions/{id}")
    suspend fun deleteRequisition(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    @GET("api/requisitions/{id}/attachments")
    suspend fun getRequisitionAttachments(@Path("id") id: Int): Response<ApiResponse<List<RequisitionAttachmentDto>>>

    @Multipart
    @POST("api/requisitions/{id}/attachments")
    suspend fun createRequisitionAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<RequisitionAttachmentDto>>

    @PUT("api/requisitions/{id}/attachments/{attachId}")
    suspend fun updateRequisitionAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body body: AttachmentTitleRequest
    ): Response<ApiResponse<RequisitionAttachmentDto>>

    @DELETE("api/requisitions/{id}/attachments/{attachId}")
    suspend fun deleteRequisitionAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ========================
    // Personne Recherchée (Police Judiciaire)
    // ========================
    @GET("api/personne-recherchee")
    suspend fun getPersonneRechercheeList(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<PersonneRechercheeDto>>>

    @GET("api/personne-recherchee/{id}")
    suspend fun getPersonneRecherchee(@Path("id") id: Int): Response<ApiResponse<PersonneRechercheeDto>>

    @POST("api/personne-recherchee")
    suspend fun createPersonneRecherchee(@Body request: PersonneRechercheeRequest): Response<ApiResponse<PersonneRechercheeDto>>

    @PUT("api/personne-recherchee/{id}")
    suspend fun updatePersonneRecherchee(@Path("id") id: Int, @Body request: PersonneRechercheeRequest): Response<ApiResponse<PersonneRechercheeDto>>

    @DELETE("api/personne-recherchee/{id}")
    suspend fun deletePersonneRecherchee(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    // Dedicated multi-image endpoints (NOT the generic attachment system)
    @GET("api/personne-recherchee/{id}/photos")
    suspend fun getPersonneRechercheePhotos(@Path("id") id: Int): Response<ApiResponse<List<PersonneRechercheePhotoDto>>>

    @Multipart
    @POST("api/personne-recherchee/{id}/photos")
    suspend fun createPersonneRechercheePhoto(
        @Path("id") id: Int,
        @Part("caption") caption: RequestBody,
        @Part("capture_source") captureSource: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<PersonneRechercheePhotoDto>>

    @PUT("api/personne-recherchee/{id}/photos/{photoId}")
    suspend fun updatePersonneRechercheePhotoCaption(
        @Path("id") id: Int,
        @Path("photoId") photoId: Int,
        @Body body: PersonneRechercheePhotoCaptionRequest
    ): Response<ApiResponse<PersonneRechercheePhotoDto>>

    @DELETE("api/personne-recherchee/{id}/photos/{photoId}")
    suspend fun deletePersonneRechercheePhoto(
        @Path("id") id: Int,
        @Path("photoId") photoId: Int
    ): Response<ApiResponse<Nothing>>

    // ========================
    // Objet Saisi (Police Judiciaire — OBJET SAISI tab)
    // ========================
    @GET("api/objets/saisi")
    suspend fun getObjetSaisiList(
        @Query("type_objet") typeObjet: String? = null,
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<ObjetSaisiDto>>>

    @GET("api/objets/saisi/{id}")
    suspend fun getObjetSaisi(@Path("id") id: Int): Response<ApiResponse<ObjetSaisiDto>>

    @POST("api/objets/saisi")
    suspend fun createObjetSaisi(@Body request: ObjetSaisiRequest): Response<ApiResponse<ObjetSaisiDto>>

    @PUT("api/objets/saisi/{id}")
    suspend fun updateObjetSaisi(@Path("id") id: Int, @Body request: ObjetSaisiRequest): Response<ApiResponse<ObjetSaisiDto>>

    @DELETE("api/objets/saisi/{id}")
    suspend fun deleteObjetSaisi(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    @GET("api/objets/saisi/{id}/attachments")
    suspend fun getObjetSaisiAttachments(@Path("id") id: Int): Response<ApiResponse<List<ObjetSaisiAttachmentDto>>>

    @Multipart
    @POST("api/objets/saisi/{id}/attachments")
    suspend fun createObjetSaisiAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<ObjetSaisiAttachmentDto>>

    @PUT("api/objets/saisi/{id}/attachments/{attachId}")
    suspend fun updateObjetSaisiAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body body: AttachmentTitleRequest
    ): Response<ApiResponse<ObjetSaisiAttachmentDto>>

    @DELETE("api/objets/saisi/{id}/attachments/{attachId}")
    suspend fun deleteObjetSaisiAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ========================
    // Objet Trouvé (Police Judiciaire — OBJET TROUVÉ tab)
    // ========================
    @GET("api/objets/trouve")
    suspend fun getObjetTrouveList(
        @Query("motif_decouverte") motifDecouverte: String? = null,
        @Query("restitution") restitution: Boolean? = null,
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<ObjetTrouveDto>>>

    @GET("api/objets/trouve/{id}")
    suspend fun getObjetTrouve(@Path("id") id: Int): Response<ApiResponse<ObjetTrouveDto>>

    @POST("api/objets/trouve")
    suspend fun createObjetTrouve(@Body request: ObjetTrouveRequest): Response<ApiResponse<ObjetTrouveDto>>

    @PUT("api/objets/trouve/{id}")
    suspend fun updateObjetTrouve(@Path("id") id: Int, @Body request: ObjetTrouveRequest): Response<ApiResponse<ObjetTrouveDto>>

    @DELETE("api/objets/trouve/{id}")
    suspend fun deleteObjetTrouve(@Path("id") id: Int): Response<ApiResponse<Nothing>>

    @GET("api/objets/trouve/{id}/attachments")
    suspend fun getObjetTrouveAttachments(@Path("id") id: Int): Response<ApiResponse<List<ObjetTrouveAttachmentDto>>>

    @Multipart
    @POST("api/objets/trouve/{id}/attachments")
    suspend fun createObjetTrouveAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<ObjetTrouveAttachmentDto>>

    @PUT("api/objets/trouve/{id}/attachments/{attachId}")
    suspend fun updateObjetTrouveAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body body: AttachmentTitleRequest
    ): Response<ApiResponse<ObjetTrouveAttachmentDto>>

    @DELETE("api/objets/trouve/{id}/attachments/{attachId}")
    suspend fun deleteObjetTrouveAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Nothing>>

    // ========================
    // Perquisition (Police Judiciaire)
    // ========================

    @GET("api/perquisitions")
    suspend fun getPerquisitionList(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<PerquisitionDto>>>

    @GET("api/perquisitions/next-number")
    suspend fun getPerquisitionNextNumber(): Response<ApiResponse<PerquisitionNextNumberDto>>

    @GET("api/perquisitions/{id}")
    suspend fun getPerquisition(@Path("id") id: Int): Response<ApiResponse<PerquisitionDto>>

    @POST("api/perquisitions")
    suspend fun createPerquisition(@Body data: PerquisitionRequest): Response<ApiResponse<PerquisitionDto>>

    @PUT("api/perquisitions/{id}")
    suspend fun updatePerquisition(@Path("id") id: Int, @Body data: PerquisitionRequest): Response<ApiResponse<PerquisitionDto>>

    @DELETE("api/perquisitions/{id}")
    suspend fun deletePerquisition(@Path("id") id: Int): Response<ApiResponse<Unit>>

    @GET("api/perquisitions/{id}/attachments")
    suspend fun getPerquisitionAttachments(@Path("id") id: Int): Response<ApiResponse<List<PerquisitionAttachmentDto>>>

    @Multipart
    @POST("api/perquisitions/{id}/attachments")
    suspend fun createPerquisitionAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<PerquisitionAttachmentDto>>

    @PUT("api/perquisitions/{id}/attachments/{attachId}")
    suspend fun updatePerquisitionAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body body: AttachmentTitleRequest
    ): Response<ApiResponse<PerquisitionAttachmentDto>>

    @DELETE("api/perquisitions/{id}/attachments/{attachId}")
    suspend fun deletePerquisitionAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Unit>>

    // ========================
    // Renseignement PJ (Police Judiciaire)
    // ========================

    @GET("api/renseignements-pj")
    suspend fun getRenseignementPjList(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<RenseignementPjDto>>>

    @GET("api/renseignements-pj/{id}")
    suspend fun getRenseignementPj(@Path("id") id: Int): Response<ApiResponse<RenseignementPjDto>>

    @POST("api/renseignements-pj")
    suspend fun createRenseignementPj(@Body data: RenseignementPjRequest): Response<ApiResponse<RenseignementPjDto>>

    @PUT("api/renseignements-pj/{id}")
    suspend fun updateRenseignementPj(@Path("id") id: Int, @Body data: RenseignementPjRequest): Response<ApiResponse<RenseignementPjDto>>

    @DELETE("api/renseignements-pj/{id}")
    suspend fun deleteRenseignementPj(@Path("id") id: Int): Response<ApiResponse<Unit>>

    @GET("api/renseignements-pj/{id}/attachments")
    suspend fun getRenseignementPjAttachments(@Path("id") id: Int): Response<ApiResponse<List<RenseignementPjAttachmentDto>>>

    @Multipart
    @POST("api/renseignements-pj/{id}/attachments")
    suspend fun createRenseignementPjAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<RenseignementPjAttachmentDto>>

    @PUT("api/renseignements-pj/{id}/attachments/{attachId}")
    suspend fun updateRenseignementPjAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body body: AttachmentTitleRequest
    ): Response<ApiResponse<RenseignementPjAttachmentDto>>

    @DELETE("api/renseignements-pj/{id}/attachments/{attachId}")
    suspend fun deleteRenseignementPjAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Unit>>

    // ========================
    // Mandat (Police Judiciaire)
    // ========================

    @GET("api/mandats")
    suspend fun getMandatList(
        @Query("search") search: String? = null,
        @Query("type") type: String? = null
    ): Response<ApiResponse<List<MandatDto>>>

    @GET("api/mandats/next-number")
    suspend fun peekMandatNumber(): Response<ApiResponse<MandatNextNumberDto>>

    @GET("api/mandats/{id}")
    suspend fun getMandat(@Path("id") id: Int): Response<ApiResponse<MandatDto>>

    @POST("api/mandats")
    suspend fun createMandat(@Body data: MandatRequest): Response<ApiResponse<MandatDto>>

    @PUT("api/mandats/{id}")
    suspend fun updateMandat(@Path("id") id: Int, @Body data: MandatRequest): Response<ApiResponse<MandatDto>>

    @DELETE("api/mandats/{id}")
    suspend fun deleteMandat(@Path("id") id: Int): Response<ApiResponse<Unit>>

    @GET("api/mandats/{id}/attachments")
    suspend fun getMandatAttachments(@Path("id") id: Int): Response<ApiResponse<List<MandatAttachmentDto>>>

    @Multipart
    @POST("api/mandats/{id}/attachments")
    suspend fun createMandatAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<MandatAttachmentDto>>

    @PUT("api/mandats/{id}/attachments/{attachId}")
    suspend fun updateMandatAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body body: AttachmentTitleRequest
    ): Response<ApiResponse<MandatAttachmentDto>>

    @DELETE("api/mandats/{id}/attachments/{attachId}")
    suspend fun deleteMandatAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Unit>>

    // ========================
    // Arrestation (Police Judiciaire)
    // ========================

    @GET("api/arrestations")
    suspend fun getArrestationList(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<ArrestationDto>>>

    @GET("api/arrestations/next-number")
    suspend fun peekArrestationNumber(): Response<ApiResponse<ArrestationNextNumberDto>>

    @GET("api/arrestations/{id}")
    suspend fun getArrestation(@Path("id") id: Int): Response<ApiResponse<ArrestationDto>>

    @POST("api/arrestations")
    suspend fun createArrestation(@Body data: ArrestationRequest): Response<ApiResponse<ArrestationDto>>

    @PUT("api/arrestations/{id}")
    suspend fun updateArrestation(@Path("id") id: Int, @Body data: ArrestationRequest): Response<ApiResponse<ArrestationDto>>

    @DELETE("api/arrestations/{id}")
    suspend fun deleteArrestation(@Path("id") id: Int): Response<ApiResponse<Unit>>

    @GET("api/arrestations/{id}/attachments")
    suspend fun getArrestationAttachments(@Path("id") id: Int): Response<ApiResponse<List<ArrestationAttachmentDto>>>

    @Multipart
    @POST("api/arrestations/{id}/attachments")
    suspend fun createArrestationAttachment(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<ArrestationAttachmentDto>>

    @PUT("api/arrestations/{id}/attachments/{attachId}")
    suspend fun updateArrestationAttachmentTitle(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int,
        @Body body: AttachmentTitleRequest
    ): Response<ApiResponse<ArrestationAttachmentDto>>

    @DELETE("api/arrestations/{id}/attachments/{attachId}")
    suspend fun deleteArrestationAttachment(
        @Path("id") id: Int,
        @Path("attachId") attachId: Int
    ): Response<ApiResponse<Unit>>
}
