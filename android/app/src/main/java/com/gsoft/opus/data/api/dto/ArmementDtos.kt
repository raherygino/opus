package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

// ─── Armement ─────────────────────────────────────────────────────────

data class ArmementDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_perception") val datePerception: String,
    @SerializedName("heure_perception") val heurePerception: String,
    @SerializedName("agent_preneur_personnel_id") val agentPreneurPersonnelId: Int? = null,
    @SerializedName("agent_preneur_im") val agentPreneurIm: String? = null,
    @SerializedName("agent_preneur_grade") val agentPreneurGrade: String? = null,
    @SerializedName("agent_preneur_nom") val agentPreneurNom: String? = null,
    @SerializedName("arme_id") val armeId: Int? = null,
    @SerializedName("type_arme") val typeArme: String,
    @SerializedName("matricule_arme") val matriculeArme: String,
    @SerializedName("munitions") val munitions: Int? = null,
    @SerializedName("secteur_mission") val secteurMission: String? = null,
    @SerializedName("etat_perception") val etatPerception: String? = null,
    @SerializedName("agent_verifie") val agentVerifie: Int = 0,
    @SerializedName("agent_verifie_at") val agentVerifieAt: String? = null,
    @SerializedName("signature_svg") val signatureSvg: String? = null,
    @SerializedName("latitude") val latitude: String? = null,
    @SerializedName("longitude") val longitude: String? = null,
    @SerializedName("heure_reintegration") val heureReintegration: String? = null,
    @SerializedName("date_reintegration") val dateReintegration: String? = null,
    @SerializedName("etat_reintegration") val etatReintegration: String? = null,
    @SerializedName("munitions_consommees") val munitionsConsommees: Int? = null,
    @SerializedName("reintegration_latitude") val reintegrationLatitude: String? = null,
    @SerializedName("reintegration_longitude") val reintegrationLongitude: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("attachments") val attachments: List<ArmementAttachmentDto>? = null
)

data class ArmementRequest(
    @SerializedName("date_perception") val datePerception: String,
    @SerializedName("heure_perception") val heurePerception: String,
    // The agent preneur identity (IM + grade + nom) is snapshotted
    // server-side from the personnel table — only the id is sent.
    @SerializedName("agent_preneur_personnel_id") val agentPreneurPersonnelId: Int,
    /** FK to the exact arme perceived. When set, type_arme and
     * matricule_arme are snapshotted server-side from the arme. */
    @SerializedName("arme_id") val armeId: Int? = null,
    @SerializedName("type_arme") val typeArme: String,
    @SerializedName("matricule_arme") val matriculeArme: String,
    @SerializedName("munitions") val munitions: Int? = null,
    @SerializedName("secteur_mission") val secteurMission: String,
    @SerializedName("etat_perception") val etatPerception: String,
    // The agent preneur's code secret — verified server-side before the
    // perception is created. Required on create, ignored on update.
    @SerializedName("code_secret") val codeSecret: String? = null,
    // SVG vector data of the agent signature captured after verification.
    // Optional on create, ignored on update (one-way field).
    @SerializedName("signature_svg") val signatureSvg: String? = null,
    // GPS coordinates captured on mobile (required on Android, null on desktop).
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

/** The fields of the reintegration transition. */
data class ReintegrationRequest(
    @SerializedName("heure_reintegration") val heureReintegration: String,
    @SerializedName("date_reintegration") val dateReintegration: String,
    @SerializedName("etat_reintegration") val etatReintegration: String,
    @SerializedName("munitions_consommees") val munitionsConsommees: Int,
    @SerializedName("reintegration_latitude") val reintegrationLatitude: Double? = null,
    @SerializedName("reintegration_longitude") val reintegrationLongitude: Double? = null
)

data class ArmementAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("armement_id") val armementId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
