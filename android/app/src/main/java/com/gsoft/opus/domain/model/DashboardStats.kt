package com.gsoft.opus.domain.model

/** Aggregated KPI counts shown on the main dashboard. */
data class DashboardStats(
    val personnelTotal: Int,
    val personnelEnService: Int,
    val personnelEnMouvement: Int,
    val mouvementsEnCours: Int,
    val usersTotal: Int,
    val usersActifs: Int,
    val gavEnCours: Int,
    val armesEnService: Int,
    val vehiculesEnService: Int,
    val activitesTotal: Int,
    val activites7j: Int,
    val activitesAujourdhui: Int,
    val evenementsAujourdhui: Int,
    val mainCouranteAujourdhui: Int,
    val correspondancesTotal: Int,
    val declarationsPerteTotal: Int,
    val plaintesEnAttente: Int,
    val personnesRecherchees: Int,
    val notificationsNonLues: Int,
    val generatedAt: String?
)
