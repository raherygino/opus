package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.DashboardStats

interface DashboardRepository {
    /** GET /api/dashboard/stats — aggregated KPI counts for the dashboard. */
    suspend fun getStats(): Resource<DashboardStats>
}
