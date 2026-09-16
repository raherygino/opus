<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * RenseignementPjAttachment — file attached to a RenseignementPj record.
 * Generic PJ attachment pattern (DB rows only; controller handles file I/O).
 */
class RenseignementPjAttachment
{
    public static function listForRenseignement(int $renseignementId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM attach_renseignement_pj WHERE renseignement_id = ? ORDER BY created_at ASC'
        );
        $stmt->execute([$renseignementId]);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM attach_renseignement_pj WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function belongsToRenseignement(int $attachId, int $renseignementId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT 1 FROM attach_renseignement_pj WHERE id = ? AND renseignement_id = ?'
        );
        $stmt->execute([$attachId, $renseignementId]);
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO attach_renseignement_pj
                (renseignement_id, title, filename, original_filename, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['renseignement_id'],
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
        $stmt = $db->prepare('UPDATE attach_renseignement_pj SET title = ? WHERE id = ?');
        return $stmt->execute([$title, $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM attach_renseignement_pj WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
