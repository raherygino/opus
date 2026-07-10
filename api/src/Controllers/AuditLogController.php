<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\AuditLog;

class AuditLogController
{
    private static function requireAdmin(): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized();
        }
        if ($authUser['role_code'] !== 'SUPER_ADMIN') {
            Response::forbidden('Seul un Super Administrateur peut consulter les journaux d\'audit');
        }
    }

    /**
     * GET /api/audit-logs
     * Query params: action, module, user_id, search, date_from, date_to
     */
    public function index(array $params): void
    {
        self::requireAdmin();

        $filters = [];
        if (isset($_GET['action'])) {
            $filters['action'] = $_GET['action'];
        }
        if (isset($_GET['module'])) {
            $filters['module'] = $_GET['module'];
        }
        if (isset($_GET['user_id'])) {
            $filters['user_id'] = $_GET['user_id'];
        }
        if (isset($_GET['search'])) {
            $filters['search'] = $_GET['search'];
        }
        if (isset($_GET['date_from'])) {
            $filters['date_from'] = $_GET['date_from'];
        }
        if (isset($_GET['date_to'])) {
            $filters['date_to'] = $_GET['date_to'];
        }

        $list = AuditLog::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/audit-logs/{id}
     */
    public function show(array $params): void
    {
        self::requireAdmin();

        $log = AuditLog::getById((int) $params['id']);
        if (!$log) {
            Response::notFound('Audit log not found');
        }
        Response::success($log);
    }
}
