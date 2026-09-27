<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\RapportStats;

class RapportController
{
    /**
     * GET /api/rapports?type=daily|weekly|monthly&date=YYYY-MM-DD
     *
     * Aggregated record counts per Sédentaire module for the requested
     * period (daily report for the given day, ISO week Monday–Sunday, or
     * calendar month). Requires authentication; figures are global counts —
     * the feature is gated client-side by the sedentaire_secretariat_rapport
     * permission, like the other Sédentaire modules.
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $type = $_GET['type'] ?? 'daily';
        if (!in_array($type, RapportStats::TYPES, true)) {
            Response::error('Invalid report type (daily, weekly or monthly expected)', 422);
        }

        $date = $_GET['date'] ?? date('Y-m-d');
        if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date) || strtotime($date) === false) {
            Response::error('Invalid date format (YYYY-MM-DD expected)', 422);
        }

        Response::success(RapportStats::generate($type, $date));
    }
}
