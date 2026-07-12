<?php

namespace App\Models;

use App\Database;
use PDO;

class Personnel
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT p.*,
                       COALESCE(
                           (SELECT mp.type_mouvement
                            FROM mouvement_personnel mp
                            WHERE mp.personnel_id = p.id
                              AND mp.retour = "Non"
                            ORDER BY mp.created_at DESC
                            LIMIT 1),
                           "En service"
                       ) AS status
                FROM personnel p WHERE 1=1';
        $params = [];

        if (!empty($filters['status'])) {
            if ($filters['status'] === 'active' || $filters['status'] === 'En service') {
                $sql .= ' AND NOT EXISTS (SELECT 1 FROM mouvement_personnel mp WHERE mp.personnel_id = p.id AND mp.retour = "Non")';
            } else {
                $sql .= ' AND EXISTS (SELECT 1 FROM mouvement_personnel mp WHERE mp.personnel_id = p.id AND mp.retour = "Non" AND mp.type_mouvement = ?)';
                $params[] = $filters['status'];
            }
        }
        if (!empty($filters['grade'])) {
            $sql .= ' AND grade LIKE ?';
            $params[] = '%' . $filters['grade'] . '%';
        }
        if (!empty($filters['affectation'])) {
            $sql .= ' AND affectation LIKE ?';
            $params[] = '%' . $filters['affectation'] . '%';
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (lastname LIKE ? OR firstname LIKE ? OR im LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY lastname ASC, firstname ASC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT p.*,
                    COALESCE(
                        (SELECT mp.type_mouvement
                         FROM mouvement_personnel mp
                         WHERE mp.personnel_id = p.id
                           AND mp.retour = "Non"
                         ORDER BY mp.created_at DESC
                         LIMIT 1),
                        "En service"
                    ) AS status
             FROM personnel p WHERE p.id = ?'
        );
        $stmt->execute([$id]);
        $person = $stmt->fetch();
        return $person ?: null;
    }

    public static function getByIM(string $im): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT p.*,
                    COALESCE(
                        (SELECT mp.type_mouvement
                         FROM mouvement_personnel mp
                         WHERE mp.personnel_id = p.id
                           AND mp.retour = "Non"
                         ORDER BY mp.created_at DESC
                         LIMIT 1),
                        "En service"
                    ) AS status
             FROM personnel p WHERE p.im = ?'
        );
        $stmt->execute([$im]);
        $person = $stmt->fetch();
        return $person ?: null;
    }

    public static function getAvailableForUser(): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT p.* FROM personnel p
                LEFT JOIN users u ON p.id = u.personnel_id
                WHERE u.id IS NULL';
        $stmt = $db->query($sql);
        return $stmt->fetchAll();
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO personnel (im, grade, lastname, firstname, affectation, phone, address, photo, signature)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['im'],
            $data['grade'],
            $data['lastname'],
            $data['firstname'],
            $data['affectation'] ?? null,
            $data['phone'] ?? null,
            $data['address'] ?? null,
            $data['photo'] ?? null,
            $data['signature'] ?? null,
        ]);
        
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['im', 'grade', 'lastname', 'firstname', 'affectation', 'phone', 'address', 'photo', 'thumbnail', 'signature', 'signature_svg'];
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
            'UPDATE personnel SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM personnel WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
