<?php

namespace App\Models;

use App\Database;
use PDO;

class Notification
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT n.*, p.im AS personnel_im, p.lastname AS personnel_nom,
                       p.firstname AS personnel_prenoms, p.grade AS personnel_grade,
                       u.username AS created_by_username
                FROM notifications n
                LEFT JOIN personnel p ON n.personnel_id = p.id
                LEFT JOIN users u ON n.created_by = u.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['service'])) {
            $sql .= ' AND n.service = ?';
            $params[] = $filters['service'];
        }
        if (!empty($filters['user_id'])) {
            $sql .= ' AND (n.user_id = ? OR n.user_id IS NULL)';
            $params[] = $filters['user_id'];
        }
        if (isset($filters['is_read']) && $filters['is_read'] !== '') {
            $sql .= ' AND n.is_read = ?';
            $params[] = (int) $filters['is_read'];
        }

        $sql .= ' ORDER BY n.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT n.*, p.im AS personnel_im, p.lastname AS personnel_nom,
                    p.firstname AS personnel_prenoms, p.grade AS personnel_grade,
                    u.username AS created_by_username
             FROM notifications n
             LEFT JOIN personnel p ON n.personnel_id = p.id
             LEFT JOIN users u ON n.created_by = u.id
             WHERE n.id = ?'
        );
        $stmt->execute([$id]);
        $notif = $stmt->fetch();
        return $notif ?: null;
    }

    public static function getForUser(int $userId, ?string $roleCode = null, ?string $service = null): array
    {
        $db = Database::getInstance()->getConnection();

        if ($roleCode === 'SUPER_ADMIN' || $roleCode === 'STATION_ADMIN') {
            $sql = 'SELECT n.*, p.im AS personnel_im, p.lastname AS personnel_nom,
                           p.firstname AS personnel_prenoms, p.grade AS personnel_grade,
                           u.username AS created_by_username
                    FROM notifications n
                    LEFT JOIN personnel p ON n.personnel_id = p.id
                    LEFT JOIN users u ON n.created_by = u.id
                    ORDER BY n.created_at DESC';
            $stmt = $db->query($sql);
        } else {
            $sql = 'SELECT n.*, p.im AS personnel_im, p.lastname AS personnel_nom,
                           p.firstname AS personnel_prenoms, p.grade AS personnel_grade,
                           u.username AS created_by_username
                    FROM notifications n
                    LEFT JOIN personnel p ON n.personnel_id = p.id
                    LEFT JOIN users u ON n.created_by = u.id
                    WHERE n.user_id = ? OR (n.user_id IS NULL AND (? IS NULL OR n.service = ?))
                    ORDER BY n.created_at DESC';
            $stmt = $db->prepare($sql);
            $stmt->execute([$userId, $service, $service]);
        }
        return $stmt->fetchAll();
    }

    public static function getUnreadCount(int $userId, ?string $roleCode = null, ?string $service = null): int
    {
        $db = Database::getInstance()->getConnection();

        if ($roleCode === 'SUPER_ADMIN' || $roleCode === 'STATION_ADMIN') {
            $sql = 'SELECT COUNT(*) FROM notifications WHERE is_read = 0';
            $stmt = $db->query($sql);
        } else {
            $sql = 'SELECT COUNT(*) FROM notifications WHERE is_read = 0 AND (user_id = ? OR (user_id IS NULL AND (? IS NULL OR service = ?)))';
            $stmt = $db->prepare($sql);
            $stmt->execute([$userId, $service, $service]);
        }
        return (int) $stmt->fetchColumn();
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO notifications (title, message, type, service, user_id, personnel_id, created_by, is_read)
             VALUES (?, ?, ?, ?, ?, ?, ?, 0)'
        );
        $stmt->execute([
            $data['title'],
            $data['message'] ?? null,
            $data['type'] ?? 'info',
            $data['service'],
            $data['user_id'] ?? null,
            $data['personnel_id'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function markAsRead(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function markAllAsRead(int $userId, ?string $roleCode = null, ?string $service = null): bool
    {
        $db = Database::getInstance()->getConnection();

        if ($roleCode === 'SUPER_ADMIN' || $roleCode === 'STATION_ADMIN') {
            $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE is_read = 0');
            return $stmt->execute();
        } else {
            $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE is_read = 0 AND (user_id = ? OR (user_id IS NULL AND (? IS NULL OR service = ?)))');
            return $stmt->execute([$userId, $service, $service]);
        }
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM notifications WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function getAdminUsers(): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT u.id, u.username, u.personnel_id, r.code AS role_code
                FROM users u
                JOIN roles r ON u.role_id = r.id
                WHERE r.code IN ("SUPER_ADMIN", "STATION_ADMIN") AND u.is_active = 1';
        $stmt = $db->query($sql);
        return $stmt->fetchAll();
    }
}
