<?php

namespace App\Models;

use App\Database;
use PDO;

class MainCourante
{
    public const ORIGINES = ['Secretariat', 'Poste'];
    public const CATEGORIES = ['Entrée/Sortie de tiers', 'Incident au poste', 'Renseignement reçu'];

    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT m.*, u.username AS agent_username, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM main_courante m
                LEFT JOIN users u ON m.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['origine'])) {
            $sql .= ' AND m.origine = ?';
            $params[] = $filters['origine'];
        }
        if (!empty($filters['categorie'])) {
            $sql .= ' AND m.categorie = ?';
            $params[] = $filters['categorie'];
        }
        if (!empty($filters['date_from'])) {
            $sql .= ' AND m.date_evenement >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND m.date_evenement <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (m.description LIKE ? OR m.categorie LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY m.date_evenement DESC, m.heure_evenement DESC, m.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT m.*, u.username AS agent_username, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM main_courante m
             LEFT JOIN users u ON m.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE m.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO main_courante
                (date_evenement, heure_evenement, categorie, description, origine, created_by)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['date_evenement'],
            $data['heure_evenement'],
            $data['categorie'],
            $data['description'],
            $data['origine'] ?? 'Secretariat',
            $data['created_by'] ?? null,
        ]);

        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['date_evenement', 'heure_evenement', 'categorie', 'description', 'origine'];
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
            'UPDATE main_courante SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM main_courante WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
