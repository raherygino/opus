package com.gsoft.opus.core

import com.gsoft.opus.domain.model.User

enum class PermissionAction { VIEW, CREATE, EDIT, DELETE, EXPORT }

/**
 * Mirrors the desktop `hasPermission` helper: SUPER_ADMIN bypasses the
 * permission table, otherwise the role permission for the given module
 * must explicitly allow the action.
 */
fun hasPermission(user: User?, module: String, action: PermissionAction): Boolean {
    if (user == null) return false
    if (user.roleCode == "SUPER_ADMIN") return true
    val perm = user.permissions.find { it.module == module } ?: return false
    return when (action) {
        PermissionAction.VIEW -> perm.canView
        PermissionAction.CREATE -> perm.canCreate
        PermissionAction.EDIT -> perm.canEdit
        PermissionAction.DELETE -> perm.canDelete
        PermissionAction.EXPORT -> perm.canExport
    }
}

/**
 * Modules grouped by division. Used to decide whether a division section
 * (drawer entry, dashboard quick link) should be visible: a division is
 * accessible when at least one of its modules is viewable.
 */
val DIVISION_MODULES: Map<String, List<String>> = mapOf(
    "sedentaire" to listOf(
        "sedentaire_secretariat_correspondance",
        "personnel",
        "sedentaire_secretariat_declaration_perte",
        "sedentaire_secretariat_rapport",
        "sedentaire_secretariat_main_courante",
        "sedentaire_poste_passation",
        "sedentaire_poste_armement",
        "sedentaire_poste_arme",
        "sedentaire_poste_materiels",
        "sedentaire_poste_materiel_roulant",
        "sedentaire_poste_situation_gav",
        "sedentaire_poste_main_courante",
    ),
    "sg" to listOf(
        "sg_rassemblement_journalier",
        "sg_evenement_survenu",
        "sg_activite",
        "sg_dispositif_exceptionnel",
    ),
    "pj" to listOf(
        "pj_plainte",
        "pj_enquete",
        "pj_mandat",
        "pj_convocation",
        "pj_arrestation",
        "pj_gav",
        "pj_requisition",
        "pj_personne_recherchee",
        "pj_objets",
        "pj_perquisition",
        "pj_deferrement",
        "pj_renseignement",
    ),
)

fun userCanAccessDivision(user: User?, division: String): Boolean {
    val modules = DIVISION_MODULES[division] ?: return false
    return modules.any { hasPermission(user, it, PermissionAction.VIEW) }
}
