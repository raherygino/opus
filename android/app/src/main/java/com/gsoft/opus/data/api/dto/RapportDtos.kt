package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /api/rapports — aggregated record counts per Sédentaire
 * module for a daily / weekly (ISO Mon–Sun) / monthly period around an
 * anchor date. Counts are computed server-side so Desktop and Android
 * display identical figures. Defaults keep the payload additive-safe.
 */
data class RapportSectionDto(
    @SerializedName("key") val key: String = "",
    @SerializedName("label") val label: String = "",
    @SerializedName("created") val created: Int = 0,
    @SerializedName("updated") val updated: Int = 0,
    @SerializedName("total") val total: Int = 0
)

data class RapportGroupDto(
    @SerializedName("key") val key: String = "",
    @SerializedName("label") val label: String = "",
    @SerializedName("sections") val sections: List<RapportSectionDto> = emptyList()
)

data class RapportTotalsDto(
    @SerializedName("created") val created: Int = 0,
    @SerializedName("updated") val updated: Int = 0,
    @SerializedName("total") val total: Int = 0
)

data class RapportDto(
    @SerializedName("type") val type: String = "daily",
    @SerializedName("type_label") val typeLabel: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("period_start") val periodStart: String = "",
    @SerializedName("period_end") val periodEnd: String = "",
    @SerializedName("groups") val groups: List<RapportGroupDto> = emptyList(),
    @SerializedName("totals") val totals: RapportTotalsDto = RapportTotalsDto(),
    @SerializedName("generated_at") val generatedAt: String? = null
)
