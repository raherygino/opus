package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Garde à Vue DTOs (Police Judiciaire — GAV)
// ========================

data class GardeAVueDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nom") val nom: String,
    @SerializedName("prenoms") val prenoms: String? = null,
    @SerializedName("date_naissance") val dateNaissance: String? = null,
    @SerializedName("adresse") val adresse: String? = null,
    @SerializedName("enqueteur_permance") val enqueteurPermance: String? = null,
    @SerializedName("opj_gav") val opjGav: String? = null,
    @SerializedName("motif") val motif: String? = null,
    @SerializedName("etat_sante") val etatSante: String? = null,
    @SerializedName("droits_notifies") val droitsNotifies: String? = null,
    @SerializedName("personne_contacter") val personneContacter: String? = null,
    @SerializedName("debut_gav") val debutGav: String? = null,
    @SerializedName("fin_gav") val finGav: String? = null,
    @SerializedName("prolongation_gav") val prolongationGav: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<GardeAVueAttachmentDto>? = null
)

data class GardeAVueRequest(
    @SerializedName("nom") val nom: String,
    @SerializedName("prenoms") val prenoms: String? = null,
    @SerializedName("date_naissance") val dateNaissance: String?,
    @SerializedName("adresse") val adresse: String?,
    @SerializedName("enqueteur_permance") val enqueteurPermance: String?,
    @SerializedName("opj_gav") val opjGav: String?,
    @SerializedName("motif") val motif: String?,
    @SerializedName("etat_sante") val etatSante: String?,
    @SerializedName("droits_notifies") val droitsNotifies: String?,
    @SerializedName("personne_contacter") val personneContacter: String?,
    @SerializedName("debut_gav") val debutGav: String?,
    @SerializedName("fin_gav") val finGav: String?,
    @SerializedName("prolongation_gav") val prolongationGav: String?
)

data class GardeAVueAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("garde_a_vue_id") val gardeAVueId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

fun GardeAVueDto.toDomain() = com.gsoft.opus.domain.model.GardeAVue(
    id = id,
    nom = nom,
    prenoms = prenoms,
    dateNaissance = dateNaissance,
    adresse = adresse,
    enqueteurPermance = enqueteurPermance,
    opjGav = opjGav,
    motif = motif,
    etatSante = etatSante,
    droitsNotifies = droitsNotifies,
    personneContacter = personneContacter,
    debutGav = debutGav,
    finGav = finGav,
    prolongationGav = prolongationGav,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun GardeAVueAttachmentDto.toDomain() = com.gsoft.opus.domain.model.GardeAVueAttachment(
    id = id,
    gardeAVueId = gardeAVueId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
