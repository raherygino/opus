<?php

namespace App\Models;

use App\Database;
use PDO;

class PlainteSortie
{
    public const NATURES = ['DAT', 'DEFERREMENT'];

    /** Human-readable labels for each nature (used in notifications/audit). */
    public const NATURE_LABELS = [
        'DAT'         => 'DAT',
        'DEFERREMENT' => 'Déferrement',
    ];

    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT ps.*,
                    pe.type AS entree_type, pe.numero_dossier AS entree_numero_dossier,
                    pe.infraction AS entree_infraction, pe.mise_en_cause AS entree_mise_en_cause,
                    u.username AS agent_username, u.personnel_id AS agent_personnel_id,
                    p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM plainte_sortie ps
                LEFT JOIN plainte_entree pe ON ps.plainte_entree_id = pe.id
                LEFT JOIN users u ON ps.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['nature'])) {
            $sql .= ' AND ps.nature = ?';
            $params[] = $filters['nature'];
        }
        if (!empty($filters['entree_id'])) {
            $sql .= ' AND ps.plainte_entree_id = ?';
            $params[] = (int) $filters['entree_id'];
        }
        if (!empty($filters['date_from'])) {
            $sql .= ' AND ps.date_sortie >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND ps.date_sortie <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (ps.numero LIKE ? OR pe.numero_dossier LIKE ? OR ps.nom_substitut LIKE ? OR ps.numero_ttr LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY ps.date_sortie DESC, ps.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT ps.*,
                    pe.type AS entree_type, pe.numero_dossier AS entree_numero_dossier,
                    pe.date_plainte AS entree_date_plainte, pe.infraction AS entree_infraction,
                    pe.mise_en_cause AS entree_mise_en_cause, pe.partie_civile AS entree_partie_civile,
                    opj.firstname AS entree_opj_prenoms, opj.lastname AS entree_opj_nom, opj.grade AS entree_opj_grade,
                    u.username AS agent_username, u.personnel_id AS agent_personnel_id,
                    p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM plainte_sortie ps
             LEFT JOIN plainte_entree pe ON ps.plainte_entree_id = pe.id
             LEFT JOIN personnel opj ON pe.opj_personnel_id = opj.id
             LEFT JOIN users u ON ps.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE ps.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    /**
     * Find a plainte sortie by numero for uniqueness validation.
     */
    public static function getByNumero(string $numero): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM plainte_sortie WHERE numero = ?');
        $stmt->execute([$numero]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    /**
     * Find the sortie linked to a given ENTRÉE (if any).
     */
    public static function getByEntreeId(int $entreeId): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM plainte_sortie WHERE plainte_entree_id = ?');
        $stmt->execute([$entreeId]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO plainte_sortie
                (plainte_entree_id, nature, date_sortie, numero, numero_ttr, nom_substitut,
                 date_deferrement, observation, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['plainte_entree_id'],
            $data['nature'],
            $data['date_sortie'],
            $data['numero'],
            $data['numero_ttr'] ?? null,
            $data['nom_substitut'] ?? null,
            $data['date_deferrement'] ?? null,
            $data['observation'] ?? null,
            $data['created_by'] ?? null,
        ]);

        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = [
            'plainte_entree_id', 'nature', 'date_sortie', 'numero_ttr', 'nom_substitut',
            'date_deferrement', 'observation',
        ];
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
            'UPDATE plainte_sortie SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM plainte_sortie WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
