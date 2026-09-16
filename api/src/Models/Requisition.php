<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * Requisition — judicial requisition (Police Judiciaire).
 *
 * Tracks requisitions issued to external services (TPH, médecin légiste,
 * CIM, autre). The numero is auto-generated via PlainteSequence::REQ
 * when the user does not supply one, and is user-overridable.
 */
class Requisition
{
    /** Allowed requisition types. */
    public const TYPES = ['TPH', 'MEDECIN_LEGISTE', 'CIM', 'AUTRE'];

    /** Human-readable labels for each type. */
    public const TYPE_LABELS = [
        'TPH'            => 'TPH',
        'MEDECIN_LEGISTE' => 'Médecin légiste',
        'CIM'            => 'CIM',
        'AUTRE'          => 'Autre',
    ];

    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['type'])) {
            $where[] = 'r.type = ?';
            $args[] = $filters['type'];
        }
        if (!empty($filters['search'])) {
            $where[] = '(r.numero LIKE ? OR r.affaire LIKE ? OR r.nom_substitut LIKE ? OR r.opj LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s, $s);
        }
        if (!empty($filters['date_from'])) {
            $where[] = 'r.date_requisition >= ?';
            $args[] = $filters['date_from'];
        }
        if (!empty($filters['date_to'])) {
            $where[] = 'r.date_requisition <= ?';
            $args[] = $filters['date_to'];
        }

        $sql = 'SELECT r.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM requisition r
                LEFT JOIN users u ON r.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY r.date_requisition DESC, r.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT r.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM requisition r
             LEFT JOIN users u ON r.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
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
            $stmt = $db->prepare('SELECT 1 FROM requisition WHERE numero = ? AND id <> ?');
            $stmt->execute([$numero, $excludeId]);
        } else {
            $stmt = $db->prepare('SELECT 1 FROM requisition WHERE numero = ?');
            $stmt->execute([$numero]);
        }
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO requisition
                (type, date_requisition, numero, numero_ttr, nom_substitut, affaire, numero_dossier, opj, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['type'],
            $data['date_requisition'],
            $data['numero'],
            $data['numero_ttr'] ?? null,
            $data['nom_substitut'] ?? null,
            $data['affaire'],
            $data['numero_dossier'] ?? null,
            $data['opj'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE requisition SET
                type = ?, date_requisition = ?, numero = ?, numero_ttr = ?,
                nom_substitut = ?, affaire = ?, numero_dossier = ?, opj = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['type'],
            $data['date_requisition'],
            $data['numero'],
            $data['numero_ttr'] ?? null,
            $data['nom_substitut'] ?? null,
            $data['affaire'],
            $data['numero_dossier'] ?? null,
            $data['opj'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM requisition WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
