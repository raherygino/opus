package com.gsoft.opus.navigation

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Login : Routes("login")
    data object Main : Routes("main")
    data object QrAuthScanner : Routes("qr_auth_scanner")
}

/**
 * Destinations hosted inside the main shell (behind the bottom navigation bar).
 */
sealed class MainRoutes(val route: String) {
    data object Dashboard : MainRoutes("dashboard")
    data object Notifications : MainRoutes("notifications")
    data object Settings : MainRoutes("settings")
    data object Profile : MainRoutes("profile")
    data object PersonnelList : MainRoutes("personnel_list")

    // Sédentaire – Secrétariat
    data object SedDashboard : MainRoutes("sed_dashboard")
    data object Correspondance : MainRoutes("sed_correspondance")
    data object GestionPersonnel : MainRoutes("sed_gestion_personnel")
    data object DeclarationPerte : MainRoutes("sed_declaration_perte")
    data object Rapport : MainRoutes("sed_rapport")
    data object MainCouranteSec : MainRoutes("sed_main_courante_sec?origine=Secretariat")

    // Sédentaire – Poste
    data object Passation : MainRoutes("sed_passation")
    data object Armement : MainRoutes("sed_armement")
    data object Materiels : MainRoutes("sed_materiels")
    data object SituationGav : MainRoutes("sed_situation_gav")
    data object MainCourantePoste : MainRoutes("sed_main_courante_poste?origine=Poste")
    data object RenseignementSed : MainRoutes("sed_renseignement")

    // Division Service Général
    data object SgDashboard : MainRoutes("sg_dashboard")
    data object Spa : MainRoutes("sg_spa")
    data object RassemblementJournalier : MainRoutes("sg_rassemblement_journalier")
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
    data object Perquisition : MainRoutes("pj_perquisition")
    data object RegistreDeferrement : MainRoutes("pj_registre_deferrement")
    data object RenseignementPj : MainRoutes("pj_renseignement")

    // Global modules
    data object Cartographie : MainRoutes("cartographie")
    data object Utilisateurs : MainRoutes("utilisateurs")
    data object Roles : MainRoutes("roles")

    // Signature pad
    data object SignaturePairing : MainRoutes("signature_pairing")
    data object SignaturePad : MainRoutes("signature_pad")

    // Photo capture
    data object PhotoPairing : MainRoutes("photo_pairing")
    data object PhotoCapture : MainRoutes("photo_capture")

    // QR auth — scan to log in a desktop from the phone
    data object QrAuthScanner : MainRoutes("qr_auth_scanner_main")

    // Personnel management
    data object PersonnelDetail : MainRoutes("personnel_detail/{personnelId}") {
        fun createRoute(personnelId: Int) = "personnel_detail/$personnelId"
    }
    data object PersonnelForm : MainRoutes("personnel_form?personnelId={personnelId}") {
        fun createRoute(personnelId: Int) = "personnel_form?personnelId=$personnelId"
    }
    data object PersonnelBrowseDetail : MainRoutes("personnel_browse_detail/{personnelId}") {
        fun createRoute(personnelId: Int) = "personnel_browse_detail/$personnelId"
    }
    data object MouvementForm : MainRoutes("mouvement_form?personnelId={personnelId}") {
        fun createRoute(personnelId: Int = 0) = "mouvement_form?personnelId=$personnelId"
    }
    data object ComportementForm : MainRoutes("comportement_form?personnelId={personnelId}") {
        fun createRoute(personnelId: Int = 0) = "comportement_form?personnelId=$personnelId"
    }

    // Correspondance management
    data object CorrespondanceDetail : MainRoutes("sed_correspondance_detail/{correspondanceId}") {
        fun createRoute(correspondanceId: Int) = "sed_correspondance_detail/$correspondanceId"
    }
    data object CorrespondanceForm : MainRoutes("sed_correspondance_form?correspondanceId={correspondanceId}") {
        fun createRoute(correspondanceId: Int = 0) = "sed_correspondance_form?correspondanceId=$correspondanceId"
    }

    // Déclaration de perte management
    data object DeclarationPerteDetail : MainRoutes("sed_declaration_perte_detail/{declarationId}") {
        fun createRoute(declarationId: Int) = "sed_declaration_perte_detail/$declarationId"
    }
    data object DeclarationPerteForm : MainRoutes("sed_declaration_perte_form?declarationId={declarationId}") {
        fun createRoute(declarationId: Int = 0) = "sed_declaration_perte_form?declarationId=$declarationId"
    }

    // Passation management
    data object PassationDetail : MainRoutes("sed_passation_detail/{passationId}") {
        fun createRoute(passationId: Int) = "sed_passation_detail/$passationId"
    }
    data object PassationForm : MainRoutes("sed_passation_form?passationId={passationId}") {
        fun createRoute(passationId: Int = 0) = "sed_passation_form?passationId=$passationId"
    }

