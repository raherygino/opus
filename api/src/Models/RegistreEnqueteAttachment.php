<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * RegistreEnqueteAttachment — file attached to a RegistreEnquete record.
 * Generic PJ attachment pattern (DB rows only; controller handles file I/O).
 */
class RegistreEnqueteAttachment
{
    public static function listForEnquete(int $enqueteId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM attach_registre_enquete WHERE registre_enquete_id = ? ORDER BY created_at ASC'
        );
        $stmt->execute([$enqueteId]);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM attach_registre_enquete WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function belongsToEnquete(int $attachId, int $enqueteId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT 1 FROM attach_registre_enquete WHERE id = ? AND registre_enquete_id = ?'
        );
        $stmt->execute([$attachId, $enqueteId]);
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO attach_registre_enquete
                (registre_enquete_id, title, filename, original_filename, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['registre_enquete_id'],
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
        $stmt = $db->prepare('UPDATE attach_registre_enquete SET title = ? WHERE id = ?');
        return $stmt->execute([$title, $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM attach_registre_enquete WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
