package com.gsoft.opus.presentation.roles

import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.domain.repository.ModulePermissions

/**
 * Permission module catalog — mirrors the desktop `MODULES` list in
 * `roles-form.tsx` exactly (same keys, same order, same labels).
 */
object RoleModules {

    data class ModuleDef(val key: String, val label: String)

    /** Short label: the last segment of the full "A > B > C" label. */
    val ModuleDef.shortLabel: String
        get() = label.substringAfterLast(" > ")

    /** Group label: everything before the last segment ("" when flat). */
    val ModuleDef.groupLabel: String
        get() = label.substringBeforeLast(" > ", "")

    val MODULES: List<ModuleDef> = listOf(
        ModuleDef("sedentaire_secretariat_correspondance", "Sédentaire > Secrétariat > Correspondance"),
        ModuleDef("sedentaire_secretariat_declaration_perte", "Sédentaire > Secrétariat > Déclaration de perte"),
        ModuleDef("sedentaire_secretariat_rapport", "Sédentaire > Secrétariat > Rapport"),
        ModuleDef("sedentaire_secretariat_main_courante", "Sédentaire > Secrétariat > Main courante"),
        ModuleDef("personnel", "Sédentaire > Secrétariat > Personnel"),
        ModuleDef("sedentaire_poste_passation", "Sédentaire > Poste > Passation"),
        ModuleDef("sedentaire_poste_armement", "Sédentaire > Poste > Armement"),
        ModuleDef("sedentaire_poste_materiels", "Sédentaire > Poste > Matériels"),
        ModuleDef("sedentaire_poste_materiel_roulant", "Sédentaire > Poste > Matériel roulant"),
        ModuleDef("sedentaire_poste_situation_gav", "Sédentaire > Poste > Situation GAV"),
        ModuleDef("sedentaire_poste_main_courante", "Sédentaire > Poste > Main courante"),
        ModuleDef("sedentaire_poste_renseignement", "Sédentaire > Poste > Renseignement"),
        ModuleDef("sg_rassemblement_journalier", "Service Général > Rassemblement Journalier"),
        ModuleDef("sg_evenement_survenu", "Service Général > Évènements survenus"),
        ModuleDef("sg_activite", "Service Général > Activité"),
        ModuleDef("sg_dispositif_exceptionnel", "Service Général > Dispositif exceptionnel"),
        ModuleDef("pj_plainte", "Police Judiciaire > Plainte reçue"),
        ModuleDef("pj_enquete", "Police Judiciaire > Registre d'enquête"),
        ModuleDef("pj_mandat", "Police Judiciaire > Mandat"),
        ModuleDef("pj_convocation", "Police Judiciaire > Convocation"),
        ModuleDef("pj_arrestation", "Police Judiciaire > Arrestation"),
        ModuleDef("pj_gav", "Police Judiciaire > GAV"),
        ModuleDef("pj_requisition", "Police Judiciaire > Réquisition"),
        ModuleDef("pj_personne_recherchee", "Police Judiciaire > Personne recherchée"),
        ModuleDef("pj_objets", "Police Judiciaire > Objets"),
        ModuleDef("pj_perquisition", "Police Judiciaire > Perquisition"),
        ModuleDef("pj_deferrement", "Police Judiciaire > Registre de déferrement"),
        ModuleDef("pj_renseignement", "Police Judiciaire > Renseignement"),
        ModuleDef("users", "Utilisateurs"),
    )

    val ACTIONS: List<Pair<PermissionAction, String>> = listOf(
        PermissionAction.VIEW to "Voir",
        PermissionAction.CREATE to "Créer",
        PermissionAction.EDIT to "Modif.",
        PermissionAction.DELETE to "Suppr.",
        PermissionAction.EXPORT to "Export"
    )

    /** A fully cleared map covering every known module. */
    fun emptyPermissions(): Map<String, ModulePermissions> =
        MODULES.associate { it.key to ModulePermissions.NONE }
}
