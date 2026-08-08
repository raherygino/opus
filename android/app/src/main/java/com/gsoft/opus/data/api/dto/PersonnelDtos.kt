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
    @SerializedName("updated_at") val updatedAt: String? = null
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
    @SerializedName("created_at") val createdAt: String? = null
)
