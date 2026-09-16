<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * ObjetTrouve — found object (Police Judiciaire, OBJET TROUVÉ tab).
 */
class ObjetTrouve
{
    /** Discovery motives. */
    public const MOTIFS = [
        'REQUISITION',
        'SUR_PERSONNE',
        'PERQUISITION',
    ];

    public const MOTIF_LABELS = [
        'REQUISITION' => 'Réquisition',
        'SUR_PERSONNE' => 'Sur une personne',
        'PERQUISITION' => 'Perquisition',
    ];

    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['motif_decouverte'])) {
            $where[] = 'o.motif_decouverte = ?';
            $args[] = $filters['motif_decouverte'];
        }
        if (isset($filters['restitution']) && $filters['restitution'] !== '') {
            $where[] = 'o.restitution = ?';
            $args[] = (int) $filters['restitution'];
        }
        if (!empty($filters['search'])) {
            $where[] = 'o.affaire LIKE ?';
            $args[] = '%' . $filters['search'] . '%';
        }

        $sql = 'SELECT o.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM objet_trouve o
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
             FROM objet_trouve o
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
            'INSERT INTO objet_trouve (affaire, motif_decouverte, restitution, created_by)
             VALUES (?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['affaire'],
            $data['motif_decouverte'],
            !empty($data['restitution']) ? 1 : 0,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE objet_trouve SET affaire = ?, motif_decouverte = ?, restitution = ? WHERE id = ?'
        );
        return $stmt->execute([
            $data['affaire'],
            $data['motif_decouverte'],
            !empty($data['restitution']) ? 1 : 0,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM objet_trouve WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
