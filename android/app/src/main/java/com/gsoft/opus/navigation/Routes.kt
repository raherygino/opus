package com.gsoft.opus.navigation

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Login : Routes("login")
    data object Main : Routes("main")
}

/**
 * Destinations hosted inside the main shell (behind the bottom navigation bar).
 */
sealed class MainRoutes(val route: String) {
    data object Dashboard : MainRoutes("dashboard")
    data object Notifications : MainRoutes("notifications")
    data object Settings : MainRoutes("settings")
    data object Profile : MainRoutes("profile")

    // Sédentaire – Secrétariat
    data object SedDashboard : MainRoutes("sed_dashboard")
    data object Correspondance : MainRoutes("sed_correspondance")
    data object GestionPersonnel : MainRoutes("sed_gestion_personnel")
    data object DeclarationPerte : MainRoutes("sed_declaration_perte")
    data object Rapport : MainRoutes("sed_rapport")
    data object MainCouranteSec : MainRoutes("sed_main_courante_sec")

    // Sédentaire – Poste
    data object Passation : MainRoutes("sed_passation")
    data object Armement : MainRoutes("sed_armement")
    data object Materiels : MainRoutes("sed_materiels")
    data object SituationGav : MainRoutes("sed_situation_gav")
    data object MainCourantePoste : MainRoutes("sed_main_courante_poste")
    data object RenseignementSed : MainRoutes("sed_renseignement")

    // Division Service Général
    data object SgDashboard : MainRoutes("sg_dashboard")
    data object Spa : MainRoutes("sg_spa")
    data object InfoRassemblement : MainRoutes("sg_info_rassemblement")
    data object Repartition : MainRoutes("sg_repartition")
    data object Patrouille : MainRoutes("sg_patrouille")
    data object Intervention : MainRoutes("sg_intervention")
    data object DispositifExceptionnel : MainRoutes("sg_dispositif_exceptionnel")
    data object InstructionAutorite : MainRoutes("sg_instruction_autorite")
    data object CompteRendu : MainRoutes("sg_compte_rendu")
    data object RechercheSg : MainRoutes("sg_recherche")
    data object RenseignementSg : MainRoutes("sg_renseignement")

    // Division Police Judiciaire
    data object PjDashboard : MainRoutes("pj_dashboard")
    data object Plainte : MainRoutes("pj_plainte")
    data object RegistreEnquete : MainRoutes("pj_registre_enquete")
    data object Mandat : MainRoutes("pj_mandat")
    data object Convocation : MainRoutes("pj_convocation")
    data object Arrestation : MainRoutes("pj_arrestation")
    data object Gav : MainRoutes("pj_gav")
    data object Requisition : MainRoutes("pj_requisition")
    data object PersonneRecherchee : MainRoutes("pj_personne_recherchee")
    data object Objets : MainRoutes("pj_objets")
    data object RegistreDeferrement : MainRoutes("pj_registre_deferrement")
    data object RenseignementPj : MainRoutes("pj_renseignement")

    // Global modules
    data object Cartographie : MainRoutes("cartographie")
    data object Utilisateurs : MainRoutes("utilisateurs")
    data object Roles : MainRoutes("roles")
}
