package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ─── Déclaration de perte ─────────────────────────────────────────────

data class DeclarationPerteDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_declaration") val dateDeclaration: String,
    @SerializedName("heure_declaration") val heureDeclaration: String,
    @SerializedName("identite_declarant") val identiteDeclarant: String,
    @SerializedName("nature_objet") val natureObjet: String,
    @SerializedName("description_objet") val descriptionObjet: String,
    @SerializedName("date_perte") val datePerte: String,
    @SerializedName("lieu_perte") val lieuPerte: String,
    @SerializedName("numero_attestation") val numeroAttestation: String,
    @SerializedName("nom_agent") val nomAgent: String,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<DeclarationPerteAttachmentDto>? = null
)

data class DeclarationPerteRequest(
    @SerializedName("date_declaration") val dateDeclaration: String,
    @SerializedName("heure_declaration") val heureDeclaration: String,
    @SerializedName("identite_declarant") val identiteDeclarant: String,
    @SerializedName("nature_objet") val natureObjet: String,
    @SerializedName("description_objet") val descriptionObjet: String,
    @SerializedName("date_perte") val datePerte: String,
    @SerializedName("lieu_perte") val lieuPerte: String,
    @SerializedName("numero_attestation") val numeroAttestation: String,
    @SerializedName("nom_agent") val nomAgent: String
)

data class DeclarationPerteAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("declaration_id") val declarationId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
