<?php

namespace App\Models;

use App\Database;
use PDO;

class Personnel
{
    /**
     * Strip the code_secret_hash from a personnel row (or list of rows)
     * before returning it to the client. The hash must never be exposed.
     */
    public static function stripSecret(array $row): array
    {
        unset($row['code_secret_hash']);
        return $row;
    }

    public static function stripSecretList(array $rows): array
    {
        return array_map([self::class, 'stripSecret'], $rows);
    }

    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT p.*,
                       COALESCE(
                           (SELECT mp.type_mouvement
                            FROM mouvement_personnel mp
                            WHERE mp.personnel_id = p.id
                              AND mp.retour = "Non"
                            ORDER BY mp.created_at DESC
                            LIMIT 1),
                           "En service"
                       ) AS status
                FROM personnel p WHERE 1=1';
        $params = [];

        if (!empty($filters['status'])) {
            if ($filters['status'] === 'active' || $filters['status'] === 'En service') {
                $sql .= ' AND NOT EXISTS (SELECT 1 FROM mouvement_personnel mp WHERE mp.personnel_id = p.id AND mp.retour = "Non")';
            } else {
                $sql .= ' AND EXISTS (SELECT 1 FROM mouvement_personnel mp WHERE mp.personnel_id = p.id AND mp.retour = "Non" AND mp.type_mouvement = ?)';
                $params[] = $filters['status'];
            }
        }
        if (!empty($filters['grade'])) {
            $sql .= ' AND grade LIKE ?';
            $params[] = '%' . $filters['grade'] . '%';
        }
        if (!empty($filters['affectation'])) {
            $sql .= ' AND affectation LIKE ?';
            $params[] = '%' . $filters['affectation'] . '%';
        }
        if (!empty($filters['search'])) {
            $sql .= ' AND (lastname LIKE ? OR firstname LIKE ? OR im LIKE ?)';
            $search = '%' . $filters['search'] . '%';
            $params[] = $search;
            $params[] = $search;
            $params[] = $search;
        }

        $sql .= ' ORDER BY lastname ASC, firstname ASC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $rows = $stmt->fetchAll();
        return self::stripSecretList($rows);
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT p.*,
                    COALESCE(
                        (SELECT mp.type_mouvement
                         FROM mouvement_personnel mp
                         WHERE mp.personnel_id = p.id
                           AND mp.retour = "Non"
                         ORDER BY mp.created_at DESC
                         LIMIT 1),
                        "En service"
                    ) AS status
             FROM personnel p WHERE p.id = ?'
        );
        $stmt->execute([$id]);
        $person = $stmt->fetch();
        if (!$person) return null;
        return self::stripSecret($person);
    }

    public static function getByIM(string $im): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT p.*,
                    COALESCE(
                        (SELECT mp.type_mouvement
                         FROM mouvement_personnel mp
                         WHERE mp.personnel_id = p.id
                           AND mp.retour = "Non"
                         ORDER BY mp.created_at DESC
                         LIMIT 1),
                        "En service"
                    ) AS status
             FROM personnel p WHERE p.im = ?'
        );
        $stmt->execute([$im]);
        $person = $stmt->fetch();
        if (!$person) return null;
        return self::stripSecret($person);
    }

    public static function getAvailableForUser(): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT p.* FROM personnel p
                LEFT JOIN users u ON p.id = u.personnel_id
                WHERE u.id IS NULL';
        $stmt = $db->query($sql);
        return self::stripSecretList($stmt->fetchAll());
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO personnel (im, grade, lastname, firstname, affectation, phone, address, photo, signature, code_secret_hash)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $codeSecretHash = null;
        if (!empty($data['code_secret'])) {
            $codeSecretHash = password_hash((string) $data['code_secret'], PASSWORD_BCRYPT);
        } elseif (!empty($data['code_secret_hash'])) {
            $codeSecretHash = $data['code_secret_hash'];
        }
        $stmt->execute([
            $data['im'],
            $data['grade'],
            $data['lastname'],
            $data['firstname'],
            $data['affectation'] ?? null,
            $data['phone'] ?? null,
            $data['address'] ?? null,
            $data['photo'] ?? null,
            $data['signature'] ?? null,
            $codeSecretHash,
        ]);

        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = [];
        $values = [];

        $allowed = ['im', 'grade', 'lastname', 'firstname', 'affectation', 'phone', 'address', 'photo', 'thumbnail', 'signature', 'signature_svg'];
        foreach ($allowed as $field) {
            if (array_key_exists($field, $data)) {
                $fields[] = "$field = ?";
                $values[] = $data[$field];
            }
        }

        // code_secret_hash is handled separately via setCodeSecret() to
        // ensure it is always hashed — never accept a raw hash from client data.
        if (empty($fields)) {
            return false;
        }

        $values[] = $id;
        $stmt = $db->prepare(
            'UPDATE personnel SET ' . implode(', ', $fields) . ' WHERE id = ?'
        );
        return $stmt->execute($values);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM personnel WHERE id = ?');
        return $stmt->execute([$id]);
    }

    // ─── Code secret (Armement identity verification) ───────────────

    /**
     * Hash and store the personnel's secret code. The plaintext is never
     * stored — only a bcrypt hash. Pass null to clear the code.
     */
    public static function setCodeSecret(int $id, ?string $code): bool
    {
        $db = Database::getInstance()->getConnection();
        $hash = ($code !== null && $code !== '') ? password_hash($code, PASSWORD_BCRYPT) : null;
        $stmt = $db->prepare('UPDATE personnel SET code_secret_hash = ? WHERE id = ?');
        return $stmt->execute([$hash, $id]);
    }

    /**
     * Verify the given plaintext code against the stored hash.
     * Returns false when no code is set or the code does not match.
     */
    public static function verifyCodeSecret(int $id, string $code): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT code_secret_hash FROM personnel WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        if (!$row || empty($row['code_secret_hash'])) {
            return false;
        }
        return password_verify($code, $row['code_secret_hash']);
    }

    /**
     * Whether this personnel has a code secret set.
     */
    public static function hasCodeSecret(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT code_secret_hash FROM personnel WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row && !empty($row['code_secret_hash']);
    }
}