    // Armement management
    data object ArmementDetail : MainRoutes("sed_armement_detail/{armementId}") {
        fun createRoute(armementId: Int) = "sed_armement_detail/$armementId"
    }
    data object ArmementForm : MainRoutes("sed_armement_form?armementId={armementId}") {
        fun createRoute(armementId: Int = 0) = "sed_armement_form?armementId=$armementId"
    }
    data object ArmementReintegration : MainRoutes("sed_armement_reintegration/{armementId}") {
        fun createRoute(armementId: Int) = "sed_armement_reintegration/$armementId"
    }

    // Arme management (weapon catalog + ammunition stock)
    data object Arme : MainRoutes("sed_arme")
    data object ArmeDetail : MainRoutes("sed_arme_detail/{armeId}") {
        fun createRoute(armeId: Int) = "sed_arme_detail/$armeId"
    }
    data object ArmeForm : MainRoutes("sed_arme_form?armeId={armeId}") {
        fun createRoute(armeId: Int = 0) = "sed_arme_form?armeId=$armeId"
    }

    // Matériel management (equipment assignment & return)
    data object MaterielDetail : MainRoutes("sed_materiel_detail/{affectationId}") {
        fun createRoute(affectationId: Int) = "sed_materiel_detail/$affectationId"
    }
    data object MaterielForm : MainRoutes("sed_materiel_form?affectationId={affectationId}") {
        fun createRoute(affectationId: Int = 0) = "sed_materiel_form?affectationId=$affectationId"
    }
    data object MaterielReintegration : MainRoutes("sed_materiel_reintegration/{affectationId}") {
        fun createRoute(affectationId: Int) = "sed_materiel_reintegration/$affectationId"
    }

    // Matériel roulant management (vehicle perception & reintegration — VHL / Moto)
    data object MaterielRoulant : MainRoutes("sed_materiel_roulant")
    data object MaterielRoulantDetail : MainRoutes("sed_materiel_roulant_detail/{affectationId}") {
        fun createRoute(affectationId: Int) = "sed_materiel_roulant_detail/$affectationId"
    }
    data object MaterielRoulantForm : MainRoutes("sed_materiel_roulant_form?affectationId={affectationId}") {
        fun createRoute(affectationId: Int = 0) = "sed_materiel_roulant_form?affectationId=$affectationId"
    }
    data object MaterielRoulantReintegration : MainRoutes("sed_materiel_roulant_reintegration/{affectationId}") {
        fun createRoute(affectationId: Int) = "sed_materiel_roulant_reintegration/$affectationId"
    }

    // Main courante management (event logbook — Sédentaire > Secrétariat & Poste)
    // Both contexts share the same screens; the origine is passed as a query
    // parameter so the ViewModel knows which module/permission to use.
    data object MainCouranteDetail : MainRoutes("sed_main_courante_detail/{mainCouranteId}?origine={origine}") {
        fun createRoute(mainCouranteId: Int, origine: String = "Secretariat") =
            "sed_main_courante_detail/$mainCouranteId?origine=$origine"
    }
    data object MainCouranteForm : MainRoutes("sed_main_courante_form?mainCouranteId={mainCouranteId}&origine={origine}") {
        fun createRoute(mainCouranteId: Int = 0, origine: String = "Secretariat") =
            "sed_main_courante_form?mainCouranteId=$mainCouranteId&origine=$origine"
    }

    // Rassemblement journalier (Service Général) — situation de prise d'arme
    // integrated into the record; repartitions Diurne/Nocturne share one structure.
    data object RassemblementJournalierDetail : MainRoutes("sg_rassemblement_journalier_detail/{rassemblementId}") {
        fun createRoute(rassemblementId: Int) = "sg_rassemblement_journalier_detail/$rassemblementId"
    }
    data object RassemblementJournalierForm : MainRoutes("sg_rassemblement_journalier_form?rassemblementId={rassemblementId}") {
        fun createRoute(rassemblementId: Int = 0) = "sg_rassemblement_journalier_form?rassemblementId=$rassemblementId"
    }

    // Plainte (Police Judiciaire) — ENTRÉE + SORTIE
    data object PlainteDetail : MainRoutes("pj_plainte_detail/{plainteEntreeId}") {
        fun createRoute(plainteEntreeId: Int) = "pj_plainte_detail/$plainteEntreeId"
    }
    data object PlainteForm : MainRoutes("pj_plainte_form?plainteEntreeId={plainteEntreeId}") {
        fun createRoute(plainteEntreeId: Int = 0) =
            "pj_plainte_form?plainteEntreeId=$plainteEntreeId"
    }
    data object PlainteSortieDetail : MainRoutes("pj_plainte_sortie_detail/{plainteSortieId}") {
        fun createRoute(plainteSortieId: Int) = "pj_plainte_sortie_detail/$plainteSortieId"
    }
    data object PlainteSortieForm : MainRoutes("pj_plainte_sortie_form?plainteSortieId={plainteSortieId}&plainteEntreeId={plainteEntreeId}") {
        fun createRoute(plainteSortieId: Int = 0, plainteEntreeId: Int = 0) =
            "pj_plainte_sortie_form?plainteSortieId=$plainteSortieId&plainteEntreeId=$plainteEntreeId"
    }

