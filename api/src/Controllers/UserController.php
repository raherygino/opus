<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\User;
use App\Models\AuditLog;
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
        $authUser = AuthController::getAuthenticatedUser();
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
