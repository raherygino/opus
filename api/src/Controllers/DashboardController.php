<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\DashboardStats;

class DashboardController
{
    /**
     * GET /api/dashboard/stats
     *
     * Aggregated KPI counts for the main dashboard. Requires authentication;
     * the figures are global counts (no per-module permission gating) except
     * notifications_non_lues which is scoped to the authenticated user.
     */
    public function stats(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        Response::success(
            DashboardStats::get((int) $authUser['sub'], $authUser['role_code'] ?? null)
        );
    }
}
