<?php

namespace App\Models;

use App\Database;
use PDO;

class DeclarationPerte
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT d.*, u.username AS agent_username, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM declaration_perte d
                LEFT JOIN users u ON d.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['date_from'])) {
            $sql .= ' AND d.date_declaration >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND d.date_declaration <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (d.numero_attestation LIKE ? OR d.identite_declarant LIKE ? OR d.nature_objet LIKE ? OR d.lieu_perte LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY d.date_declaration DESC, d.heure_declaration DESC, d.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT d.*, u.username AS agent_username, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM declaration_perte d
             LEFT JOIN users u ON d.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE d.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    /**
     * Find a declaration by numero_attestation for uniqueness validation.
     */
    public static function getByAttestation(string $numeroAttestation): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM declaration_perte WHERE numero_attestation = ?'
        );
        $stmt->execute([$numeroAttestation]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO declaration_perte
                (date_declaration, heure_declaration, identite_declarant, nature_objet, description_objet, date_perte, lieu_perte, numero_attestation, nom_agent, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['date_declaration'],
            $data['heure_declaration'],
            $data['identite_declarant'],
            $data['nature_objet'],
            $data['description_objet'],
            $data['date_perte'],
            $data['lieu_perte'],
            $data['numero_attestation'],
            $data['nom_agent'],
            $data['created_by'] ?? null,
        ]);

        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['date_declaration', 'heure_declaration', 'identite_declarant', 'nature_objet', 'description_objet', 'date_perte', 'lieu_perte', 'numero_attestation', 'nom_agent'];
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
            'UPDATE declaration_perte SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM declaration_perte WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
