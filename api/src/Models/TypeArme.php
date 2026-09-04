<?php

namespace App\Models;

use App\Database;
use PDO;

class TypeArme
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT t.* FROM type_arme t WHERE 1=1';
        $params = [];

        if (!empty($filters['search'])) {
            $sql .= ' AND (t.nom LIKE ? OR t.description LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY t.nom ASC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM type_arme WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function getByNom(string $nom): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM type_arme WHERE nom = ?');
        $stmt->execute([$nom]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO type_arme (nom, description, munitions_stock) VALUES (?, ?, ?)'
        );
        $stmt->execute([
            $data['nom'],
            $data['description'] ?? null,
            $data['munitions_stock'] ?? 0,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['nom', 'description', 'munitions_stock'];
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
            'UPDATE type_arme SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    /**
     * Atomically decrease the ammunition stock of a weapon TYPE by
     * $quantite, but ONLY if enough stock is available. Returns true
     * when the stock was actually decreased (rowCount > 0), false when
     * the type does not exist or the stock was insufficient. The
     * conditional UPDATE (WHERE munitions_stock >= ?) prevents the
     * stock from going negative and prevents race conditions between
     * concurrent consumers — MySQL takes a row lock on the matching
     * row, so two concurrent consumers cannot both succeed when only
     * one unit is left.
     *
     * Munitions stock lives at the type_arme level (not per individual
     * arme) because all weapons of the same type share the same
     * caliber/munition pool.
     *
     * Must be called inside a transaction by the caller so the stock
     * deduction and the consumption history insert are atomic together.
     */
    public static function decreaseStock(int $typeArmeId, int $quantite): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE type_arme
             SET munitions_stock = munitions_stock - ?
             WHERE id = ? AND munitions_stock >= ?'
        );
        $stmt->execute([$quantite, $typeArmeId, $quantite]);
        return $stmt->rowCount() > 0;
    }

    /**
     * Number of weapons (arme) using this type — used to prevent deleting
     * a type that is still referenced.
     */
    public static function countArmes(int $id): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT COUNT(*) FROM arme WHERE type_arme_id = ?');
        $stmt->execute([$id]);
        return (int) $stmt->fetchColumn();
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM type_arme WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
