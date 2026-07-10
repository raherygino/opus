<?php

namespace App\Models;

use App\Database;
use PDO;

class MouvementPersonnel
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT * FROM mouvement_personnel WHERE 1=1';
        $params = [];

        if (!empty($filters['personnel_id'])) {
            $sql .= ' AND personnel_id = ?';
            $params[] = (int) $filters['personnel_id'];
        }
        if (!empty($filters['type_mouvement'])) {
            $sql .= ' AND type_mouvement LIKE ?';
            $params[] = '%' . $filters['type_mouvement'] . '%';
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (im LIKE ? OR nom LIKE ? OR prenoms LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM mouvement_personnel WHERE id = ?');
        $stmt->execute([$id]);
        $mouvement = $stmt->fetch();
        return $mouvement ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO mouvement_personnel (personnel_id, im, grade, service, nom, prenoms, type_mouvement, date_depart, days, date_retour, retour)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            (int) $data['personnel_id'],
            $data['im'],
            $data['grade'] ?? null,
            $data['service'] ?? null,
            $data['nom'] ?? null,
            $data['prenoms'] ?? null,
            $data['type_mouvement'],
            $data['date_depart'] ?? null,
            $data['days'] ?? null,
            $data['date_retour'] ?? null,
            $data['retour'] ?? 'Non',
        ]);
        
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['im', 'grade', 'service', 'nom', 'prenoms', 'type_mouvement', 'date_depart', 'days', 'date_retour', 'retour'];
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
            'UPDATE mouvement_personnel SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM mouvement_personnel WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
