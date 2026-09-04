package com.gsoft.opus.domain.model

/**
 * Weapon type/category (e.g. "Pistolet PA 9mm", "Fusil AK-47").
 * Referenced by [Arme]. Managed via the TypeArme CRUD endpoints.
 */
data class TypeArme(
    val id: Int,
    val nom: String,
    val description: String?,
    /** Shared ammunition stock for all weapons of this type. */
    val munitionsStock: Int,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * Individual physical weapon identified by its unique matricule.
 * Each arme belongs to a [TypeArme] and tracks its current ammunition
 * stock ([munitionsStock]). The stock is the source of truth for how
 * many rounds are currently available for this weapon; it is decreased
 * atomically by the backend when ammunition is consumed (either via an
 * armement reintegration recording munitionsConsommees, or via the
 * dedicated POST /armes/{id}/consommation endpoint).
 */
data class Arme(
    val id: Int,
    val typeArmeId: Int,
    /** Joined type name from the API. */
    val typeArmeNom: String?,
    val matricule: String,
    /** Legacy per-weapon stock (kept for backward compat, no longer active). */
    val munitionsStock: Int,
    /** Joined from type_arme — the active shared stock for this weapon's type. */
    val typeArmeMunitionsStock: Int,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * Ammunition consumption history row (auditable log of every ammunition
 * consumption event). Each row records which weapon ([armeId]) was
 * involved, which agent (personnel) consumed the ammunition, how many
 * rounds were consumed, when, and — when the consumption happened during
 * an armement reintegration — which perception ([armementId]) triggered
 * it. This table is the audit trail; the [Arme.munitionsStock] column is
 * the current live stock. The two are kept consistent inside a single
 * database transaction on every consumption.
 */
data class ArmeMunitionsConsommation(
    val id: Int,
    val armeId: Int,
    val agentId: Int?,
    val armementId: Int?,
    val quantite: Int,
    val dateConsommation: String,
    val createdAt: String?,
    /** Joined from arme via the API. */
    val armeMatricule: String?,
    /** Joined from type_arme via the API. */
    val typeArmeNom: String?,
    /** Joined from personnel via the API. */
    val agentIm: String?,
    val agentGrade: String?,
    val agentFirstname: String?,
    val agentLastname: String?
)
