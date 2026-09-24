<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * DispositifExceptionnel — exceptional security operation (Service Général).
 *
 * A dispositif exceptionnel has one child table:
 *   - dispositif_exceptionnel_effectif ("Effectif engagé" sector rows)
 */
class DispositifExceptionnel
{
    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = 'd.nature_evenement LIKE ?';
            $args[] = '%' . $filters['search'] . '%';
        }

        $sql = 'SELECT d.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM dispositif_exceptionnel d
                LEFT JOIN users u ON d.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY d.date_debut DESC, d.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT d.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM dispositif_exceptionnel d
             LEFT JOIN users u ON d.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE d.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO dispositif_exceptionnel
                (nature_evenement, date_debut, date_fin, created_by)
             VALUES (?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['nature_evenement'],
            $data['date_debut'],
            $data['date_fin'],
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE dispositif_exceptionnel SET
                nature_evenement = ?, date_debut = ?, date_fin = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['nature_evenement'],
            $data['date_debut'],
            $data['date_fin'],
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM dispositif_exceptionnel WHERE id = ?');
        return $stmt->execute([$id]);
    }

    // ── Effectif engagé (sector rows) ──────────────────────────────

    public static function effectifs(int $dispositifId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM dispositif_exceptionnel_effectif WHERE dispositif_id = ? ORDER BY id ASC'
        );
        $stmt->execute([$dispositifId]);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function replaceEffectifs(int $dispositifId, array $rows): void
    {
        $db = Database::getInstance()->getConnection();
        $db->prepare('DELETE FROM dispositif_exceptionnel_effectif WHERE dispositif_id = ?')->execute([$dispositifId]);
        $stmt = $db->prepare(
            'INSERT INTO dispositif_exceptionnel_effectif
                (dispositif_id, secteur, chef_element_contact, controle_contact, materiels_armements, missions)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        foreach ($rows as $row) {
            $stmt->execute([
                $dispositifId,
                $row['secteur'] ?? '',
                $row['chef_element_contact'] ?? null,
                $row['controle_contact'] ?? null,
                $row['materiels_armements'] ?? null,
                $row['missions'] ?? null,
            ]);
        }
    }
}
