<?php

namespace App\Models;

use App\Database;
use PDO;

class RolePermission
{
    public static function getByRoleId(int $roleId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM role_permissions WHERE role_id = ?');
        $stmt->execute([$roleId]);
        return $stmt->fetchAll();
    }

    public static function upsert(int $roleId, string $module, array $perms): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO role_permissions (role_id, module, can_view, can_create, can_edit, can_delete, can_export)
             VALUES (?, ?, ?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE
               can_view = VALUES(can_view),
               can_create = VALUES(can_create),
               can_edit = VALUES(can_edit),
               can_delete = VALUES(can_delete),
               can_export = VALUES(can_export)'
        );
        return $stmt->execute([
            $roleId,
            $module,
            $perms['can_view'] ?? 0,
            $perms['can_create'] ?? 0,
            $perms['can_edit'] ?? 0,
            $perms['can_delete'] ?? 0,
            $perms['can_export'] ?? 0,
        ]);
    }

    public static function deleteByRoleId(int $roleId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM role_permissions WHERE role_id = ?');
        return $stmt->execute([$roleId]);
    }
}
