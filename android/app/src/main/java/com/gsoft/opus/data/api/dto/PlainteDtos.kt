package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Plainte ENTRÉE DTOs (Police Judiciaire — incoming complaints)
// ========================

data class PlainteEntreeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("date_plainte") val datePlainte: String,
    @SerializedName("numero_dossier") val numeroDossier: String,
    @SerializedName("numero_st") val numeroSt: String? = null,
    @SerializedName("opj_personnel_id") val opjPersonnelId: Int? = null,
    @SerializedName("enqueteur_personnel_id") val enqueteurPersonnelId: Int? = null,
    @SerializedName("partie_civile") val partieCivile: String? = null,
    @SerializedName("mise_en_cause") val miseEnCause: String? = null,
    @SerializedName("adresse_pc") val adressePc: String? = null,
    @SerializedName("infraction") val infraction: String? = null,
    @SerializedName("prejudice") val prejudice: String? = null,
    @SerializedName("lieu_infraction") val lieuInfraction: String? = null,
    @SerializedName("heure_infraction") val heureInfraction: String? = null,
    @SerializedName("observation") val observation: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("opj_prenoms") val opjPrenoms: String? = null,
    @SerializedName("opj_nom") val opjNom: String? = null,
    @SerializedName("opj_grade") val opjGrade: String? = null,
    @SerializedName("opj_im") val opjIm: String? = null,
    @SerializedName("enqueteur_prenoms") val enqueteurPrenoms: String? = null,
    @SerializedName("enqueteur_nom") val enqueteurNom: String? = null,
    @SerializedName("enqueteur_grade") val enqueteurGrade: String? = null,
    @SerializedName("enqueteur_im") val enqueteurIm: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<PlainteEntreeAttachmentDto>? = null
)

data class PlainteEntreeRequest(
    @SerializedName("type") val type: String,
    @SerializedName("date_plainte") val datePlainte: String,
    @SerializedName("numero_dossier") val numeroDossier: String? = null,
    @SerializedName("numero_st") val numeroSt: String?,
    @SerializedName("opj_personnel_id") val opjPersonnelId: Int?,
    @SerializedName("enqueteur_personnel_id") val enqueteurPersonnelId: Int?,
    @SerializedName("partie_civile") val partieCivile: String?,
    @SerializedName("mise_en_cause") val miseEnCause: String?,
    @SerializedName("adresse_pc") val adressePc: String?,
    @SerializedName("infraction") val infraction: String?,
    @SerializedName("prejudice") val prejudice: String?,
    @SerializedName("lieu_infraction") val lieuInfraction: String?,
    @SerializedName("heure_infraction") val heureInfraction: String?,
    @SerializedName("observation") val observation: String?
)

data class PlainteEntreeAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("plainte_entree_id") val plainteEntreeId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

/** Lightweight ENTRÉE summary returned by /api/plaintes-entree/without-sortie. */
data class PlainteEntreeSummaryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("numero_dossier") val numeroDossier: String,
    @SerializedName("date_plainte") val datePlainte: String,
    @SerializedName("partie_civile") val partieCivile: String? = null,
    @SerializedName("mise_en_cause") val miseEnCause: String? = null,
    @SerializedName("infraction") val infraction: String? = null,
    @SerializedName("opj_prenoms") val opjPrenoms: String? = null,
    @SerializedName("opj_nom") val opjNom: String? = null,
    @SerializedName("opj_grade") val opjGrade: String? = null
)

// ========================
// Plainte SORTIE DTOs (Police Judiciaire — outgoing processing)
// ========================

data class PlainteSortieDto(
    @SerializedName("id") val id: Int,
    @SerializedName("plainte_entree_id") val plainteEntreeId: Int,
    @SerializedName("nature") val nature: String,
    @SerializedName("date_sortie") val dateSortie: String,
    @SerializedName("numero") val numero: String,
    @SerializedName("numero_ttr") val numeroTtr: String? = null,
    @SerializedName("nom_substitut") val nomSubstitut: String? = null,
    @SerializedName("date_deferrement") val dateDeferrement: String? = null,
    @SerializedName("observation") val observation: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("entree_type") val entreeType: String? = null,
    @SerializedName("entree_numero_dossier") val entreeNumeroDossier: String? = null,
    @SerializedName("entree_date_plainte") val entreeDatePlainte: String? = null,
    @SerializedName("entree_infraction") val entreeInfraction: String? = null,
    @SerializedName("entree_mise_en_cause") val entreeMiseEnCause: String? = null,
    @SerializedName("entree_partie_civile") val entreePartieCivile: String? = null,
    @SerializedName("entree_opj_prenoms") val entreeOpjPrenoms: String? = null,
    @SerializedName("entree_opj_nom") val entreeOpjNom: String? = null,
    @SerializedName("entree_opj_grade") val entreeOpjGrade: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<PlainteSortieAttachmentDto>? = null
)

data class PlainteSortieRequest(
    @SerializedName("plainte_entree_id") val plainteEntreeId: Int,
    @SerializedName("nature") val nature: String,
    @SerializedName("date_sortie") val dateSortie: String,
    @SerializedName("numero") val numero: String? = null,
    @SerializedName("numero_ttr") val numeroTtr: String,
    @SerializedName("nom_substitut") val nomSubstitut: String,
    @SerializedName("date_deferrement") val dateDeferrement: String?,
    @SerializedName("observation") val observation: String?
)

data class PlainteSortieAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("plainte_sortie_id") val plainteSortieId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

/** Response from /api/plaintes-entree/next-number and /api/plaintes-sortie/next-number. */
data class PlainteNextNumberDto(
    @SerializedName("numero_dossier") val numeroDossier: String? = null,
    @SerializedName("numero") val numero: String? = null
)
