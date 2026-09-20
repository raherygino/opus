<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * RassemblementJournalier — daily briefing record (Service Général).
 *
 * The situation de prise d'arme (weapon-taking situation) is stored directly
 * on the main record. A rassemblement journalier has one child table:
 *   - repartition_secteur (sector allocation rows, type = 'diurne' | 'nocturne')
 */
class RassemblementJournalier
{
    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = '(r.brigade_service LIKE ? OR r.officier_permanence LIKE ? OR r.inspecteur_permanence LIKE ? OR r.chef_poste LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s, $s);
        }

        $sql = 'SELECT r.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM rassemblement_journalier r
                LEFT JOIN users u ON r.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY r.date_rassemblement DESC, r.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT r.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM rassemblement_journalier r
             LEFT JOIN users u ON r.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
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
            'INSERT INTO rassemblement_journalier
                (date_rassemblement, heure_rassemblement, brigade_service, officier_permanence,
                 inspecteur_permanence, chef_poste, instructions_autorite,
                 effectif_theorique, present, absent, motif_absence, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['date_rassemblement'],
            $data['heure_rassemblement'],
            $data['brigade_service'],
            $data['officier_permanence'] ?? null,
            $data['inspecteur_permanence'] ?? null,
            $data['chef_poste'] ?? null,
            $data['instructions_autorite'] ?? null,
            (int) ($data['effectif_theorique'] ?? 0),
            (int) ($data['present'] ?? 0),
            (int) ($data['absent'] ?? 0),
            $data['motif_absence'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE rassemblement_journalier SET
                date_rassemblement = ?, heure_rassemblement = ?, brigade_service = ?,
                officier_permanence = ?, inspecteur_permanence = ?, chef_poste = ?,
                instructions_autorite = ?,
                effectif_theorique = ?, present = ?, absent = ?, motif_absence = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['date_rassemblement'],
            $data['heure_rassemblement'],
            $data['brigade_service'],
            $data['officier_permanence'] ?? null,
            $data['inspecteur_permanence'] ?? null,
            $data['chef_poste'] ?? null,
            $data['instructions_autorite'] ?? null,
            (int) ($data['effectif_theorique'] ?? 0),
            (int) ($data['present'] ?? 0),
            (int) ($data['absent'] ?? 0),
            $data['motif_absence'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM rassemblement_journalier WHERE id = ?');
        return $stmt->execute([$id]);
    }

    // ── Répartition par secteur (Diurne / Nocturne) ────────────────

    public static function repartitions(int $rassemblementId, ?string $type = null): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT * FROM repartition_secteur WHERE rassemblement_id = ?';
        $args = [$rassemblementId];
        if ($type !== null) {
            $sql .= ' AND type = ?';
            $args[] = $type;
        }
        $sql .= ' ORDER BY id ASC';
        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function replaceRepartitions(int $rassemblementId, array $rows): void
    {
        $db = Database::getInstance()->getConnection();
        $db->prepare('DELETE FROM repartition_secteur WHERE rassemblement_id = ?')->execute([$rassemblementId]);
        $stmt = $db->prepare(
            'INSERT INTO repartition_secteur
                (rassemblement_id, type, secteur, effectif_engage, chef_element_contact, controle_contact, materiels_armements, missions)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
        );
        foreach ($rows as $row) {
            $type = $row['type'] ?? 'diurne';
            if ($type !== 'diurne' && $type !== 'nocturne') {
                continue;
            }
            $stmt->execute([
                $rassemblementId,
                $type,
                $row['secteur'] ?? '',
                $row['effectif_engage'] ?? null,
                $row['chef_element_contact'] ?? null,
                $row['controle_contact'] ?? null,
                $row['materiels_armements'] ?? null,
                $row['missions'] ?? null,
            ]);
        }
    }
}
