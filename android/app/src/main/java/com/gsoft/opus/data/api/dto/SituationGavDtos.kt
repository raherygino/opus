package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Situation GAV DTOs (Sédentaire — Poste)
// ========================

data class SituationGavDto(
    @SerializedName("id") val id: Int,
    @SerializedName("garde_a_vue_id") val gardeAVueId: Int,
    @SerializedName("date_controle") val dateControle: String? = null,
    @SerializedName("agent_controle_id") val agentControleId: Int? = null,
    @SerializedName("etat_general") val etatGeneral: String? = null,
    @SerializedName("observations") val observations: String? = null,
    @SerializedName("mesures_prises") val mesuresPrises: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("personne_nom") val personneNom: String? = null,
    @SerializedName("personne_prenoms") val personnePrenoms: String? = null,
    @SerializedName("personne_debut_gav") val personneDebutGav: String? = null,
    @SerializedName("personne_fin_gav") val personneFinGav: String? = null,
    @SerializedName("agent_controle_grade") val agentControleGrade: String? = null,
    @SerializedName("agent_controle_nom") val agentControleNom: String? = null,
    @SerializedName("agent_controle_prenoms") val agentControlePrenoms: String? = null,
    @SerializedName("agent_controle_im") val agentControleIm: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<SituationGavAttachmentDto>? = null
)

data class SituationGavRequest(
    @SerializedName("garde_a_vue_id") val gardeAVueId: Int,
    @SerializedName("date_controle") val dateControle: String,
    @SerializedName("agent_controle_id") val agentControleId: Int? = null,
    @SerializedName("etat_general") val etatGeneral: String? = null,
    @SerializedName("observations") val observations: String? = null,
    @SerializedName("mesures_prises") val mesuresPrises: String? = null
)

data class SituationGavAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("situation_gav_id") val situationGavId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

fun SituationGavDto.toDomain() = com.gsoft.opus.domain.model.SituationGav(
    id = id,
    gardeAVueId = gardeAVueId,
    dateControle = dateControle,
    agentControleId = agentControleId,
    etatGeneral = etatGeneral,
    observations = observations,
    mesuresPrises = mesuresPrises,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    personneNom = personneNom,
    personnePrenoms = personnePrenoms,
    personneDebutGav = personneDebutGav,
    personneFinGav = personneFinGav,
    agentControleGrade = agentControleGrade,
    agentControleNom = agentControleNom,
    agentControlePrenoms = agentControlePrenoms,
    agentControleIm = agentControleIm,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun SituationGavAttachmentDto.toDomain() = com.gsoft.opus.domain.model.SituationGavAttachment(
    id = id,
    situationGavId = situationGavId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
