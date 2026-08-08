package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Mirrors a row returned by GET /api/notifications (see Notification::getForUser
 * on the PHP backend — includes the joined personnel and creator fields).
 */
data class NotificationDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("message") val message: String? = null,
    @SerializedName("type") val type: String = "info",
    @SerializedName("service") val service: String = "System",
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("personnel_id") val personnelId: Int? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("is_read") val isRead: Int = 0,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("personnel_im") val personnelIm: String? = null,
    @SerializedName("personnel_nom") val personnelNom: String? = null,
    @SerializedName("personnel_prenoms") val personnelPrenoms: String? = null,
    @SerializedName("personnel_grade") val personnelGrade: String? = null,
    @SerializedName("created_by_username") val createdByUsername: String? = null
)
