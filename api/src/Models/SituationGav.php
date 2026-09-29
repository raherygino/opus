<?php

namespace App\Models;

use App\Database;

/**
 * Situation GAV (Sédentaire > Poste) — contrôle d'une personne en garde à vue.
 *
 * Rows reference garde_a_vue (personne concernée) and personnel (agent ayant
 * effectué le contrôle); identity data is joined, never duplicated.
 */
class SituationGav
{
    private const SELECT_FIELDS = 'sg.*,
            g.nom AS personne_nom, g.prenoms AS personne_prenoms,
            g.debut_gav AS personne_debut_gav, g.fin_gav AS personne_fin_gav,
            c.grade AS agent_controle_grade, c.lastname AS agent_controle_nom,
            c.firstname AS agent_controle_prenoms, c.im AS agent_controle_im,
            u.username AS agent_username, u.personnel_id AS agent_personnel_id,
            p.firstname AS agent_prenoms, p.lastname AS agent_nom';

    private const JOINS = ' FROM situation_gav sg
            LEFT JOIN garde_a_vue g ON sg.garde_a_vue_id = g.id
            LEFT JOIN personnel c ON sg.agent_controle_id = c.id
            LEFT JOIN users u ON sg.created_by = u.id
            LEFT JOIN personnel p ON u.personnel_id = p.id';

    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT ' . self::SELECT_FIELDS . self::JOINS . ' WHERE 1=1';
        $params = [];

        if (!empty($filters['garde_a_vue_id'])) {
            $sql .= ' AND sg.garde_a_vue_id = ?';
            $params[] = (int) $filters['garde_a_vue_id'];
        }
        if (!empty($filters['date_from'])) {
            $sql .= ' AND sg.date_controle >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND sg.date_controle <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (g.nom LIKE ? OR g.prenoms LIKE ? OR sg.etat_general LIKE ? OR sg.observations LIKE ? OR sg.mesures_prises LIKE ? OR c.lastname LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY sg.date_controle DESC, sg.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT ' . self::SELECT_FIELDS . self::JOINS . ' WHERE sg.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO situation_gav
                (garde_a_vue_id, date_controle, agent_controle_id,
                 etat_general, observations, mesures_prises, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['garde_a_vue_id'],
            $data['date_controle'],
            $data['agent_controle_id'] ?? null,
            $data['etat_general'] ?? null,
            $data['observations'] ?? null,
            $data['mesures_prises'] ?? null,
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
            'garde_a_vue_id', 'date_controle', 'agent_controle_id',
            'etat_general', 'observations', 'mesures_prises',
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
            'UPDATE situation_gav SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM situation_gav WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
