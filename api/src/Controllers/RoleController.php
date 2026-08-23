<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Role;
use App\Models\RolePermission;
use App\Models\AuditLog;
use App\Models\Notification;

class RoleController
{
    private static function requireAdmin(): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized();
        }
        if ($authUser['role_code'] !== 'SUPER_ADMIN') {
            Response::forbidden('Seul un Super Administrateur peut gérer les rôles');
        }
    }

    public function index(array $params): void
    {
        self::requireAdmin();
        $roles = Role::getAll();
        Response::success($roles);
    }

    public function show(array $params): void
    {
        self::requireAdmin();
        $role = Role::getById((int) $params['id']);
        if (!$role) {
            Response::notFound('Role not found');
        }
        $role['permissions'] = RolePermission::getByRoleId($role['id']);
        Response::success($role);
    }

    public function store(array $params): void
    {
        self::requireAdmin();
        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['code']) || empty($data['name'])) {
            Response::error('Code and name are required', 422);
        }

        $existing = Role::getByCode($data['code']);
        if ($existing) {
            Response::error('A role with this code already exists', 422, ['code' => 'Ce code existe déjà']);
        }

        $id = Role::create($data);
        $role = Role::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'roles',
            'entity_id' => $id,
            'description' => "Création du rôle '{$role['name']}' (Code: {$role['code']})",
            'new_values' => $role,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::created($role, 'Role created successfully');
    }

    public function update(array $params): void
    {
        self::requireAdmin();
        $id = (int) $params['id'];
        $role = Role::getById($id);
        if (!$role) {
            Response::notFound('Role not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $oldRole = $role;
        Role::update($id, $data);
        $role = Role::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'roles',
            'entity_id' => $id,
            'description' => "Modification du rôle '{$role['name']}'",
            'old_values' => $oldRole,
            'new_values' => $role,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notify every user who currently has this role ---
        // When an administrator modifies a role (name, code, description),
        // every active user with that role is informed — except the admin who
        // made the change. Recipients are determined dynamically from the
        // current user↔role relationships at the time of the change.
        $actorId = $authUser['sub'] ?? null;
        $usersWithRole = Notification::getUsersByRoleId($id);
        $recipientIds = array_map(fn($u) => (int) $u['id'], $usersWithRole);
        Notification::notifyRecipients($recipientIds, [
            'title'   => 'Rôle modifié',
            'message' => "Votre rôle « {$role['name']} » a été modifié par un administrateur. Veuillez en prendre connaissance des changements.",
            'type'    => 'info',
            'service' => 'System',
            'link'    => '/roles',
        ], $actorId);

        Response::success($role, 'Role updated successfully');
    }

    public function destroy(array $params): void
    {
        self::requireAdmin();
        $id = (int) $params['id'];
        $role = Role::getById($id);
        if (!$role) {
            Response::notFound('Role not found');
        }

        Role::delete($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'roles',
            'entity_id' => $id,
            'description' => "Suppression du rôle '{$role['name']}' (Code: {$role['code']})",
            'old_values' => $role,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Role deleted successfully');
    }

    public function permissions(array $params): void
    {
        self::requireAdmin();
        $id = (int) $params['id'];
        $role = Role::getById($id);
        if (!$role) {
            Response::notFound('Role not found');
        }

        $permissions = RolePermission::getByRoleId($id);
        Response::success([
            'role' => $role,
            'permissions' => $permissions,
        ]);
    }

    public function updatePermissions(array $params): void
    {
        self::requireAdmin();
        $id = (int) $params['id'];
        $role = Role::getById($id);
        if (!$role) {
            Response::notFound('Role not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $modules = $data['permissions'] ?? [];

        foreach ($modules as $module => $perms) {
            RolePermission::upsert($id, $module, $perms);
        }

        $permissions = RolePermission::getByRoleId($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update_permissions',
            'module' => 'roles',
            'entity_id' => $id,
            'description' => "Modification des permissions du rôle '{$role['name']}'",
            'new_values' => $modules,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notify every user who currently has this role ---
        // When an administrator changes the permissions of a role, every
        // active user with that role is informed — except the admin who made
        // the change. This is the "attribute associated with a role changed"
        // case: the role↔permission relationship directly affects what these
        // users can see and do.
        $actorId = $authUser['sub'] ?? null;
        $usersWithRole = Notification::getUsersByRoleId($id);
        $recipientIds = array_map(fn($u) => (int) $u['id'], $usersWithRole);
        Notification::notifyRecipients($recipientIds, [
            'title'   => 'Permissions modifiées',
            'message' => "Les permissions de votre rôle « {$role['name']} » ont été modifiées par un administrateur. Veuillez en prendre connaissance des changements.",
            'type'    => 'warning',
            'service' => 'System',
            'link'    => '/roles',
        ], $actorId);

        Response::success($permissions, 'Permissions updated successfully');
    }
}
