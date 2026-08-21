<?php

namespace App\Models;

use App\Database;
use PDO;

class User
{
    public static function getAll(): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT u.id, u.username, u.is_active, u.last_login, u.created_at,
                       r.code AS role_code, r.name AS role_name,
                       p.im, p.lastname, p.firstname, p.grade,
                       p.affectation, p.phone, p.photo, p.address,
                       COALESCE(
                           (SELECT mp.type_mouvement
                            FROM mouvement_personnel mp
                            WHERE mp.personnel_id = p.id
                              AND mp.retour = "Non"
                            ORDER BY mp.created_at DESC
                            LIMIT 1),
                           "En service"
                       ) AS personnel_status
                FROM users u
                JOIN roles r ON u.role_id = r.id
                JOIN personnel p ON u.personnel_id = p.id
                ORDER BY p.lastname ASC';
        $stmt = $db->query($sql);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT u.*, r.code AS role_code, r.name AS role_name,
                       p.im, p.lastname, p.firstname, p.grade,
                       p.affectation, p.phone, p.photo, p.address,
                       COALESCE(
                           (SELECT mp.type_mouvement
                            FROM mouvement_personnel mp
                            WHERE mp.personnel_id = p.id
                              AND mp.retour = "Non"
                            ORDER BY mp.created_at DESC
                            LIMIT 1),
                           "En service"
                       ) AS personnel_status
                FROM users u
                JOIN roles r ON u.role_id = r.id
                JOIN personnel p ON u.personnel_id = p.id
                WHERE u.id = ?';
        $stmt = $db->prepare($sql);
        $stmt->execute([$id]);
        $user = $stmt->fetch();
        return $user ?: null;
    }

    public static function getByUsername(string $username): ?array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT u.*, r.code AS role_code, r.name AS role_name,
                       p.im, p.lastname, p.firstname, p.grade,
                       p.affectation, p.phone, p.photo, p.address,
                       COALESCE(
                           (SELECT mp.type_mouvement
                            FROM mouvement_personnel mp
                            WHERE mp.personnel_id = p.id
                              AND mp.retour = "Non"
                            ORDER BY mp.created_at DESC
                            LIMIT 1),
                           "En service"
                       ) AS personnel_status
                FROM users u
                JOIN roles r ON u.role_id = r.id
                JOIN personnel p ON u.personnel_id = p.id
                WHERE u.username = ?';
        $stmt = $db->prepare($sql);
        $stmt->execute([$username]);
        $user = $stmt->fetch();
        return $user ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO users (personnel_id, username, password_hash, role_id, is_active)
             VALUES (?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['personnel_id'],
            $data['username'],
            password_hash($data['password'], PASSWORD_BCRYPT, ['cost' => 12]),
            $data['role_id'],
            $data['is_active'] ?? 1,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['username', 'role_id', 'is_active'];
        foreach ($allowed as $field) {
            if (isset($data[$field])) {
                $fields[] = "$field = ?";
                $values[] = $data[$field];
            }
        }

        if (!empty($data['password'])) {
            $fields[] = 'password_hash = ?';
            $values[] = password_hash($data['password'], PASSWORD_BCRYPT, ['cost' => 12]);
        }

        if (empty($fields)) {
            return false;
        }

        $values[] = $id;
        $stmt = $db->prepare(
            'UPDATE users SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function updateLastLogin(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE users SET last_login = NOW() WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM users WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function usernameExists(string $username, ?int $excludeId = null): bool
    {
        $db = Database::getInstance()->getConnection();
        if ($excludeId) {
            $stmt = $db->prepare('SELECT COUNT(*) FROM users WHERE username = ? AND id != ?');
            $stmt->execute([$username, $excludeId]);
        } else {
            $stmt = $db->prepare('SELECT COUNT(*) FROM users WHERE username = ?');
            $stmt->execute([$username]);
        }
        return $stmt->fetchColumn() > 0;
    }

    public static function personnelHasUser(int $personnelId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT COUNT(*) FROM users WHERE personnel_id = ?');
        $stmt->execute([$personnelId]);
        return $stmt->fetchColumn() > 0;
    }

    /**
     * Get the user account linked to a personnel record (if any), including
     * the role code so callers can determine whether the personnel belongs to
     * an administrator.
     */
    public static function getByPersonnelId(int $personnelId): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT u.id, u.username, u.role_id, u.is_active, r.code AS role_code
             FROM users u
             JOIN roles r ON u.role_id = r.id
             WHERE u.personnel_id = ?'
        );
        $stmt->execute([$personnelId]);
        $user = $stmt->fetch();
        return $user ?: null;
    }

    /**
     * Returns true when the given personnel record is linked to a user account
     * whose role is an administrator role (SUPER_ADMIN or STATION_ADMIN).
     */
    public static function personnelBelongsToAdmin(int $personnelId): bool
    {
        $user = self::getByPersonnelId($personnelId);
        return $user !== null && self::isAdminRoleCode($user['role_code'] ?? '');
    }

    /**
     * Returns true when the given user id belongs to an administrator
     * (SUPER_ADMIN or STATION_ADMIN).
     */
    public static function isAdminUser(int $userId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT r.code FROM users u JOIN roles r ON u.role_id = r.id WHERE u.id = ?'
        );
        $stmt->execute([$userId]);
        $code = $stmt->fetchColumn();
        return $code && self::isAdminRoleCode($code);
    }

    /**
     * Check a role code against the canonical admin role codes.
     */
    public static function isAdminRoleCode(string $roleCode): bool
    {
        return in_array($roleCode, ['SUPER_ADMIN', 'STATION_ADMIN'], true);
    }
}
