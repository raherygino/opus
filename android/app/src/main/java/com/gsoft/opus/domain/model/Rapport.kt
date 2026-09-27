package com.gsoft.opus.domain.model

/**
 * Rapport d'activité (Sédentaire > Secrétariat) — record counts per module
 * for a daily, weekly (ISO Mon–Sun) or monthly period.
 *
 * Per module: [created] counts records created inside the period, [updated]
 * counts pre-existing records modified inside it (never double-counted with
 * created), and [total] = created + updated.
 */
data class RapportSection(
    val key: String,
    val label: String,
    val created: Int,
    val updated: Int,
    val total: Int
)

data class RapportGroup(
    val key: String,
    val label: String,
    val sections: List<RapportSection>
)

data class RapportTotals(
    val created: Int,
    val updated: Int,
    val total: Int
)

data class Rapport(
    val type: String,
    val typeLabel: String,
    /** Anchor date (Y-m-d) the period was built around. */
    val date: String,
    /** Inclusive period bounds (Y-m-d) for display. */
    val periodStart: String,
    val periodEnd: String,
    val groups: List<RapportGroup>,
    val totals: RapportTotals,
    val generatedAt: String?
)
