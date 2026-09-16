<?php

namespace App\Models;

use App\Database;
use PDO;

class Convocation
{
    public const TYPES = ['ST_PARQUET', 'PLAINTE_DIRECTE'];

    /** Human-readable labels for each type (used in notifications/audit). */
    public const TYPE_LABELS = [
        'ST_PARQUET'      => 'ST Parquet',
        'PLAINTE_DIRECTE' => 'Plainte directe',
    ];

    /** Map CONVOCATION type → sequence type_key (used by PlainteSequence). */
    public const TYPE_PREFIXES = [
        'ST_PARQUET'      => 'COV_ST',
        'PLAINTE_DIRECTE' => 'COV_PD',
    ];

    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT c.*,
                    u.username AS agent_username, u.personnel_id AS agent_personnel_id,
                    p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM convocation c
                LEFT JOIN users u ON c.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['type'])) {
            $sql .= ' AND c.type = ?';
            $params[] = $filters['type'];
        }
        if (!empty($filters['date_from'])) {
            $sql .= ' AND c.date_convocation >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND c.date_convocation <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (c.numero LIKE ? OR c.nom LIKE ? OR c.infraction LIKE ? OR c.numero_dossier LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY c.date_convocation DESC, c.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT c.*,
                    u.username AS agent_username, u.personnel_id AS agent_personnel_id,
                    p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM convocation c
             LEFT JOIN users u ON c.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE c.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    /**
     * Find a convocation by numero for uniqueness validation.
     */
    public static function getByNumero(string $numero): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM convocation WHERE numero = ?');
        $stmt->execute([$numero]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO convocation
                (type, date_convocation, numero, nom, adresse, infraction,
                 personne_accuse_recu, numero_dossier, observation, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['type'],
            $data['date_convocation'],
            $data['numero'],
            $data['nom'],
            $data['adresse'] ?? null,
            $data['infraction'] ?? null,
            $data['personne_accuse_recu'] ?? null,
            $data['numero_dossier'] ?? null,
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
            'type', 'date_convocation', 'nom', 'adresse', 'infraction',
            'personne_accuse_recu', 'numero_dossier', 'observation',
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
            'UPDATE convocation SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM convocation WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
