<?php

namespace App\Models;

use App\Database;
use PDO;

class Correspondance
{
    public const SENS = ['Entrant', 'Sortant'];
    public const STATUTS = ['Enregistré', 'En traitement', 'Traité', 'Archivé'];

    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT c.*, u.username AS agent_username, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM correspondance c
                LEFT JOIN users u ON c.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['sens'])) {
            $sql .= ' AND c.sens = ?';
            $params[] = $filters['sens'];
        }
        if (!empty($filters['statut'])) {
            $sql .= ' AND c.statut = ?';
            $params[] = $filters['statut'];
        }
        if (!empty($filters['date_from'])) {
            $sql .= ' AND c.date_correspondance >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND c.date_correspondance <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (c.reference LIKE ? OR c.emetteur_destinataire LIKE ? OR c.objet LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY c.date_correspondance DESC, c.heure_enregistrement DESC, c.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT c.*, u.username AS agent_username, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM correspondance c
             LEFT JOIN users u ON c.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE c.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    /**
     * Find a correspondance by (sens, reference) for uniqueness validation.
     */
    public static function getByReference(string $sens, string $reference): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM correspondance WHERE sens = ? AND reference = ?'
        );
        $stmt->execute([$sens, $reference]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO correspondance
                (date_correspondance, heure_enregistrement, sens, reference, emetteur_destinataire, objet, statut, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['date_correspondance'],
            $data['heure_enregistrement'],
            $data['sens'],
            $data['reference'],
            $data['emetteur_destinataire'],
            $data['objet'],
            $data['statut'] ?? 'Enregistré',
            $data['created_by'] ?? null,
        ]);

        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['date_correspondance', 'heure_enregistrement', 'sens', 'reference', 'emetteur_destinataire', 'objet', 'statut'];
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
            'UPDATE correspondance SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM correspondance WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
