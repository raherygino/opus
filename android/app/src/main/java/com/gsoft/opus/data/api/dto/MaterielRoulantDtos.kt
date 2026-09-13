package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ========================
// MaterielRoulant DTOs (vehicle perception & reintegration — VHL / Moto)
// ========================

data class MaterielRoulantDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_perception") val datePerception: String,
    @SerializedName("heure_perception") val heurePerception: String,
    @SerializedName("type_materiel") val typeMateriel: String,
    @SerializedName("numero_immatriculation") val numeroImmatriculation: String? = null,
    @SerializedName("description_vehicule") val descriptionVehicule: String? = null,
    @SerializedName("agent_conducteur_personnel_id") val agentConducteurPersonnelId: Int? = null,
    @SerializedName("agent_conducteur_im") val agentConducteurIm: String? = null,
    @SerializedName("agent_conducteur_grade") val agentConducteurGrade: String? = null,
    @SerializedName("agent_conducteur_nom") val agentConducteurNom: String? = null,
    @SerializedName("chef_de_bord_personnel_id") val chefDeBordPersonnelId: Int? = null,
    @SerializedName("chef_de_bord_im") val chefDeBordIm: String? = null,
    @SerializedName("chef_de_bord_grade") val chefDeBordGrade: String? = null,
    @SerializedName("chef_de_bord_nom") val chefDeBordNom: String? = null,
    @SerializedName("kilometrage_depart") val kilometrageDepart: String? = null,
    @SerializedName("niveau_carburant_depart") val niveauCarburantDepart: String? = null,
    @SerializedName("heure_reintegration") val heureReintegration: String? = null,
    @SerializedName("date_reintegration") val dateReintegration: String? = null,
    @SerializedName("kilometrage_retour") val kilometrageRetour: String? = null,
    @SerializedName("niveau_carburant_retour") val niveauCarburantRetour: String? = null,
    @SerializedName("observations_techniques") val observationsTechniques: String? = null,
    @SerializedName("defaillances") val defaillances: String? = null,
    @SerializedName("agent_verifie") val agentVerifie: Int = 0,
    @SerializedName("agent_verifie_at") val agentVerifieAt: String? = null,
    @SerializedName("signature_svg") val signatureSvg: String? = null,
    @SerializedName("statut") val statut: String,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class MaterielRoulantRequest(
    @SerializedName("date_perception") val datePerception: String,
    @SerializedName("heure_perception") val heurePerception: String,
    @SerializedName("type_materiel") val typeMateriel: String,
    @SerializedName("numero_immatriculation") val numeroImmatriculation: String? = null,
    @SerializedName("description_vehicule") val descriptionVehicule: String? = null,
    @SerializedName("agent_conducteur_personnel_id") val agentConducteurPersonnelId: Int,
    @SerializedName("chef_de_bord_personnel_id") val chefDeBordPersonnelId: Int? = null,
    @SerializedName("kilometrage_depart") val kilometrageDepart: String? = null,
    @SerializedName("niveau_carburant_depart") val niveauCarburantDepart: String? = null,
    // Agent verification (create only — set at perception time, one-way).
    @SerializedName("code_secret") val codeSecret: String? = null,
    @SerializedName("signature_svg") val signatureSvg: String? = null
)

/** The fields of the reintegration transition. */
data class ReintegrationMaterielRoulantRequest(
    @SerializedName("date_reintegration") val dateReintegration: String,
    @SerializedName("heure_reintegration") val heureReintegration: String,
    @SerializedName("kilometrage_retour") val kilometrageRetour: String,
    @SerializedName("niveau_carburant_retour") val niveauCarburantRetour: String? = null,
    @SerializedName("observations_techniques") val observationsTechniques: String? = null,
    @SerializedName("defaillances") val defaillances: String? = null
)
