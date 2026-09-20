package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Rassemblement Journalier DTOs (Service Général)
// ========================

data class RassemblementJournalierDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_rassemblement") val dateRassemblement: String,
    @SerializedName("heure_rassemblement") val heureRassemblement: String,
    @SerializedName("brigade_service") val brigadeService: String,
    @SerializedName("officier_permanence") val officierPermanence: String? = null,
    @SerializedName("inspecteur_permanence") val inspecteurPermanence: String? = null,
    @SerializedName("chef_poste") val chefPoste: String? = null,
    @SerializedName("instructions_autorite") val instructionsAutorite: String? = null,
    @SerializedName("effectif_theorique") val effectifTheorique: Int = 0,
    @SerializedName("present") val present: Int = 0,
    @SerializedName("absent") val absent: Int = 0,
    @SerializedName("motif_absence") val motifAbsence: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("repartitions") val repartitions: List<RepartitionSecteurDto>? = null
)

data class RepartitionSecteurDto(
    @SerializedName("id") val id: Int,
    @SerializedName("rassemblement_id") val rassemblementId: Int? = null,
    @SerializedName("type") val type: String,
    @SerializedName("secteur") val secteur: String,
    @SerializedName("effectif_engage") val effectifEngage: String? = null,
    @SerializedName("chef_element_contact") val chefElementContact: String? = null,
    @SerializedName("controle_contact") val controleContact: String? = null,
    @SerializedName("materiels_armements") val materielsArmements: String? = null,
    @SerializedName("missions") val missions: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class RassemblementJournalierRequest(
    @SerializedName("date_rassemblement") val dateRassemblement: String,
    @SerializedName("heure_rassemblement") val heureRassemblement: String,
    @SerializedName("brigade_service") val brigadeService: String,
    @SerializedName("officier_permanence") val officierPermanence: String? = null,
    @SerializedName("inspecteur_permanence") val inspecteurPermanence: String? = null,
    @SerializedName("chef_poste") val chefPoste: String? = null,
    @SerializedName("instructions_autorite") val instructionsAutorite: String? = null,
    @SerializedName("effectif_theorique") val effectifTheorique: Int = 0,
    @SerializedName("present") val present: Int = 0,
    @SerializedName("absent") val absent: Int = 0,
    @SerializedName("motif_absence") val motifAbsence: String? = null,
    @SerializedName("repartitions") val repartitions: List<RepartitionSecteurRequest> = emptyList()
)

data class RepartitionSecteurRequest(
    @SerializedName("type") val type: String,
    @SerializedName("secteur") val secteur: String,
    @SerializedName("effectif_engage") val effectifEngage: String? = null,
    @SerializedName("chef_element_contact") val chefElementContact: String? = null,
    @SerializedName("controle_contact") val controleContact: String? = null,
    @SerializedName("materiels_armements") val materielsArmements: String? = null,
    @SerializedName("missions") val missions: String? = null
)
