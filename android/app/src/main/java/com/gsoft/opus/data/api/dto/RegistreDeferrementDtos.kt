package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Registre de déferrement DTOs (Police Judiciaire)
// ========================

data class RegistreDeferrementDto(
    @SerializedName("id") val id: Int,
    @SerializedName("numero") val numero: String,
    @SerializedName("date_heure_deferrement") val dateHeureDeferrement: String,
    @SerializedName("personne_nom") val personneNom: String,
    @SerializedName("date_lieu_naissance") val dateLieuNaissance: String? = null,
    @SerializedName("infraction") val infraction: String? = null,
    @SerializedName("numero_dossier") val numeroDossier: String? = null,
    @SerializedName("autorite") val autorite: String? = null,
    @SerializedName("destination") val destination: String? = null,
    @SerializedName("escorte") val escorte: String? = null,
    @SerializedName("suite_donnee") val suiteDonnee: String? = null,
    @SerializedName("observations") val observations: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<RegistreDeferrementAttachmentDto>? = null
)

data class RegistreDeferrementRequest(
    @SerializedName("numero") val numero: String?,
    @SerializedName("date_heure_deferrement") val dateHeureDeferrement: String,
    @SerializedName("personne_nom") val personneNom: String,
    @SerializedName("date_lieu_naissance") val dateLieuNaissance: String?,
    @SerializedName("infraction") val infraction: String?,
    @SerializedName("numero_dossier") val numeroDossier: String?,
    @SerializedName("autorite") val autorite: String?,
    @SerializedName("destination") val destination: String?,
    @SerializedName("escorte") val escorte: String?,
    @SerializedName("suite_donnee") val suiteDonnee: String?,
    @SerializedName("observations") val observations: String?
)

data class RegistreDeferrementAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("registre_deferrement_id") val registreDeferrementId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

/** Response from /api/registres-deferrement/next-number. */
data class RegistreDeferrementNextNumberDto(
    @SerializedName("numero") val numero: String
)

fun RegistreDeferrementDto.toDomain() = com.gsoft.opus.domain.model.RegistreDeferrement(
    id = id,
    numero = numero,
    dateHeureDeferrement = dateHeureDeferrement,
    personneNom = personneNom,
    dateLieuNaissance = dateLieuNaissance,
    infraction = infraction,
    numeroDossier = numeroDossier,
    autorite = autorite,
    destination = destination,
    escorte = escorte,
    suiteDonnee = suiteDonnee,
    observations = observations,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun RegistreDeferrementAttachmentDto.toDomain() = com.gsoft.opus.domain.model.RegistreDeferrementAttachment(
    id = id,
    registreDeferrementId = registreDeferrementId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
