<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * RegistreEnquete — registre d'enquête / dossier d'enquête (Police Judiciaire).
 *
 * The numero is auto-generated via PlainteSequence::ENQ when the user
 * does not supply one, and is user-overridable.
 * Enquêteur and OPJ are FK references to personnel (same pattern as plainte).
 */
class RegistreEnquete
{
    public const STATUTS = ['EN_COURS', 'SUSPENDUE', 'TRANSMISE', 'CLOTUREE'];

    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = '(r.numero LIKE ? OR r.numero_dossier LIKE ? OR r.nature_infraction LIKE ? OR r.plaignant LIKE ? OR r.mise_en_cause LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s, $s, $s);
        }
        if (!empty($filters['statut'])) {
            $where[] = 'r.statut = ?';
            $args[] = $filters['statut'];
        }

        $sql = 'SELECT r.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom,
                    enq.firstname AS enqueteur_prenoms, enq.lastname AS enqueteur_nom, enq.grade AS enqueteur_grade, enq.im AS enqueteur_im,
                    opj.firstname AS opj_prenoms, opj.lastname AS opj_nom, opj.grade AS opj_grade, opj.im AS opj_im
                FROM registre_enquete r
                LEFT JOIN users u ON r.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id
                LEFT JOIN personnel enq ON r.enqueteur_personnel_id = enq.id
                LEFT JOIN personnel opj ON r.opj_personnel_id = opj.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY r.created_at DESC, r.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT r.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom,
                    enq.firstname AS enqueteur_prenoms, enq.lastname AS enqueteur_nom, enq.grade AS enqueteur_grade, enq.im AS enqueteur_im,
                    opj.firstname AS opj_prenoms, opj.lastname AS opj_nom, opj.grade AS opj_grade, opj.im AS opj_im
             FROM registre_enquete r
             LEFT JOIN users u ON r.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             LEFT JOIN personnel enq ON r.enqueteur_personnel_id = enq.id
             LEFT JOIN personnel opj ON r.opj_personnel_id = opj.id
             WHERE r.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function numeroExists(string $numero, ?int $excludeId = null): bool
    {
        $db = Database::getInstance()->getConnection();
        if ($excludeId !== null) {
            $stmt = $db->prepare('SELECT 1 FROM registre_enquete WHERE numero = ? AND id <> ?');
            $stmt->execute([$numero, $excludeId]);
        } else {
            $stmt = $db->prepare('SELECT 1 FROM registre_enquete WHERE numero = ?');
            $stmt->execute([$numero]);
        }
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO registre_enquete
                (numero, date_ouverture, numero_dossier, nature_infraction, date_lieu_faits,
                 plaignant, mise_en_cause, enqueteur_personnel_id, opj_personnel_id, statut,
                 observations, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['numero'],
            $data['date_ouverture'],
            $data['numero_dossier'] ?? null,
            $data['nature_infraction'],
            $data['date_lieu_faits'] ?? null,
            $data['plaignant'] ?? null,
            $data['mise_en_cause'] ?? null,
            $data['enqueteur_personnel_id'] ?? null,
            $data['opj_personnel_id'] ?? null,
            $data['statut'] ?? 'EN_COURS',
            $data['observations'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE registre_enquete SET
                numero = ?, date_ouverture = ?, numero_dossier = ?, nature_infraction = ?,
                date_lieu_faits = ?, plaignant = ?, mise_en_cause = ?,
                enqueteur_personnel_id = ?, opj_personnel_id = ?, statut = ?, observations = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['numero'],
            $data['date_ouverture'],
            $data['numero_dossier'] ?? null,
            $data['nature_infraction'],
            $data['date_lieu_faits'] ?? null,
            $data['plaignant'] ?? null,
            $data['mise_en_cause'] ?? null,
            $data['enqueteur_personnel_id'] ?? null,
            $data['opj_personnel_id'] ?? null,
            $data['statut'] ?? 'EN_COURS',
            $data['observations'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM registre_enquete WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
