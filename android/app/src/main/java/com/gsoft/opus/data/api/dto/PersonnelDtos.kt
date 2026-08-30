package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ─── Personnel ───────────────────────────────────────────────────────

data class PersonnelDto(
    @SerializedName("id") val id: Int,
    @SerializedName("im") val im: String,
    @SerializedName("grade") val grade: String,
    @SerializedName("lastname") val lastname: String,
    @SerializedName("firstname") val firstname: String,
    @SerializedName("affectation") val affectation: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("photo") val photo: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("signature") val signature: String? = null,
    @SerializedName("signature_svg") val signatureSvg: String? = null,
    @SerializedName("status") val status: String = "",
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("is_admin_profile") val isAdminProfile: Boolean? = null,
    @SerializedName("has_code_secret") val hasCodeSecret: Boolean? = null
)

data class PersonnelRequest(
    @SerializedName("im") val im: String,
    @SerializedName("grade") val grade: String,
    @SerializedName("lastname") val lastname: String,
    @SerializedName("firstname") val firstname: String,
    @SerializedName("affectation") val affectation: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("thumbnail") val thumbnail: String? = null
)

data class PersonnelAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("personnel_id") val personnelId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AttachmentTitleRequest(
    @SerializedName("title") val title: String
)

// ─── Code secret (Armement identity verification) ────────────────────

data class CodeSecretRequest(
    @SerializedName("code") val code: String?
)

data class VerifyCodeSecretRequest(
    @SerializedName("code") val code: String
)

data class CodeSecretResultDto(
    @SerializedName("has_code_secret") val hasCodeSecret: Boolean? = null,
    @SerializedName("verified") val verified: Boolean? = null
)

// ─── Mouvement ───────────────────────────────────────────────────────

data class MouvementDto(
    @SerializedName("id") val id: Int,
    @SerializedName("personnel_id") val personnelId: Int,
    @SerializedName("im") val im: String,
    @SerializedName("grade") val grade: String? = null,
    @SerializedName("service") val service: String? = null,
    @SerializedName("nom") val nom: String? = null,
    @SerializedName("prenoms") val prenoms: String? = null,
    @SerializedName("type_mouvement") val typeMouvement: String,
    @SerializedName("date_depart") val dateDepart: String? = null,
    @SerializedName("days") val days: Int? = null,
    @SerializedName("date_retour") val dateRetour: String? = null,
    @SerializedName("retour") val retour: String = "Non",
    @SerializedName("created_at") val createdAt: String? = null
)

data class MouvementRequest(
    @SerializedName("personnel_id") val personnelId: Int,
    @SerializedName("im") val im: String,
    @SerializedName("grade") val grade: String?,
    @SerializedName("service") val service: String?,
    @SerializedName("nom") val nom: String?,
    @SerializedName("prenoms") val prenoms: String?,
    @SerializedName("type_mouvement") val typeMouvement: String,
    @SerializedName("date_depart") val dateDepart: String?,
    @SerializedName("days") val days: Int?,
    @SerializedName("date_retour") val dateRetour: String?,
    @SerializedName("retour") val retour: String = "Non"
)

data class MouvementRetourRequest(
    @SerializedName("date_retour") val dateRetour: String?,
    @SerializedName("retour") val retour: String = "Oui"
)

data class MouvementAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("mouvement_id") val mouvementId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// ─── Comportement ────────────────────────────────────────────────────

data class ComportementDto(
    @SerializedName("id") val id: Int,
    @SerializedName("personnel_id") val personnelId: Int,
    @SerializedName("im") val im: String,
    @SerializedName("grade") val grade: String? = null,
    @SerializedName("service") val service: String? = null,
    @SerializedName("nom") val nom: String? = null,
    @SerializedName("prenoms") val prenoms: String? = null,
    @SerializedName("type") val type: String,
    @SerializedName("date_comportement") val dateComportement: String,
    @SerializedName("motif") val motif: String,
    @SerializedName("decision") val decision: String? = null,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("confirmed_by") val confirmedBy: Int? = null,
    @SerializedName("confirmed_at") val confirmedAt: String? = null,
    @SerializedName("rejected_reason") val rejectedReason: String? = null,
    @SerializedName("confirmed_by_username") val confirmedByUsername: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_by_username") val createdByUsername: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ComportementRequest(
    @SerializedName("personnel_id") val personnelId: Int,
    @SerializedName("im") val im: String,
    @SerializedName("grade") val grade: String?,
    @SerializedName("service") val service: String?,
    @SerializedName("nom") val nom: String?,
    @SerializedName("prenoms") val prenoms: String?,
    @SerializedName("type") val type: String,
    @SerializedName("date_comportement") val dateComportement: String,
    @SerializedName("motif") val motif: String,
    @SerializedName("decision") val decision: String?
)

data class ComportementRejectRequest(
    @SerializedName("reason") val reason: String?
)
