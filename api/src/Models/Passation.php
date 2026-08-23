<?php

namespace App\Models;

use App\Database;
use PDO;

class Passation
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT p.*,
                       ud.username AS chef_descendant_username,
                       um.username AS chef_montant_username
                FROM passation p
                LEFT JOIN users ud ON p.chef_descendant_user_id = ud.id
                LEFT JOIN users um ON p.chef_montant_user_id = um.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['date_from'])) {
            $sql .= ' AND p.date_passation >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND p.date_passation <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (p.chef_descendant_lastname LIKE ? OR p.chef_montant_lastname LIKE ? OR p.instructions_autorite LIKE ? OR p.incidents_survenus LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY p.date_passation DESC, p.heure_passation DESC, p.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT p.*,
                    ud.username AS chef_descendant_username,
                    um.username AS chef_montant_username
             FROM passation p
             LEFT JOIN users ud ON p.chef_descendant_user_id = ud.id
             LEFT JOIN users um ON p.chef_montant_user_id = um.id
             WHERE p.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO passation
                (date_passation, heure_passation,
                 chef_descendant_user_id, chef_descendant_grade, chef_descendant_lastname,
                 chef_montant_user_id, chef_montant_grade, chef_montant_lastname,
                 instructions_autorite, incidents_survenus, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['date_passation'],
            $data['heure_passation'],
            $data['chef_descendant_user_id'] ?? null,
            $data['chef_descendant_grade'] ?? null,
            $data['chef_descendant_lastname'] ?? null,
            $data['chef_montant_user_id'] ?? null,
            $data['chef_montant_grade'] ?? null,
            $data['chef_montant_lastname'] ?? null,
            $data['instructions_autorite'] ?? null,
            $data['incidents_survenus'] ?? null,
            $data['created_by'] ?? null,
        ]);

        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = [
            'date_passation', 'heure_passation',
            'chef_descendant_user_id', 'chef_descendant_grade', 'chef_descendant_lastname',
            'chef_montant_user_id', 'chef_montant_grade', 'chef_montant_lastname',
            'instructions_autorite', 'incidents_survenus',
        ];
        foreach ($allowed as $field) {
            if (array_key_exists($field, $data)) {
                $fields[] = "$field = ?";
                $values[] = $data[$field];
            }
        }

        if (empty($fields)) {
            return false;
        }

        $values[] = $id;
        $stmt = $db->prepare(
            'UPDATE passation SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM passation WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
