package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /api/dashboard/stats — aggregated KPI counts computed
 * server-side. All fields default to 0 so a missing key never crashes the
 * dashboard (the payload is additive-only by contract).
 */
data class DashboardStatsDto(
    @SerializedName("personnel_total") val personnelTotal: Int = 0,
    @SerializedName("personnel_en_service") val personnelEnService: Int = 0,
    @SerializedName("personnel_en_mouvement") val personnelEnMouvement: Int = 0,
    @SerializedName("mouvements_en_cours") val mouvementsEnCours: Int = 0,
    @SerializedName("users_total") val usersTotal: Int = 0,
    @SerializedName("users_actifs") val usersActifs: Int = 0,
    @SerializedName("gav_en_cours") val gavEnCours: Int = 0,
    @SerializedName("armes_en_service") val armesEnService: Int = 0,
    @SerializedName("vehicules_en_service") val vehiculesEnService: Int = 0,
    @SerializedName("activites_total") val activitesTotal: Int = 0,
    @SerializedName("activites_7j") val activites7j: Int = 0,
    @SerializedName("activites_aujourdhui") val activitesAujourdhui: Int = 0,
    @SerializedName("evenements_aujourdhui") val evenementsAujourdhui: Int = 0,
    @SerializedName("main_courante_aujourdhui") val mainCouranteAujourdhui: Int = 0,
    @SerializedName("correspondances_total") val correspondancesTotal: Int = 0,
    @SerializedName("declarations_perte_total") val declarationsPerteTotal: Int = 0,
    @SerializedName("plaintes_en_attente") val plaintesEnAttente: Int = 0,
    @SerializedName("personnes_recherchees") val personnesRecherchees: Int = 0,
    @SerializedName("notifications_non_lues") val notificationsNonLues: Int = 0,
    @SerializedName("generated_at") val generatedAt: String? = null
)
