<?php

namespace App\Models;

use App\Database;
use PDO;

class ComportementPersonnel
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT * FROM comportement_personnel WHERE 1=1';
        $params = [];

        if (!empty($filters['personnel_id'])) {
            $sql .= ' AND personnel_id = ?';
            $params[] = (int) $filters['personnel_id'];
        }
        if (!empty($filters['type'])) {
            $sql .= ' AND type = ?';
            $params[] = $filters['type'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (im LIKE ? OR nom LIKE ? OR prenoms LIKE ? OR motif LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY date_comportement DESC, created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM comportement_personnel WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO comportement_personnel (personnel_id, im, grade, service, nom, prenoms, type, date_comportement, motif, decision)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            (int) $data['personnel_id'],
            $data['im'],
            $data['grade'] ?? null,
            $data['service'] ?? null,
            $data['nom'] ?? null,
            $data['prenoms'] ?? null,
            $data['type'],
            $data['date_comportement'],
            $data['motif'],
            $data['decision'] ?? null,
        ]);

        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['im', 'grade', 'service', 'nom', 'prenoms', 'type', 'date_comportement', 'motif', 'decision'];
        foreach ($allowed as $field) {
            if (isset($data[$field])) {
                $fields[] = "$field = ?";
                $values[] = $data[$field];
            }
        }

        if (empty($fields)) {
            return false;
        }

        $values[] = $id;
        $stmt = $db->prepare(
            'UPDATE comportement_personnel SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM comportement_personnel WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
