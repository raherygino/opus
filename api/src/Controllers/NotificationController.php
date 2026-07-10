<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Notification;
use App\Models\User;
use App\Controllers\AuthController;

class NotificationController
{
    private static function getAuthUser(): ?array
    {
        return AuthController::getAuthenticatedUser();
    }

    private static function getUserService(): ?string
    {
        $authUser = self::getAuthUser();
        if (!$authUser) return null;
        $user = User::getById((int) $authUser['sub']);
        if (!$user || empty($user['service'])) return null;
        return self::serviceToCode($user['service']);
    }

    private static function serviceToCode(string $service): string
    {
        if (stripos($service, 'PJ') !== false) return 'PJ';
        if (stripos($service, 'SG') !== false) return 'SG';
        if (stripos($service, 'Sédentaire') !== false || stripos($service, 'Sedentaire') !== false) return 'Sedentaire';
        return 'System';
    }

    /**
     * GET /api/notifications
     * Optional filters: ?service=PJ&is_read=0
     */
    public function index(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $filters = [];
        if (isset($_GET['service'])) {
            $filters['service'] = $_GET['service'];
        }
        if (isset($_GET['is_read']) && $_GET['is_read'] !== '') {
            $filters['is_read'] = $_GET['is_read'];
        }

        $roleCode = $authUser['role_code'] ?? null;
        $service = self::getUserService();
        $list = Notification::getForUser($authUser['sub'], $roleCode, $service);

        if (!empty($filters['service'])) {
            $list = array_filter($list, fn($n) => $n['service'] === $filters['service']);
            $list = array_values($list);
        }
        if (isset($filters['is_read']) && $filters['is_read'] !== '') {
            $target = (int) $filters['is_read'];
            $list = array_filter($list, fn($n) => (int) $n['is_read'] === $target);
            $list = array_values($list);
        }

        Response::success($list);
    }

    /**
     * GET /api/notifications/unread-count
     */
    public function unreadCount(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $roleCode = $authUser['role_code'] ?? null;
        $service = self::getUserService();
        $count = Notification::getUnreadCount($authUser['sub'], $roleCode, $service);
        Response::success(['count' => $count]);
    }

    /**
     * GET /api/notifications/{id}
     */
    public function show(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $notif = Notification::getById((int) $params['id']);
        if (!$notif) {
            Response::notFound('Notification not found');
        }
        Response::success($notif);
    }

    /**
     * POST /api/notifications
     * Body: { title, message, type, service, personnel_id?, user_id? }
     */
    public function store(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['title'])) {
            Response::error('Validation failed', 422, ['title' => 'Title is required']);
        }
        if (empty($data['service'])) {
            Response::error('Validation failed', 422, ['service' => 'Service is required']);
        }

        $data['created_by'] = $authUser['sub'];

        $id = Notification::create($data);
        $notif = Notification::getById($id);

        Response::created($notif, 'Notification created successfully');
    }

    /**
     * PUT /api/notifications/{id}/read
     * Mark a single notification as read
     */
    public function markAsRead(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $id = (int) $params['id'];
        $notif = Notification::getById($id);
        if (!$notif) {
            Response::notFound('Notification not found');
        }

        Notification::markAsRead($id);
        $notif = Notification::getById($id);
        Response::success($notif, 'Notification marked as read');
    }

    /**
     * PUT /api/notifications/read-all
     * Mark all notifications as read for the authenticated user
     */
    public function markAllAsRead(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $roleCode = $authUser['role_code'] ?? null;
        $service = self::getUserService();
        Notification::markAllAsRead($authUser['sub'], $roleCode, $service);
        Response::success(null, 'All notifications marked as read');
    }

    /**
     * DELETE /api/notifications/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $id = (int) $params['id'];
        $notif = Notification::getById($id);
        if (!$notif) {
            Response::notFound('Notification not found');
        }

        Notification::delete($id);
        Response::success(null, 'Notification deleted successfully');
    }
}
