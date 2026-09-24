package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Dispositif exceptionnel DTOs (Service Général)
// ========================

data class DispositifExceptionnelDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nature_evenement") val natureEvenement: String,
    @SerializedName("date_debut") val dateDebut: String,
    @SerializedName("date_fin") val dateFin: String,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("effectifs") val effectifs: List<DispositifEffectifDto>? = null
)

data class DispositifEffectifDto(
    @SerializedName("id") val id: Int,
    @SerializedName("dispositif_id") val dispositifId: Int? = null,
    @SerializedName("secteur") val secteur: String,
    @SerializedName("chef_element_contact") val chefElementContact: String? = null,
    @SerializedName("controle_contact") val controleContact: String? = null,
    @SerializedName("materiels_armements") val materielsArmements: String? = null,
    @SerializedName("missions") val missions: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class DispositifExceptionnelRequest(
    @SerializedName("nature_evenement") val natureEvenement: String,
    @SerializedName("date_debut") val dateDebut: String,
    @SerializedName("date_fin") val dateFin: String,
    @SerializedName("effectifs") val effectifs: List<DispositifEffectifRequest> = emptyList()
)

data class DispositifEffectifRequest(
    @SerializedName("secteur") val secteur: String,
    @SerializedName("chef_element_contact") val chefElementContact: String? = null,
    @SerializedName("controle_contact") val controleContact: String? = null,
    @SerializedName("materiels_armements") val materielsArmements: String? = null,
    @SerializedName("missions") val missions: String? = null
)
