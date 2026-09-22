<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * EvenementSurvenu — event that occurred on public roads (Service Général).
 *
 * Single-table feature: the identities of the parties involved
 * (auteurs présumés / victimes / témoins) are stored as text fields
 * directly on the record.
 */
class EvenementSurvenu
{
    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = '(e.type_evenement LIKE ? OR e.lieu_exact LIKE ? OR e.auteurs_presumes LIKE ? OR e.victimes LIKE ? OR e.temoins LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s, $s, $s);
        }
        if (!empty($filters['type'])) {
            $where[] = 'e.type_evenement = ?';
            $args[] = $filters['type'];
        }

        $sql = 'SELECT e.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM evenement_survenu e
                LEFT JOIN users u ON e.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY e.date_evenement DESC, e.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT e.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM evenement_survenu e
             LEFT JOIN users u ON e.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE e.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO evenement_survenu
                (date_evenement, heure_evenement, type_evenement, lieu_exact,
                 auteurs_presumes, victimes, temoins, mesures_prises, latitude, longitude, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['date_evenement'],
            $data['heure_evenement'],
            $data['type_evenement'],
            $data['lieu_exact'],
            $data['auteurs_presumes'] ?? null,
            $data['victimes'] ?? null,
            $data['temoins'] ?? null,
            $data['mesures_prises'] ?? null,
            $data['latitude'] ?? null,
            $data['longitude'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE evenement_survenu SET
                date_evenement = ?, heure_evenement = ?, type_evenement = ?, lieu_exact = ?,
                auteurs_presumes = ?, victimes = ?, temoins = ?, mesures_prises = ?,
                latitude = ?, longitude = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['date_evenement'],
            $data['heure_evenement'],
            $data['type_evenement'],
            $data['lieu_exact'],
            $data['auteurs_presumes'] ?? null,
            $data['victimes'] ?? null,
            $data['temoins'] ?? null,
            $data['mesures_prises'] ?? null,
            $data['latitude'] ?? null,
            $data['longitude'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM evenement_survenu WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
