<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * RegistreDeferrementAttachment — file attached to a RegistreDeferrement record.
 * Generic PJ attachment pattern (DB rows only; controller handles file I/O).
 */
class RegistreDeferrementAttachment
{
    public static function listForDeferrement(int $deferrementId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM attach_registre_deferrement WHERE registre_deferrement_id = ? ORDER BY created_at ASC'
        );
        $stmt->execute([$deferrementId]);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM attach_registre_deferrement WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function belongsToDeferrement(int $attachId, int $deferrementId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT 1 FROM attach_registre_deferrement WHERE id = ? AND registre_deferrement_id = ?'
        );
        $stmt->execute([$attachId, $deferrementId]);
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO attach_registre_deferrement
                (registre_deferrement_id, title, filename, original_filename, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['registre_deferrement_id'],
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
        $stmt = $db->prepare('UPDATE attach_registre_deferrement SET title = ? WHERE id = ?');
        return $stmt->execute([$title, $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM attach_registre_deferrement WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
