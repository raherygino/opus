<?php

namespace App\Models;

use App\Database;
use PDO;

class Armement
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT a.*,
                       p.im AS agent_preneur_personnel_im
                FROM armement a
                LEFT JOIN personnel p ON a.agent_preneur_personnel_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['date_from'])) {
            $sql .= ' AND a.date_perception >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND a.date_perception <= ?';
            $params[] = $filters['date_to'];
        }
        if (!empty($filters['statut'])) {
            if ($filters['statut'] === 'en_cours') {
                $sql .= ' AND a.heure_reintegration IS NULL';
            } elseif ($filters['statut'] === 'reintegree') {
                $sql .= ' AND a.heure_reintegration IS NOT NULL';
            }
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (a.agent_preneur_nom LIKE ? OR a.agent_preneur_im LIKE ? OR a.type_arme LIKE ? OR a.matricule_arme LIKE ? OR a.secteur_mission LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY a.date_perception DESC, a.heure_perception DESC, a.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT a.*,
                    p.im AS agent_preneur_personnel_im
             FROM armement a
             LEFT JOIN personnel p ON a.agent_preneur_personnel_id = p.id
             WHERE a.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO armement
                (date_perception, heure_perception,
                 agent_preneur_personnel_id, agent_preneur_im, agent_preneur_grade, agent_preneur_nom,
                 type_arme, matricule_arme, munitions, secteur_mission, etat_perception,
                 agent_verifie, agent_verifie_at, signature_svg,
                 latitude, longitude,
                 created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['date_perception'],
            $data['heure_perception'],
            $data['agent_preneur_personnel_id'] ?? null,
            $data['agent_preneur_im'] ?? null,
            $data['agent_preneur_grade'] ?? null,
            $data['agent_preneur_nom'] ?? null,
            $data['type_arme'],
            $data['matricule_arme'],
            $data['munitions'] ?? null,
            $data['secteur_mission'] ?? null,
            $data['etat_perception'] ?? null,
            !empty($data['agent_verifie']) ? 1 : 0,
            $data['agent_verifie_at'] ?? null,
            $data['signature_svg'] ?? null,
            $data['latitude'] ?? null,
            $data['longitude'] ?? null,
            $data['created_by'] ?? null,
        ]);

        return (int) $db->lastInsertId();
    }

    /**
     * Update the perception fields only. Reintegration columns are handled
     * exclusively by reintegrate() so the one-way transition cannot be
     * bypassed through the generic update path.
     */
    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = [
            'date_perception', 'heure_perception',
            'agent_preneur_personnel_id', 'agent_preneur_im', 'agent_preneur_grade', 'agent_preneur_nom',
            'type_arme', 'matricule_arme', 'munitions', 'secteur_mission', 'etat_perception',
            'latitude', 'longitude',
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
            'UPDATE armement SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    /**
     * Fill the reintegration columns. Only allowed while the weapon is still
     * "en cours de perception" (heure_reintegration IS NULL) — returns false
     * when the weapon has already been reintegrated.
     */
    public static function reintegrate(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE armement
             SET heure_reintegration = ?, date_reintegration = ?, etat_reintegration = ?, munitions_consommees = ?,
                 reintegration_latitude = ?, reintegration_longitude = ?
             WHERE id = ? AND heure_reintegration IS NULL'
        );
        $stmt->execute([
            $data['heure_reintegration'],
            $data['date_reintegration'] ?? null,
            $data['etat_reintegration'] ?? null,
            $data['munitions_consommees'] ?? null,
            $data['reintegration_latitude'] ?? null,
            $data['reintegration_longitude'] ?? null,
            $id,
        ]);
        return $stmt->rowCount() > 0;
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM armement WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
