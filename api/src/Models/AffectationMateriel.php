<?php

namespace App\Models;

use App\Database;
use PDO;

class AffectationMateriel
{
    public const STATUTS = ['Assigné', 'Réintégré'];

    /**
     * List all assignments with optional filters. Each assignment is
     * returned with its agent snapshot and its line items (lignes).
     */
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT a.*,
                       p.im AS agent_personnel_im
                FROM affectation_materiel a
                LEFT JOIN personnel p ON a.agent_personnel_id = p.id
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
            if ($filters['statut'] === 'assigne') {
                $sql .= ' AND a.statut = ?';
                $params[] = 'Assigné';
            } elseif ($filters['statut'] === 'reintegre') {
                $sql .= ' AND a.statut = ?';
                $params[] = 'Réintégré';
            }
        }
        if (!empty($filters['agent_personnel_id'])) {
            $sql .= ' AND a.agent_personnel_id = ?';
            $params[] = (int) $filters['agent_personnel_id'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (a.agent_nom LIKE ? OR a.agent_im LIKE ? OR a.observations LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY a.date_perception DESC, a.heure_perception DESC, a.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $rows = $stmt->fetchAll();

        // Attach line items to each assignment.
        foreach ($rows as &$row) {
            $row['lignes'] = self::getLignes((int) $row['id']);
        }
        unset($row);

        return $rows;
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT a.*,
                    p.im AS agent_personnel_im
             FROM affectation_materiel a
             LEFT JOIN personnel p ON a.agent_personnel_id = p.id
             WHERE a.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        if (!$row) {
            return null;
        }
        $row['lignes'] = self::getLignes($id);
        return $row;
    }

    /**
     * Line items for a given assignment.
     */
    public static function getLignes(int $affectationId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT l.* FROM affectation_materiel_ligne l
             WHERE l.affectation_id = ?
             ORDER BY l.id ASC'
        );
        $stmt->execute([$affectationId]);
        return $stmt->fetchAll();
    }

    /**
     * Create an assignment header. Line items must be inserted separately
     * via createLigne() inside the same transaction.
     */
    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO affectation_materiel
                (agent_personnel_id, agent_im, agent_grade, agent_nom,
                 date_perception, heure_perception, statut, observations,
                 agent_verifie, agent_verifie_at, signature_svg,
                 created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['agent_personnel_id'],
            $data['agent_im'] ?? null,
            $data['agent_grade'] ?? null,
            $data['agent_nom'] ?? null,
            $data['date_perception'],
            $data['heure_perception'],
            $data['statut'] ?? 'Assigné',
            $data['observations'] ?? null,
            !empty($data['agent_verifie']) ? 1 : 0,
            $data['agent_verifie_at'] ?? null,
            $data['signature_svg'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    /**
     * Insert a single line item.
     */
    public static function createLigne(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO affectation_materiel_ligne
                (affectation_id, type_materiel_id, type_materiel_nom, numero_materiel, etat_emport, etat_reintegration)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['affectation_id'],
            $data['type_materiel_id'],
            $data['type_materiel_nom'],
            $data['numero_materiel'],
            $data['etat_emport'] ?? null,
            $data['etat_reintegration'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    /**
     * Update the perception fields only. Reintegration columns and line
     * items are NOT editable here — reintegration is a one-way transition
     * via reintegrate(), and line items are managed via replaceLignes().
     */
    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = [
            'agent_personnel_id', 'agent_im', 'agent_grade', 'agent_nom',
            'date_perception', 'heure_perception', 'observations',
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
            'UPDATE affectation_materiel SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    /**
     * Replace all line items for an assignment (used on update).
     * Deletes existing lignes and inserts the new set.
     */
    public static function replaceLignes(int $affectationId, array $lignes): void
    {
        $db = Database::getInstance()->getConnection();
        $db->prepare('DELETE FROM affectation_materiel_ligne WHERE affectation_id = ?')
            ->execute([$affectationId]);

        foreach ($lignes as $ligne) {
            self::createLigne([
                'affectation_id'    => $affectationId,
                'type_materiel_id'  => $ligne['type_materiel_id'],
                'type_materiel_nom' => $ligne['type_materiel_nom'],
                'numero_materiel'   => $ligne['numero_materiel'],
                'etat_emport'       => $ligne['etat_emport'] ?? null,
                'etat_reintegration' => $ligne['etat_reintegration'] ?? null,
            ]);
        }
    }

    /**
     * One-way transition: fill the reintegration columns and set statut to
     * 'Réintégré'. Also fills etat_reintegration on each line item.
     * Returns false when the assignment has already been reintegrated.
     */
    public static function reintegrate(int $id, array $data, array $ligneEtats = []): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE affectation_materiel
             SET heure_reintegration = ?, date_reintegration = ?, statut = ?
             WHERE id = ? AND heure_reintegration IS NULL'
        );
        $stmt->execute([
            $data['heure_reintegration'],
            $data['date_reintegration'] ?? null,
            'Réintégré',
            $id,
        ]);
        $updated = $stmt->rowCount() > 0;

        if ($updated) {
            // Update etat_reintegration on each line item.
            foreach ($ligneEtats as $ligneId => $etat) {
                $db->prepare(
                    'UPDATE affectation_materiel_ligne SET etat_reintegration = ? WHERE id = ? AND affectation_id = ?'
                )->execute([$etat, (int) $ligneId, $id]);
            }
        }

        return $updated;
    }

    /**
     * Check whether a numero_materiel is currently assigned (statut =
     * 'Assigné') in any OTHER assignment. Used to enforce business rule 7:
     * a material currently assigned should not be simultaneously assigned
     * to another agent.
     *
     * @param string $numeroMateriel The material ID to check
     * @param int|null $excludeAffectationId Exclude this assignment (for updates)
     */
    public static function isNumeroMaterielActivementAffecte(string $numeroMateriel, ?int $excludeAffectationId = null): bool
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT COUNT(*) FROM affectation_materiel_ligne l
                INNER JOIN affectation_materiel a ON l.affectation_id = a.id
                WHERE l.numero_materiel = ? AND a.statut = ?';
        $params = [$numeroMateriel, 'Assigné'];

        if ($excludeAffectationId !== null) {
            $sql .= ' AND a.id != ?';
            $params[] = $excludeAffectationId;
        }

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return (int) $stmt->fetchColumn() > 0;
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM affectation_materiel WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
