<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\User;
use App\Models\AuditLog;
use App\Models\Notification;
use App\Validators\UserValidator;

class UserController
{
    private static function requireAdmin(): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized();
        }
        if ($authUser['role_code'] !== 'SUPER_ADMIN') {
            Response::forbidden('Seul un Super Administrateur peut gérer les utilisateurs');
        }
    }

    /**
     * GET /api/users
     */
    public function index(array $params): void
    {
        self::requireAdmin();
        $users = User::getAll();
        Response::success($users);
    }

    /**
     * GET /api/users/{id}
     */
    public function show(array $params): void
    {
        self::requireAdmin();
        $user = User::getById((int) $params['id']);
        if (!$user) {
            Response::notFound('User not found');
        }
        unset($user['password_hash']);
        Response::success($user);
    }

    /**
     * POST /api/users
     * Body: { personnel_id, username, password, role_id, is_active? }
     *
     * Super Admin selects a Personnel record and creates a User account for them.
     */
    public function store(array $params): void
    {
        self::requireAdmin();
        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = UserValidator::validateCreate($data);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $id = User::create($data);
        $user = User::getById($id);
        unset($user['password_hash']);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'users',
            'entity_id' => $id,
            'description' => "Création de l'utilisateur '{$user['username']}' (Rôle: {$user['role_name']})",
            'new_values' => $user,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::created($user, 'User created successfully');
    }

    /**
     * PUT /api/users/{id}
     */
    public function update(array $params): void
    {
        self::requireAdmin();
        $id = (int) $params['id'];
        $user = User::getById($id);
        if (!$user) {
            Response::notFound('User not found');
        }

        $authUser = AuthController::getAuthenticatedUser();
        // Enforce: an admin must not be able to edit another admin's user
        // account. An admin may edit their own account (e.g. change password).
        if (User::isAdminRoleCode($user['role_code'] ?? '') && (int) $authUser['sub'] !== $id) {
            Response::forbidden('Vous ne pouvez pas modifier le compte d\'un autre administrateur');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = UserValidator::validateUpdate($data, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $oldUser = $user;
        User::update($id, $data);
        $user = User::getById($id);
        unset($user['password_hash']);
        unset($oldUser['password_hash']);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'users',
            'entity_id' => $id,
            'description' => "Modification de l'utilisateur '{$user['username']}'",
            'old_values' => $oldUser,
            'new_values' => $user,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notify the affected user ---
        // When an administrator changes a user's account — especially their
        // role assignment — the affected user is informed. The admin who made
        // the change is never notified (handled by notifyRecipients + the
        // self-notification guard in Notification::create).
        $actorId = $authUser['sub'] ?? null;
        Notification::notifyRecipients([$id], [
            'title'   => 'Compte modifié',
            'message' => "Votre compte utilisateur a été modifié par un administrateur. Veuillez en prendre connaissance des changements.",
            'type'    => 'info',
            'service' => 'System',
            'link'    => '/profile',
        ], $actorId);

        Response::success($user, 'User updated successfully');
    }

    /**
     * DELETE /api/users/{id}
     */
    public function destroy(array $params): void
    {
        self::requireAdmin();
        $id = (int) $params['id'];
        $user = User::getById($id);
        if (!$user) {
            Response::notFound('User not found');
        }

        // Enforce: no user — including another administrator — should be able
        // to delete an admin user. This is enforced on the backend so that
        // even a SUPER_ADMIN cannot remove another admin's account.
        if (User::isAdminRoleCode($user['role_code'] ?? '')) {
            Response::forbidden('Un compte administrateur ne peut pas être supprimé');
        }

        User::delete($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'users',
            'entity_id' => $id,
            'description' => "Suppression de l'utilisateur '{$user['username']}'",
            'old_values' => ['username' => $user['username'], 'role_name' => $user['role_name']],
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'User deleted successfully');
    }
}
