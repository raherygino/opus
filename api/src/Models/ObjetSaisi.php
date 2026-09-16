<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * ObjetSaisi — seized object (Police Judiciaire, OBJET SAISI tab).
 */
class ObjetSaisi
{
    /** Predefined object types. */
    public const TYPES = [
        'TELEPHONE',
        'ORDINATEUR',
        'VEHICULE',
        'DOCUMENT',
        'ARGENT',
        'ARME',
        'EFFETS_PERSONNELS',
        'AUTRE',
    ];

    public const TYPE_LABELS = [
        'TELEPHONE' => 'Téléphone',
        'ORDINATEUR' => 'Ordinateur',
        'VEHICULE' => 'Véhicule',
        'DOCUMENT' => 'Document',
        'ARGENT' => 'Argent',
        'ARME' => 'Arme',
        'EFFETS_PERSONNELS' => 'Effets personnels',
        'AUTRE' => 'Autre',
    ];

    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['type_objet'])) {
            $where[] = 'o.type_objet = ?';
            $args[] = $filters['type_objet'];
        }
        if (!empty($filters['search'])) {
            $where[] = '(o.numero_dossier LIKE ? OR o.motif LIKE ? OR o.proprietaire LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s);
        }

        $sql = 'SELECT o.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM objet_saisi o
                LEFT JOIN users u ON o.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY o.created_at DESC, o.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT o.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM objet_saisi o
             LEFT JOIN users u ON o.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE o.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO objet_saisi (numero_dossier, motif, type_objet, proprietaire, created_by)
             VALUES (?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['numero_dossier'] ?? null,
            $data['motif'],
            $data['type_objet'],
            $data['proprietaire'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE objet_saisi SET numero_dossier = ?, motif = ?, type_objet = ?, proprietaire = ? WHERE id = ?'
        );
        return $stmt->execute([
            $data['numero_dossier'] ?? null,
            $data['motif'],
            $data['type_objet'],
            $data['proprietaire'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM objet_saisi WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
