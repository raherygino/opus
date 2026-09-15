<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * RenseignementPj — renseignement judiciaire (Police Judiciaire).
 *
 * Free-text record of an infraction: nature, date/lieu des faits,
 * circonstances, préjudices.
 */
class RenseignementPj
{
    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = '(r.nature_infraction LIKE ? OR r.date_lieu_faits LIKE ? OR r.circonstances LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s);
        }

        $sql = 'SELECT r.*, u.username AS agent_username, u.prenoms AS agent_prenoms, u.nom AS agent_nom
                FROM renseignement_pj r
                LEFT JOIN users u ON r.created_by = u.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY r.created_at DESC, r.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT r.*, u.username AS agent_username, u.prenoms AS agent_prenoms, u.nom AS agent_nom
             FROM renseignement_pj r
             LEFT JOIN users u ON r.created_by = u.id
             WHERE r.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO renseignement_pj
                (nature_infraction, date_lieu_faits, circonstances, prejudices, created_by)
             VALUES (?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['nature_infraction'],
            $data['date_lieu_faits'] ?? null,
            $data['circonstances'] ?? null,
            $data['prejudices'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE renseignement_pj SET
                nature_infraction = ?, date_lieu_faits = ?, circonstances = ?, prejudices = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['nature_infraction'],
            $data['date_lieu_faits'] ?? null,
            $data['circonstances'] ?? null,
            $data['prejudices'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM renseignement_pj WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
