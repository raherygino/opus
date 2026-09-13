<?php

namespace App\Models;

use App\Database;

class MainCouranteCategorie
{
    public static function getAll(): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->query('SELECT * FROM main_courante_categorie ORDER BY label ASC');
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM main_courante_categorie WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function getByLabel(string $label): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM main_courante_categorie WHERE label = ?');
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
        $stmt = $db->prepare('INSERT INTO main_courante_categorie (label) VALUES (?)');
        $stmt->execute([trim($label)]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, string $label): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE main_courante_categorie SET label = ? WHERE id = ?');
        return $stmt->execute([trim($label), $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM main_courante_categorie WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function exists(string $label): bool
    {
        return self::getByLabel($label) !== null;
    }
}
