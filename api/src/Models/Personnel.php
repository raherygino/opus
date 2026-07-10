<?php

namespace App\Models;

use App\Database;
use PDO;

class Personnel
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT * FROM personnel WHERE 1=1';
        $params = [];

        if (!empty($filters['status'])) {
            $sql .= ' AND status = ?';
            $params[] = $filters['status'];
        }
        if (!empty($filters['grade'])) {
            $sql .= ' AND grade LIKE ?';
            $params[] = '%' . $filters['grade'] . '%';
        }
        if (!empty($filters['service'])) {
            $sql .= ' AND service LIKE ?';
            $params[] = '%' . $filters['service'] . '%';
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (lastname LIKE ? OR firstname LIKE ? OR im LIKE ? OR matricule LIKE ? OR cin LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
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
        $stmt = $db->prepare('SELECT * FROM personnel WHERE id = ?');
        $stmt->execute([$id]);
        $person = $stmt->fetch();
        return $person ?: null;
    }

    public static function getByIM(string $im): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM personnel WHERE im = ?');
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
            'INSERT INTO personnel (im, matricule, lastname, firstname, grade, fonction, service, date_prise_service, email, phone, adresse, date_naissance, lieu_naissance, cin, photo, status)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['im'],
            $data['matricule'] ?? null,
            $data['lastname'],
            $data['firstname'],
            $data['grade'],
            $data['fonction'],
            $data['service'] ?? null,
            $data['date_prise_service'] ?? null,
            $data['email'] ?? null,
            $data['phone'] ?? null,
            $data['adresse'] ?? null,
            $data['date_naissance'] ?? null,
            $data['lieu_naissance'] ?? null,
            $data['cin'] ?? null,
            $data['photo'] ?? null,
            $data['status'] ?? 'active',
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['im', 'matricule', 'lastname', 'firstname', 'grade', 'fonction', 'service', 'date_prise_service', 'email', 'phone', 'adresse', 'date_naissance', 'lieu_naissance', 'cin', 'photo', 'status'];
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
