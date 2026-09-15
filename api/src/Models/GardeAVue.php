<?php

namespace App\Models;

use App\Database;

class GardeAVue
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT g.*,
                    u.username AS agent_username, u.personnel_id AS agent_personnel_id,
                    p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM garde_a_vue g
                LEFT JOIN users u ON g.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['date_from'])) {
            $sql .= ' AND g.debut_gav >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND g.debut_gav <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (g.nom LIKE ? OR g.prenoms LIKE ? OR g.opj_gav LIKE ? OR g.enqueteur_permance LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY g.debut_gav DESC, g.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT g.*,
                    u.username AS agent_username, u.personnel_id AS agent_personnel_id,
                    p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM garde_a_vue g
             LEFT JOIN users u ON g.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE g.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO garde_a_vue
                (nom, prenoms, date_naissance, adresse, enqueteur_permance, opj_gav, motif,
                 etat_sante, droits_notifies, personne_contacter, debut_gav, fin_gav,
                 prolongation_gav, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['nom'],
            $data['prenoms'] ?? null,
            $data['date_naissance'] ?? null,
            $data['adresse'] ?? null,
            $data['enqueteur_permance'] ?? null,
            $data['opj_gav'] ?? null,
            $data['motif'] ?? null,
            $data['etat_sante'] ?? null,
            $data['droits_notifies'] ?? null,
            $data['personne_contacter'] ?? null,
            $data['debut_gav'] ?? null,
            $data['fin_gav'] ?? null,
            $data['prolongation_gav'] ?? null,
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
            'nom', 'prenoms', 'date_naissance', 'adresse', 'enqueteur_permance', 'opj_gav',
            'motif', 'etat_sante', 'droits_notifies', 'personne_contacter', 'debut_gav',
            'fin_gav', 'prolongation_gav',
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
            'UPDATE garde_a_vue SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM garde_a_vue WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
