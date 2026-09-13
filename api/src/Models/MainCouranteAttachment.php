<?php

namespace App\Models;

use App\Database;

class MainCouranteAttachment
{
    public static function getByMainCouranteId(int $mainCouranteId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM attach_main_courante WHERE main_courante_id = ? ORDER BY created_at DESC'
        );
        $stmt->execute([$mainCouranteId]);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM attach_main_courante WHERE id = ?');
        $stmt->execute([$id]);
        $attach = $stmt->fetch();
        return $attach ?: null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO attach_main_courante (main_courante_id, title, filename, original_filename, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['main_courante_id'],
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
            'UPDATE attach_main_courante SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM attach_main_courante WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function belongsToMainCourante(int $attachId, int $mainCouranteId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT COUNT(*) FROM attach_main_courante WHERE id = ? AND main_courante_id = ?'
        );
        $stmt->execute([$attachId, $mainCouranteId]);
        return $stmt->fetchColumn() > 0;
    }
}
