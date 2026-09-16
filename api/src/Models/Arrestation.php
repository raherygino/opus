<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * Arrestation — arrest record (Police Judiciaire).
 *
 * The numero is auto-generated via PlainteSequence::ARR when the user
 * does not supply one, and is user-overridable.
 */
class Arrestation
{
    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = '(a.numero LIKE ? OR a.personne_nom LIKE ? OR a.lieu_arrestation LIKE ? OR a.numero_dossier LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s, $s);
        }

        $sql = 'SELECT a.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM arrestation a
                LEFT JOIN users u ON a.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY a.date_heure_arrestation DESC, a.created_at DESC, a.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT a.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM arrestation a
             LEFT JOIN users u ON a.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE a.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function numeroExists(string $numero, ?int $excludeId = null): bool
    {
        $db = Database::getInstance()->getConnection();
        if ($excludeId !== null) {
            $stmt = $db->prepare('SELECT 1 FROM arrestation WHERE numero = ? AND id <> ?');
            $stmt->execute([$numero, $excludeId]);
        } else {
            $stmt = $db->prepare('SELECT 1 FROM arrestation WHERE numero = ?');
            $stmt->execute([$numero]);
        }
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO arrestation
                (numero, date_heure_arrestation, personne_nom, lieu_arrestation, motif,
                 policiers, numero_dossier, observations, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['numero'],
            $data['date_heure_arrestation'],
            $data['personne_nom'],
            $data['lieu_arrestation'] ?? null,
            $data['motif'] ?? null,
            $data['policiers'] ?? null,
            $data['numero_dossier'] ?? null,
            $data['observations'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE arrestation SET
                numero = ?, date_heure_arrestation = ?, personne_nom = ?, lieu_arrestation = ?,
                motif = ?, policiers = ?, numero_dossier = ?, observations = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['numero'],
            $data['date_heure_arrestation'],
            $data['personne_nom'],
            $data['lieu_arrestation'] ?? null,
            $data['motif'] ?? null,
            $data['policiers'] ?? null,
            $data['numero_dossier'] ?? null,
            $data['observations'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM arrestation WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
