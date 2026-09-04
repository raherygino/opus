package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// TypeArme DTOs
// ========================

data class TypeArmeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nom") val nom: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("munitions_stock") val munitionsStock: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class TypeArmeRequest(
    @SerializedName("nom") val nom: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("munitions_stock") val munitionsStock: Int = 0
)

// ========================
// Arme DTOs
// ========================

data class ArmeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type_arme_id") val typeArmeId: Int,
    @SerializedName("type_arme_nom") val typeArmeNom: String? = null,
    @SerializedName("matricule") val matricule: String,
    @SerializedName("munitions_stock") val munitionsStock: Int = 0,
    @SerializedName("type_arme_munitions_stock") val typeArmeMunitionsStock: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class ArmeRequest(
    @SerializedName("type_arme_id") val typeArmeId: Int,
    @SerializedName("matricule") val matricule: String,
    @SerializedName("munitions_stock") val munitionsStock: Int = 0
)

// ========================
// Consommation DTOs
// ========================

data class ConsommationRequest(
    @SerializedName("agent_id") val agentId: Int? = null,
    @SerializedName("quantite") val quantite: Int,
    @SerializedName("armement_id") val armementId: Int? = null
)

data class ArmeMunitionsConsommationDto(
    @SerializedName("id") val id: Int,
    @SerializedName("arme_id") val armeId: Int,
    @SerializedName("agent_id") val agentId: Int? = null,
    @SerializedName("armement_id") val armementId: Int? = null,
    @SerializedName("quantite") val quantite: Int,
    @SerializedName("date_consommation") val dateConsommation: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("arme_matricule") val armeMatricule: String? = null,
    @SerializedName("type_arme_nom") val typeArmeNom: String? = null,
    @SerializedName("agent_im") val agentIm: String? = null,
    @SerializedName("agent_grade") val agentGrade: String? = null,
    @SerializedName("agent_firstname") val agentFirstname: String? = null,
    @SerializedName("agent_lastname") val agentLastname: String? = null
)
