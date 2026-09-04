<?php

namespace App\Models;

use App\Database;
use PDO;

class ArmeMunitionsConsommation
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT c.*,
                       a.matricule AS arme_matricule,
                       t.nom AS type_arme_nom,
                       p.im AS agent_im,
                       p.grade AS agent_grade,
                       p.firstname AS agent_firstname,
                       p.lastname AS agent_lastname
                FROM arme_munitions_consommation c
                INNER JOIN arme a ON c.arme_id = a.id
                INNER JOIN type_arme t ON a.type_arme_id = t.id
                LEFT JOIN personnel p ON c.agent_id = p.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['arme_id'])) {
            $sql .= ' AND c.arme_id = ?';
            $params[] = (int) $filters['arme_id'];
        }
        if (!empty($filters['agent_id'])) {
            $sql .= ' AND c.agent_id = ?';
            $params[] = (int) $filters['agent_id'];
        }
        if (!empty($filters['date_from'])) {
            $sql .= ' AND c.date_consommation >= ?';
            $params[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $sql .= ' AND c.date_consommation <= ?';
            $params[] = $filters['date_to'];
        }

        $sql .= ' ORDER BY c.date_consommation DESC, c.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getByArmeId(int $armeId): array
    {
        return self::getAll(['arme_id' => $armeId]);
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO arme_munitions_consommation
                (arme_id, agent_id, armement_id, quantite, date_consommation)
             VALUES (?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['arme_id'],
            $data['agent_id'] ?? null,
            $data['armement_id'] ?? null,
            $data['quantite'],
            $data['date_consommation'],
        ]);
        return (int) $db->lastInsertId();
    }
}
