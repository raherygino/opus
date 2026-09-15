<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * ObjetSaisiAttachment — file attached to an ObjetSaisi record.
 * Generic PJ attachment pattern (DB rows only; controller handles file I/O).
 */
class ObjetSaisiAttachment
{
    public static function listForObjet(int $objetId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM attach_objet_saisi WHERE objet_saisi_id = ? ORDER BY created_at ASC'
        );
        $stmt->execute([$objetId]);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM attach_objet_saisi WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function belongsToObjet(int $attachId, int $objetId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT 1 FROM attach_objet_saisi WHERE id = ? AND objet_saisi_id = ?'
        );
        $stmt->execute([$attachId, $objetId]);
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO attach_objet_saisi
                (objet_saisi_id, title, filename, original_filename, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['objet_saisi_id'],
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
        $stmt = $db->prepare('UPDATE attach_objet_saisi SET title = ? WHERE id = ?');
        return $stmt->execute([$title, $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM attach_objet_saisi WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
