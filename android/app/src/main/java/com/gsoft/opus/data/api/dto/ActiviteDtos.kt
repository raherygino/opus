package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// Activités DTOs (Service Général) — patrouilles et interventions
// ========================

data class ActiviteDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_activite") val dateActivite: String,
    @SerializedName("heure_activite") val heureActivite: String,
    @SerializedName("patrouille_diurne_motorisee_itineraire") val patrouilleDiurneMotoriseeItineraire: String? = null,
    @SerializedName("patrouille_diurne_pedestre_itineraire") val patrouilleDiurnePedestreItineraire: String? = null,
    @SerializedName("patrouille_diurne_portee_itineraire") val patrouilleDiurnePorteeItineraire: String? = null,
    @SerializedName("patrouille_nocturne_motorisee_itineraire") val patrouilleNocturneMotoriseeItineraire: String? = null,
    @SerializedName("patrouille_nocturne_pedestre_itineraire") val patrouilleNocturnePedestreItineraire: String? = null,
    @SerializedName("patrouille_nocturne_portee_itineraire") val patrouilleNocturnePorteeItineraire: String? = null,
    @SerializedName("operation_ciblee") val operationCiblee: String? = null,
    @SerializedName("faits_constates") val faitsConstates: String? = null,
    @SerializedName("compte_rendu_hierarchie") val compteRenduHierarchie: String? = null,
    @SerializedName("conduite_a_tenir") val conduiteATenir: String? = null,
    @SerializedName("nature_intervention") val natureIntervention: String? = null,
    @SerializedName("suites_donnees") val suitesDonnees: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("agent_username") val agentUsername: String? = null,
    @SerializedName("agent_prenoms") val agentPrenoms: String? = null,
    @SerializedName("agent_nom") val agentNom: String? = null,
    @SerializedName("attachments") val attachments: List<ActiviteAttachmentDto>? = null
)

data class ActiviteRequest(
    @SerializedName("date_activite") val dateActivite: String,
    @SerializedName("heure_activite") val heureActivite: String,
    @SerializedName("patrouille_diurne_motorisee_itineraire") val patrouilleDiurneMotoriseeItineraire: String? = null,
    @SerializedName("patrouille_diurne_pedestre_itineraire") val patrouilleDiurnePedestreItineraire: String? = null,
    @SerializedName("patrouille_diurne_portee_itineraire") val patrouilleDiurnePorteeItineraire: String? = null,
    @SerializedName("patrouille_nocturne_motorisee_itineraire") val patrouilleNocturneMotoriseeItineraire: String? = null,
    @SerializedName("patrouille_nocturne_pedestre_itineraire") val patrouilleNocturnePedestreItineraire: String? = null,
    @SerializedName("patrouille_nocturne_portee_itineraire") val patrouilleNocturnePorteeItineraire: String? = null,
    @SerializedName("operation_ciblee") val operationCiblee: String? = null,
    @SerializedName("faits_constates") val faitsConstates: String? = null,
    @SerializedName("compte_rendu_hierarchie") val compteRenduHierarchie: String? = null,
    @SerializedName("conduite_a_tenir") val conduiteATenir: String? = null,
    @SerializedName("nature_intervention") val natureIntervention: String? = null,
    @SerializedName("suites_donnees") val suitesDonnees: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class ActiviteAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("activite_id") val activiteId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
