package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// TypeMateriel DTOs
// ========================

data class TypeMaterielDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nom") val nom: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class TypeMaterielRequest(
    @SerializedName("nom") val nom: String,
    @SerializedName("description") val description: String? = null
)

// ========================
// AffectationMateriel DTOs
// ========================

data class AffectationMaterielLigneDto(
    @SerializedName("id") val id: Int,
    @SerializedName("affectation_id") val affectationId: Int,
    @SerializedName("type_materiel_id") val typeMaterielId: Int,
    @SerializedName("type_materiel_nom") val typeMaterielNom: String,
    @SerializedName("etat_emport") val etatEmport: String? = null,
    @SerializedName("etat_reintegration") val etatReintegration: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class AffectationMaterielDto(
    @SerializedName("id") val id: Int,
    @SerializedName("agent_personnel_id") val agentPersonnelId: Int,
    @SerializedName("agent_im") val agentIm: String? = null,
    @SerializedName("agent_grade") val agentGrade: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("date_perception") val datePerception: String,
    @SerializedName("heure_perception") val heurePerception: String,
    @SerializedName("date_reintegration") val dateReintegration: String? = null,
    @SerializedName("heure_reintegration") val heureReintegration: String? = null,
    @SerializedName("statut") val statut: String,
    @SerializedName("observations") val observations: String? = null,
    @SerializedName("agent_verifie") val agentVerifie: Int = 0,
    @SerializedName("agent_verifie_at") val agentVerifieAt: String? = null,
    @SerializedName("signature_svg") val signatureSvg: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("lignes") val lignes: List<AffectationMaterielLigneDto>? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class AffectationMaterielLigneRequest(
    @SerializedName("type_materiel_id") val typeMaterielId: Int,
    @SerializedName("etat_emport") val etatEmport: String? = null
)

data class AffectationMaterielRequest(
    @SerializedName("agent_personnel_id") val agentPersonnelId: Int,
    @SerializedName("date_perception") val datePerception: String,
    @SerializedName("heure_perception") val heurePerception: String,
    @SerializedName("observations") val observations: String? = null,
    @SerializedName("lignes") val lignes: List<AffectationMaterielLigneRequest>,
    @SerializedName("code_secret") val codeSecret: String? = null,
    @SerializedName("signature_svg") val signatureSvg: String? = null
)

/** The fields of the reintegration transition. */
data class ReintegrationMaterielRequest(
    @SerializedName("date_reintegration") val dateReintegration: String,
    @SerializedName("heure_reintegration") val heureReintegration: String,
    /** Map of ligneId → etat_reintegration (one per material). */
    @SerializedName("ligne_etats") val ligneEtats: Map<Int, String>
)
