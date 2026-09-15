<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * RequisitionAttachment — file attached to a Requisition record.
 *
 * Reuses the generic PJ attachment pattern (ConvocationAttachment):
 * the model only manages DB rows; the controller handles file I/O.
 */
class RequisitionAttachment
{
    public static function listForRequisition(int $requisitionId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM attach_requisition WHERE requisition_id = ? ORDER BY created_at ASC'
        );
        $stmt->execute([$requisitionId]);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM attach_requisition WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function belongsToRequisition(int $attachId, int $requisitionId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT 1 FROM attach_requisition WHERE id = ? AND requisition_id = ?'
        );
        $stmt->execute([$attachId, $requisitionId]);
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO attach_requisition
                (requisition_id, title, filename, original_filename, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['requisition_id'],
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
        $stmt = $db->prepare('UPDATE attach_requisition SET title = ? WHERE id = ?');
        return $stmt->execute([$title, $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM attach_requisition WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
