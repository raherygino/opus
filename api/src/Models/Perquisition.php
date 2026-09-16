<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * Perquisition — search warrant (Police Judiciaire).
 *
 * The numero is auto-generated via PlainteSequence::PEQ when the user
 * does not supply one, and is user-overridable.
 */
class Perquisition
{
    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = '(p.numero LIKE ? OR p.affaire LIKE ? OR p.substitut LIKE ? OR p.numero_ttr LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s, $s);
        }

        $sql = 'SELECT p.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, pers.firstname AS agent_prenoms, pers.lastname AS agent_nom
                FROM perquisition p
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
             FROM perquisition p
             LEFT JOIN users u ON p.created_by = u.id
             LEFT JOIN personnel pers ON u.personnel_id = pers.id
             WHERE p.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function numeroExists(string $numero, ?int $excludeId = null): bool
    {
        $db = Database::getInstance()->getConnection();
        if ($excludeId !== null) {
            $stmt = $db->prepare('SELECT 1 FROM perquisition WHERE numero = ? AND id <> ?');
            $stmt->execute([$numero, $excludeId]);
        } else {
            $stmt = $db->prepare('SELECT 1 FROM perquisition WHERE numero = ?');
            $stmt->execute([$numero]);
        }
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO perquisition
                (numero, numero_ttr, substitut, affaire, motif, created_by)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['numero'],
            $data['numero_ttr'] ?? null,
            $data['substitut'] ?? null,
            $data['affaire'],
            $data['motif'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE perquisition SET
                numero = ?, numero_ttr = ?, substitut = ?, affaire = ?, motif = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['numero'],
            $data['numero_ttr'] ?? null,
            $data['substitut'] ?? null,
            $data['affaire'],
            $data['motif'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM perquisition WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
