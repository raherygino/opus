<?php

namespace App\Models;

use App\Database;
use PDO;

class AuditLog
{
    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO audit_logs (user_id, action, module, entity_id, description, old_values, new_values, ip_address, user_agent)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['user_id'] ?? null,
            $data['action'],
            $data['module'],
            $data['entity_id'] ?? null,
            $data['description'] ?? null,
            isset($data['old_values']) ? json_encode($data['old_values'], JSON_UNESCAPED_UNICODE) : null,
            isset($data['new_values']) ? json_encode($data['new_values'], JSON_UNESCAPED_UNICODE) : null,
            $data['ip_address'] ?? null,
            $data['user_agent'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT a.*, u.username, p.firstname, p.lastname
                FROM audit_logs a
                LEFT JOIN users u ON a.user_id = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['action'])) {
            $sql .= ' AND a.action = ?';
            $params[] = $filters['action'];
        }
        if (!empty($filters['module'])) {
            $sql .= ' AND a.module = ?';
            $params[] = $filters['module'];
        }
        if (!empty($filters['user_id'])) {
            $sql .= ' AND a.user_id = ?';
            $params[] = $filters['user_id'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (a.description LIKE ? OR u.username LIKE ? OR p.firstname LIKE ? OR p.lastname LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }
        if (!empty($filters['date_from'])) {
            $sql .= ' AND DATE(a.created_at) >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND DATE(a.created_at) <= ?';
            $params[] = $filters['date_to'];
        }

        $sql .= ' ORDER BY a.created_at DESC LIMIT 500';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT a.*, u.username, p.firstname, p.lastname
                FROM audit_logs a
                LEFT JOIN users u ON a.user_id = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                WHERE a.id = ?';
        $stmt = $db->prepare($sql);
        $stmt->execute([$id]);
        $log = $stmt->fetch();
        return $log ?: null;
    }
}
