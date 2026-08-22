<?php

namespace App\Models;

use App\Database;
use PDO;

class ComportementPersonnel
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT c.*, u.username AS confirmed_by_username, cu.username AS created_by_username
                FROM comportement_personnel c
                LEFT JOIN users u ON c.confirmed_by = u.id
                LEFT JOIN users cu ON c.created_by = cu.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['personnel_id'])) {
            $sql .= ' AND c.personnel_id = ?';
            $params[] = (int) $filters['personnel_id'];
        }
        if (!empty($filters['type'])) {
            $sql .= ' AND c.type = ?';
            $params[] = $filters['type'];
        }
        if (!empty($filters['status'])) {
            $sql .= ' AND c.status = ?';
            $params[] = $filters['status'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (c.im LIKE ? OR c.nom LIKE ? OR c.prenoms LIKE ? OR c.motif LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY c.date_comportement DESC, c.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT c.*, u.username AS confirmed_by_username, cu.username AS created_by_username
             FROM comportement_personnel c
             LEFT JOIN users u ON c.confirmed_by = u.id
             LEFT JOIN users cu ON c.created_by = cu.id
             WHERE c.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO comportement_personnel
                (personnel_id, im, grade, service, nom, prenoms, type, date_comportement, motif, decision, status, confirmed_by, confirmed_at, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
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
            $data['status'] ?? 'pending',
            $data['confirmed_by'] ?? null,
            $data['confirmed_at'] ?? null,
            $data['created_by'] ?? null,
        ]);

        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['im', 'grade', 'service', 'nom', 'prenoms', 'type', 'date_comportement', 'motif', 'decision', 'status', 'confirmed_by', 'confirmed_at', 'rejected_reason', 'created_by'];
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
            'UPDATE comportement_personnel SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    /**
     * Mark a comportement record as confirmed by the given administrator.
     */
    public static function confirm(int $id, int $confirmedBy): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE comportement_personnel
             SET status = "confirmed", confirmed_by = ?, confirmed_at = NOW(), rejected_reason = NULL
             WHERE id = ?'
        );
        return $stmt->execute([$confirmedBy, $id]);
    }

    /**
     * Mark a comportement record as rejected with an optional reason.
     */
    public static function reject(int $id, int $confirmedBy, ?string $reason = null): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE comportement_personnel
             SET status = "rejected", confirmed_by = ?, confirmed_at = NOW(), rejected_reason = ?
             WHERE id = ?'
        );
        return $stmt->execute([$confirmedBy, $reason, $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM comportement_personnel WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
