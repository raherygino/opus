<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * PersonneRecherchee — wanted person record (Police Judiciaire).
 *
 * Tracks wanted persons with a dedicated multi-image table for photos,
 * separate from the generic PJ attachment system.
 */
class PersonneRecherchee
{
    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = '(p.nom LIKE ? OR p.adresse LIKE ? OR p.motif LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s);
        }

        $sql = 'SELECT p.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, pers.firstname AS agent_prenoms, pers.lastname AS agent_nom,
                (SELECT COUNT(*) FROM personne_recherchee_photo ph WHERE ph.personne_recherchee_id = p.id) AS photo_count
                FROM personne_recherchee p
                LEFT JOIN users u ON p.created_by = u.id
                LEFT JOIN personnel pers ON u.personnel_id = pers.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY p.created_at DESC, p.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT p.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, pers.firstname AS agent_prenoms, pers.lastname AS agent_nom
             FROM personne_recherchee p
             LEFT JOIN users u ON p.created_by = u.id
             LEFT JOIN personnel pers ON u.personnel_id = pers.id
             WHERE p.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO personne_recherchee (nom, adresse, motif, created_by)
             VALUES (?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['nom'],
            $data['adresse'] ?? null,
            $data['motif'],
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE personne_recherchee SET nom = ?, adresse = ?, motif = ? WHERE id = ?'
        );
        return $stmt->execute([
            $data['nom'],
            $data['adresse'] ?? null,
            $data['motif'],
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM personne_recherchee WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
