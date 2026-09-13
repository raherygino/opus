<?php

namespace App\Models;

use App\Database;

class MaterielRoulantAttachment
{
    public static function getByMaterielRoulantId(int $materielRoulantId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM attach_materiel_roulant WHERE materiel_roulant_id = ? ORDER BY created_at DESC'
        );
        $stmt->execute([$materielRoulantId]);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM attach_materiel_roulant WHERE id = ?');
        $stmt->execute([$id]);
        $attach = $stmt->fetch();
        return $attach ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO attach_materiel_roulant (materiel_roulant_id, title, filename, original_filename, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['materiel_roulant_id'],
            $data['title'],
            $data['filename'],
            $data['original_filename'] ?? $data['filename'],
            $data['mime_type'] ?? null,
            $data['file_size'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['title', 'filename', 'original_filename', 'mime_type', 'file_size'];
        foreach ($allowed as $field) {
            if (isset($data[$field])) {
                $fields[] = "$field = ?";
                $values[] = $data[$field];
            }
        }

        if (empty($fields)) {
            return false;
        }

        $values[] = $id;
        $stmt = $db->prepare(
            'UPDATE attach_materiel_roulant SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM attach_materiel_roulant WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function belongsToMaterielRoulant(int $attachId, int $materielRoulantId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT COUNT(*) FROM attach_materiel_roulant WHERE id = ? AND materiel_roulant_id = ?'
        );
        $stmt->execute([$attachId, $materielRoulantId]);
        return $stmt->fetchColumn() > 0;
    }
}
