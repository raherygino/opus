<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * PersonneRechercheePhoto — dedicated image for a PersonneRecherchee record.
 *
 * NOT the generic PJ attachment system. This is a dedicated multi-image
 * table supporting camera capture and gallery selection, with image-
 * specific metadata (caption, capture source, dimensions, sort order).
 */
class PersonneRechercheePhoto
{
    /** Allowed capture sources. */
    public const CAPTURE_SOURCES = ['CAMERA', 'GALLERY'];

    public static function listForPersonne(int $personneId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT * FROM personne_recherchee_photo
             WHERE personne_recherchee_id = ?
             ORDER BY sort_order ASC, id ASC'
        );
        $stmt->execute([$personneId]);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM personne_recherchee_photo WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function belongsToPersonne(int $photoId, int $personneId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT 1 FROM personne_recherchee_photo WHERE id = ? AND personne_recherchee_id = ?'
        );
        $stmt->execute([$photoId, $personneId]);
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO personne_recherchee_photo
                (personne_recherchee_id, caption, filename, original_filename,
                 mime_type, file_size, width, height, capture_source, sort_order)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['personne_recherchee_id'],
            $data['caption'] ?? null,
            $data['filename'],
            $data['original_filename'],
            $data['mime_type'] ?? null,
            $data['file_size'] ?? null,
            $data['width'] ?? null,
            $data['height'] ?? null,
            $data['capture_source'] ?? null,
            $data['sort_order'] ?? 0,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function updateCaption(int $id, ?string $caption): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE personne_recherchee_photo SET caption = ? WHERE id = ?');
        return $stmt->execute([$caption, $id]);
    }

    public static function updateSortOrder(int $id, int $sortOrder): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE personne_recherchee_photo SET sort_order = ? WHERE id = ?');
        return $stmt->execute([$sortOrder, $id]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM personne_recherchee_photo WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function nextSortOrder(int $personneId): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT COALESCE(MAX(sort_order), -1) + 1 AS next
             FROM personne_recherchee_photo WHERE personne_recherchee_id = ?'
        );
        $stmt->execute([$personneId]);
        return (int) $stmt->fetchColumn();
    }
}
