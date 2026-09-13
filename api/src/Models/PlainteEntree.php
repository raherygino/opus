<?php

namespace App\Models;

use App\Database;
use PDO;

class PlainteEntree
{
    public const TYPES = ['ST_PARQUET', 'PLAINTE_DIRECTE', 'RAPPORT_POLICE'];

    /** Human-readable labels for each type (used in notifications/audit). */
    public const TYPE_LABELS = [
        'ST_PARQUET'      => 'ST Parquet',
        'PLAINTE_DIRECTE' => 'Plainte directe',
        'RAPPORT_POLICE'  => 'Rapport de police',
    ];

    /** Map ENTRÉE type → sequence type_key (used by PlainteSequence). */
    public const TYPE_PREFIXES = [
        'ST_PARQUET'      => 'ST',
        'PLAINTE_DIRECTE' => 'PD',
        'RAPPORT_POLICE'  => 'RP',
    ];

    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT pe.*,
                    opj.firstname AS opj_prenoms, opj.lastname AS opj_nom, opj.grade AS opj_grade, opj.im AS opj_im,
                    enq.firstname AS enqueteur_prenoms, enq.lastname AS enqueteur_nom, enq.grade AS enqueteur_grade, enq.im AS enqueteur_im,
                    u.username AS agent_username, u.personnel_id AS agent_personnel_id,
                    p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM plainte_entree pe
                LEFT JOIN personnel opj ON pe.opj_personnel_id = opj.id
                LEFT JOIN personnel enq ON pe.enqueteur_personnel_id = enq.id
                LEFT JOIN users u ON pe.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['type'])) {
            $sql .= ' AND pe.type = ?';
            $params[] = $filters['type'];
        }
        if (!empty($filters['date_from'])) {
            $sql .= ' AND pe.date_plainte >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND pe.date_plainte <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (pe.numero_dossier LIKE ? OR pe.partie_civile LIKE ? OR pe.mise_en_cause LIKE ? OR pe.infraction LIKE ? OR pe.numero_st LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY pe.date_plainte DESC, pe.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT pe.*,
                    opj.firstname AS opj_prenoms, opj.lastname AS opj_nom, opj.grade AS opj_grade, opj.im AS opj_im,
                    enq.firstname AS enqueteur_prenoms, enq.lastname AS enqueteur_nom, enq.grade AS enqueteur_grade, enq.im AS enqueteur_im,
                    u.username AS agent_username, u.personnel_id AS agent_personnel_id,
                    p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM plainte_entree pe
             LEFT JOIN personnel opj ON pe.opj_personnel_id = opj.id
             LEFT JOIN personnel enq ON pe.enqueteur_personnel_id = enq.id
             LEFT JOIN users u ON pe.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE pe.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    /**
     * Find a plainte entree by numero_dossier for uniqueness validation.
     */
    public static function getByNumeroDossier(string $numeroDossier): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM plainte_entree WHERE numero_dossier = ?');
        $stmt->execute([$numeroDossier]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    /**
     * List ENTRÉE records that have no linked SORTIE yet (for the SORTIE picker).
     */
    public static function getWithoutSortie(): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT pe.id, pe.type, pe.numero_dossier, pe.date_plainte,
                    pe.partie_civile, pe.mise_en_cause, pe.infraction,
                    opj.firstname AS opj_prenoms, opj.lastname AS opj_nom, opj.grade AS opj_grade
                FROM plainte_entree pe
                LEFT JOIN plainte_sortie ps ON ps.plainte_entree_id = pe.id
                LEFT JOIN personnel opj ON pe.opj_personnel_id = opj.id
                WHERE ps.id IS NULL
                ORDER BY pe.date_plainte DESC, pe.created_at DESC';
        $stmt = $db->query($sql);
        return $stmt->fetchAll();
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO plainte_entree
                (type, date_plainte, numero_dossier, numero_st, opj_personnel_id, enqueteur_personnel_id,
                 partie_civile, mise_en_cause, adresse_pc, infraction, prejudice, lieu_infraction,
                 heure_infraction, observation, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['type'],
            $data['date_plainte'],
            $data['numero_dossier'],
            $data['numero_st'] ?? null,
            $data['opj_personnel_id'] ?? null,
            $data['enqueteur_personnel_id'] ?? null,
            $data['partie_civile'] ?? null,
            $data['mise_en_cause'] ?? null,
            $data['adresse_pc'] ?? null,
            $data['infraction'] ?? null,
            $data['prejudice'] ?? null,
            $data['lieu_infraction'] ?? null,
            $data['heure_infraction'] ?? null,
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
            'type', 'date_plainte', 'numero_st', 'opj_personnel_id', 'enqueteur_personnel_id',
            'partie_civile', 'mise_en_cause', 'adresse_pc', 'infraction', 'prejudice',
            'lieu_infraction', 'heure_infraction', 'observation',
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
            'UPDATE plainte_entree SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM plainte_entree WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
