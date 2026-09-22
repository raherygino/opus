<?php

namespace App\Models;

use App\Database;

/**
 * EvenementSurvenuType — user-managed catalog of event types.
 *
 * The evenement_survenu.type_evenement column stores the label string
 * directly (no foreign key), so renaming or deleting a type never
 * breaks historical entries.
 */
class EvenementSurvenuType
{
    public static function getAll(): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->query('SELECT * FROM evenement_survenu_type ORDER BY label ASC');
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM evenement_survenu_type WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function getByLabel(string $label): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM evenement_survenu_type WHERE label = ?');
        $stmt->execute([$label]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function labels(): array
    {
        return array_map(fn($r) => $r['label'], self::getAll());
    }

    public static function create(string $label): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('INSERT INTO evenement_survenu_type (label) VALUES (?)');
        $stmt->execute([trim($label)]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, string $label): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE evenement_survenu_type SET label = ? WHERE id = ?');
        return $stmt->execute([trim($label), $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM evenement_survenu_type WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function exists(string $label): bool
    {
        return self::getByLabel($label) !== null;
    }
}
