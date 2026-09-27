package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Rapport

interface RapportRepository {
    /**
     * GET /api/rapports?type=&date= — aggregated record counts per Sédentaire
     * module. [type] is "daily" | "weekly" | "monthly", [date] the anchor
     * date in Y-m-d format.
     */
    suspend fun getRapport(type: String, date: String): Resource<Rapport>
}
