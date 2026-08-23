package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ─── Passation ────────────────────────────────────────────────────────

data class PassationDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_passation") val datePassation: String,
    @SerializedName("heure_passation") val heurePassation: String,
    @SerializedName("chef_descendant_user_id") val chefDescendantUserId: Int? = null,
    @SerializedName("chef_descendant_grade") val chefDescendantGrade: String? = null,
    @SerializedName("chef_descendant_lastname") val chefDescendantLastname: String? = null,
    @SerializedName("chef_montant_user_id") val chefMontantUserId: Int? = null,
    @SerializedName("chef_montant_grade") val chefMontantGrade: String? = null,
    @SerializedName("chef_montant_lastname") val chefMontantLastname: String? = null,
    @SerializedName("instructions_autorite") val instructionsAutorite: String? = null,
    @SerializedName("incidents_survenus") val incidentsSurvenus: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("chef_descendant_username") val chefDescendantUsername: String? = null,
    @SerializedName("chef_montant_username") val chefMontantUsername: String? = null,
    @SerializedName("attachments") val attachments: List<PassationAttachmentDto>? = null
)

data class PassationRequest(
    @SerializedName("date_passation") val datePassation: String,
    @SerializedName("heure_passation") val heurePassation: String,
    @SerializedName("chef_montant_user_id") val chefMontantUserId: Int,
    @SerializedName("chef_montant_grade") val chefMontantGrade: String,
    @SerializedName("chef_montant_lastname") val chefMontantLastname: String,
    @SerializedName("instructions_autorite") val instructionsAutorite: String,
    @SerializedName("incidents_survenus") val incidentsSurvenus: String
)

data class PassationAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("passation_id") val passationId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class VerifyIdentityRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class VerifiedIdentityDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("grade") val grade: String? = null,
    @SerializedName("firstname") val firstname: String? = null,
    @SerializedName("lastname") val lastname: String? = null
)
