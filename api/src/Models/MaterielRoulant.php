<?php

namespace App\Models;

use App\Database;
use PDO;

class MaterielRoulant
{
    public const STATUTS = ['En service', 'Réintégré'];
    public const TYPES = ['VHL', 'Moto'];

    /**
     * List all vehicle movements with optional filters. Each row is
     * returned with its driver and chef de bord snapshots.
     */
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT m.*,
                       d.im AS agent_conducteur_personnel_im,
                       c.im AS chef_de_bord_personnel_im
                FROM materiel_roulant m
                LEFT JOIN personnel d ON m.agent_conducteur_personnel_id = d.id
                LEFT JOIN personnel c ON m.chef_de_bord_personnel_id = c.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['date_from'])) {
            $sql .= ' AND m.date_perception >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND m.date_perception <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['statut'])) {
            if ($filters['statut'] === 'en_service') {
                $sql .= ' AND m.statut = ?';
                $params[] = 'En service';
            } elseif ($filters['statut'] === 'reintegre') {
                $sql .= ' AND m.statut = ?';
                $params[] = 'Réintégré';
            }
        }
        if (!empty($filters['type_materiel'])) {
            $sql .= ' AND m.type_materiel = ?';
            $params[] = $filters['type_materiel'];
        }
        if (!empty($filters['agent_conducteur_personnel_id'])) {
            $sql .= ' AND m.agent_conducteur_personnel_id = ?';
            $params[] = (int) $filters['agent_conducteur_personnel_id'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (m.agent_conducteur_nom LIKE ? OR m.agent_conducteur_im LIKE ?
                          OR m.chef_de_bord_nom LIKE ? OR m.chef_de_bord_im LIKE ?
                          OR m.numero_immatriculation LIKE ?
                          OR m.description_vehicule LIKE ?
                          OR m.observations_techniques LIKE ? OR m.defaillances LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY m.date_perception DESC, m.heure_perception DESC, m.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT m.*,
                    d.im AS agent_conducteur_personnel_im,
                    c.im AS chef_de_bord_personnel_im
             FROM materiel_roulant m
             LEFT JOIN personnel d ON m.agent_conducteur_personnel_id = d.id
             LEFT JOIN personnel c ON m.chef_de_bord_personnel_id = c.id
             WHERE m.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO materiel_roulant
                (date_perception, heure_perception, type_materiel, numero_immatriculation, description_vehicule,
                 agent_conducteur_personnel_id, agent_conducteur_im, agent_conducteur_grade, agent_conducteur_nom,
                 chef_de_bord_personnel_id, chef_de_bord_im, chef_de_bord_grade, chef_de_bord_nom,
                 kilometrage_depart, niveau_carburant_depart,
                 agent_verifie, agent_verifie_at, signature_svg,
                 statut, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['date_perception'],
            $data['heure_perception'],
            $data['type_materiel'],
            $data['numero_immatriculation'] ?? null,
            $data['description_vehicule'] ?? null,
            $data['agent_conducteur_personnel_id'] ?? null,
            $data['agent_conducteur_im'] ?? null,
            $data['agent_conducteur_grade'] ?? null,
            $data['agent_conducteur_nom'] ?? null,
            $data['chef_de_bord_personnel_id'] ?? null,
            $data['chef_de_bord_im'] ?? null,
            $data['chef_de_bord_grade'] ?? null,
            $data['chef_de_bord_nom'] ?? null,
            $data['kilometrage_depart'] ?? null,
            $data['niveau_carburant_depart'] ?? null,
            !empty($data['agent_verifie']) ? 1 : 0,
            $data['agent_verifie_at'] ?? null,
            $data['signature_svg'] ?? null,
            $data['statut'] ?? 'En service',
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    /**
     * Update the perception fields only. Reintegration columns and the
     * technical observations/defaillances captured at return are NOT
     * editable here — they are set once via reintegrate() so the one-way
     * transition cannot be bypassed through the generic update path.
     */
    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = [
            'date_perception', 'heure_perception', 'type_materiel', 'numero_immatriculation', 'description_vehicule',
            'agent_conducteur_personnel_id', 'agent_conducteur_im', 'agent_conducteur_grade', 'agent_conducteur_nom',
            'chef_de_bord_personnel_id', 'chef_de_bord_im', 'chef_de_bord_grade', 'chef_de_bord_nom',
            'kilometrage_depart', 'niveau_carburant_depart',
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
            'UPDATE materiel_roulant SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    /**
     * One-way transition: fill the reintegration columns, the return
     * mileage/fuel and the technical observations/defaillances, and set
     * statut to 'Réintégré'. Returns false when the vehicle has already
     * been reintegrated (heure_reintegration IS NOT NULL).
     */
    public static function reintegrate(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE materiel_roulant
             SET heure_reintegration = ?, date_reintegration = ?,
                 kilometrage_retour = ?, niveau_carburant_retour = ?,
                 observations_techniques = ?, defaillances = ?,
                 statut = ?
             WHERE id = ? AND heure_reintegration IS NULL'
        );
        $stmt->execute([
            $data['heure_reintegration'],
            $data['date_reintegration'] ?? null,
            $data['kilometrage_retour'] ?? null,
            $data['niveau_carburant_retour'] ?? null,
            $data['observations_techniques'] ?? null,
            $data['defaillances'] ?? null,
            'Réintégré',
            $id,
        ]);
        return $stmt->rowCount() > 0;
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM materiel_roulant WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
