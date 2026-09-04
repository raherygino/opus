<?php

namespace App\Models;

use App\Database;
use PDO;

class Arme
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT a.*, t.nom AS type_arme_nom,
                       t.munitions_stock AS type_arme_munitions_stock
                FROM arme a
                INNER JOIN type_arme t ON a.type_arme_id = t.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['type_arme_id'])) {
            $sql .= ' AND a.type_arme_id = ?';
            $params[] = (int) $filters['type_arme_id'];
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (a.matricule LIKE ? OR t.nom LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY t.nom ASC, a.matricule ASC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT a.*, t.nom AS type_arme_nom,
                    t.munitions_stock AS type_arme_munitions_stock
             FROM arme a
             INNER JOIN type_arme t ON a.type_arme_id = t.id
             WHERE a.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function getByMatricule(string $matricule): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM arme WHERE matricule = ?');
        $stmt->execute([$matricule]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO arme (type_arme_id, matricule, munitions_stock)
             VALUES (?, ?, ?)'
        );
        $stmt->execute([
            $data['type_arme_id'],
            $data['matricule'],
            $data['munitions_stock'] ?? 0,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['type_arme_id', 'matricule', 'munitions_stock'];
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
            'UPDATE arme SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    /**
     * Atomically decrease the ammunition stock for the weapon TYPE of
     * the given arme. Munitions stock is managed at the type_arme level
     * (not per individual arme) because all weapons of the same type
     * share the same caliber/munition pool.
     *
     * Returns true when the stock was actually decreased, false when
     * the arme does not exist or the type's stock was insufficient.
     *
     * Must be called inside a transaction by the caller.
     */
    public static function decreaseStock(int $armeId, int $quantite): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT type_arme_id FROM arme WHERE id = ?');
        $stmt->execute([$armeId]);
        $typeArmeId = $stmt->fetchColumn();
        if (!$typeArmeId) {
            return false;
        }
        return TypeArme::decreaseStock((int) $typeArmeId, $quantite);
    }

    /**
     * Number of perceptions (armement) referencing this weapon — used to
     * prevent deleting a weapon that is still referenced by historical
     * perceptions. The FK on armement.arme_id is ON DELETE SET NULL, but
     * we deliberately reject the deletion instead so the historical link
     * is never silently lost.
     */
    public static function countArmements(int $id): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT COUNT(*) FROM armement WHERE arme_id = ?');
        $stmt->execute([$id]);
        return (int) $stmt->fetchColumn();
    }

    /**
     * Number of consumption history rows referencing this weapon.
     */
    public static function countConsommations(int $id): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT COUNT(*) FROM arme_munitions_consommation WHERE arme_id = ?');
        $stmt->execute([$id]);
        return (int) $stmt->fetchColumn();
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM arme WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
