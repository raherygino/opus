<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * MandatAttachment — file attached to a Mandat record.
 * Generic PJ attachment pattern (DB rows only; controller handles file I/O).
 */
class MandatAttachment
{
    public static function listForMandat(int $mandatId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM attach_mandat WHERE mandat_id = ? ORDER BY created_at ASC'
        );
        $stmt->execute([$mandatId]);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM attach_mandat WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function belongsToMandat(int $attachId, int $mandatId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT 1 FROM attach_mandat WHERE id = ? AND mandat_id = ?'
        );
        $stmt->execute([$attachId, $mandatId]);
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO attach_mandat
                (mandat_id, title, filename, original_filename, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['mandat_id'],
            $data['title'],
            $data['filename'],
            $data['original_filename'],
            $data['mime_type'] ?? null,
            $data['file_size'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function updateTitle(int $id, string $title): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE attach_mandat SET title = ? WHERE id = ?');
        return $stmt->execute([$title, $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM attach_mandat WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