    // Convocation (Police Judiciaire)
    data object ConvocationDetail : MainRoutes("pj_convocation_detail/{convocationId}") {
        fun createRoute(convocationId: Int) = "pj_convocation_detail/$convocationId"
    }
    data object ConvocationForm : MainRoutes("pj_convocation_form?convocationId={convocationId}") {
        fun createRoute(convocationId: Int = 0) =
            "pj_convocation_form?convocationId=$convocationId"
    }

    // Garde à Vue (Police Judiciaire)
    data object GardeAVueDetail : MainRoutes("pj_gav_detail/{gardeAVueId}") {
        fun createRoute(gardeAVueId: Int) = "pj_gav_detail/$gardeAVueId"
    }
    data object GardeAVueForm : MainRoutes("pj_gav_form?gardeAVueId={gardeAVueId}") {
        fun createRoute(gardeAVueId: Int = 0) =
            "pj_gav_form?gardeAVueId=$gardeAVueId"
    }

    // Requisition (Police Judiciaire)
    data object RequisitionDetail : MainRoutes("pj_requisition_detail/{requisitionId}") {
        fun createRoute(requisitionId: Int) = "pj_requisition_detail/$requisitionId"
    }
    data object RequisitionForm : MainRoutes("pj_requisition_form?requisitionId={requisitionId}") {
        fun createRoute(requisitionId: Int = 0) =
            "pj_requisition_form?requisitionId=$requisitionId"
    }

    // Personne Recherchée (Police Judiciaire)
    data object PersonneRechercheeDetail : MainRoutes("pj_personne_recherchee_detail/{personneRechercheeId}") {
        fun createRoute(personneRechercheeId: Int) = "pj_personne_recherchee_detail/$personneRechercheeId"
    }
    data object PersonneRechercheeForm : MainRoutes("pj_personne_recherchee_form?personneRechercheeId={personneRechercheeId}") {
        fun createRoute(personneRechercheeId: Int = 0) =
            "pj_personne_recherchee_form?personneRechercheeId=$personneRechercheeId"
    }

    // Objet Saisi / Trouvé (Police Judiciaire)
    data object ObjetSaisiDetail : MainRoutes("pj_objet_saisi_detail/{objetId}") {
        fun createRoute(objetId: Int) = "pj_objet_saisi_detail/$objetId"
    }
    data object ObjetSaisiForm : MainRoutes("pj_objet_saisi_form?objetId={objetId}") {
        fun createRoute(objetId: Int = 0) =
            "pj_objet_saisi_form?objetId=$objetId"
    }
    data object ObjetTrouveDetail : MainRoutes("pj_objet_trouve_detail/{objetId}") {
        fun createRoute(objetId: Int) = "pj_objet_trouve_detail/$objetId"
    }
    data object ObjetTrouveForm : MainRoutes("pj_objet_trouve_form?objetId={objetId}") {
        fun createRoute(objetId: Int = 0) =
            "pj_objet_trouve_form?objetId=$objetId"
    }

    // Perquisition (Police Judiciaire)
    data object PerquisitionDetail : MainRoutes("pj_perquisition_detail/{perquisitionId}") {
        fun createRoute(perquisitionId: Int) = "pj_perquisition_detail/$perquisitionId"
    }
    data object PerquisitionForm : MainRoutes("pj_perquisition_form?perquisitionId={perquisitionId}") {
        fun createRoute(perquisitionId: Int = 0) =
            "pj_perquisition_form?perquisitionId=$perquisitionId"
    }

    // Renseignement PJ (Police Judiciaire)
    data object RenseignementPjDetail : MainRoutes("pj_renseignement_detail/{renseignementId}") {
        fun createRoute(renseignementId: Int) = "pj_renseignement_detail/$renseignementId"
    }
    data object RenseignementPjForm : MainRoutes("pj_renseignement_form?renseignementId={renseignementId}") {
        fun createRoute(renseignementId: Int = 0) =
            "pj_renseignement_form?renseignementId=$renseignementId"
    }

    // Mandat (Police Judiciaire)
    data object MandatDetail : MainRoutes("pj_mandat_detail/{mandatId}") {
        fun createRoute(mandatId: Int) = "pj_mandat_detail/$mandatId"
    }
    data object MandatForm : MainRoutes("pj_mandat_form?mandatId={mandatId}") {
        fun createRoute(mandatId: Int = 0) =
            "pj_mandat_form?mandatId=$mandatId"
    }

    // Arrestation (Police Judiciaire)
    data object ArrestationDetail : MainRoutes("pj_arrestation_detail/{arrestationId}") {
        fun createRoute(arrestationId: Int) = "pj_arrestation_detail/$arrestationId"
    }
    data object ArrestationForm : MainRoutes("pj_arrestation_form?arrestationId={arrestationId}") {
        fun createRoute(arrestationId: Int = 0) =
            "pj_arrestation_form?arrestationId=$arrestationId"
    }
}
