<?php

namespace App\Models;

use App\Database;
use PDO;

/**
 * QrAuthRequest model — short-lived, one-time-use QR-code authentication
 * requests that let a phone approve a desktop login (or vice-versa).
 *
 * The QR code embedded in the request only ever contains the `request_code`
 * — never credentials, passwords, or access tokens.
 */
class QrAuthRequest
{
    /**
     * Generate a cryptographically secure 32-byte (64 hex char) request code.
     */
    public static function generateCode(): string
    {
        return bin2hex(random_bytes(32));
    }

    /**
     * Create a new pending QR auth request.
     *
     * @param array{device_type: string, device_name: string, requester_user_id?: ?int, expires_at: string, client_ip?: ?string, user_agent?: ?string} $data
     */
    public static function create(array $data): array
    {
        $db = Database::getInstance()->getConnection();
        $code = self::generateCode();

        $stmt = $db->prepare(
            'INSERT INTO qr_auth_requests
                (request_code, device_type, device_name, status, requester_user_id,
                 client_ip, user_agent, expires_at)
             VALUES (?, ?, ?, "pending", ?, ?, ?, ?)'
        );
        $stmt->execute([
            $code,
            $data['device_type'],
            $data['device_name'],
            $data['requester_user_id'] ?? null,
            $data['client_ip'] ?? null,
            $data['user_agent'] ?? null,
            $data['expires_at'],
        ]);

        return self::getByCode($code);
    }

    /**
     * Get a request by its code.
     */
    public static function getByCode(string $code): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('SELECT * FROM qr_auth_requests WHERE request_code = ?');
        $stmt->execute([$code]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    /**
     * Update the status of a request.
     */
    public static function updateStatus(string $code, string $status, array $extra = []): bool
    {
        $db = Database::getInstance()->getConnection();
        $fields = ['status = ?'];
        $values = [$status];

        if ($status === 'scanned') {
            $fields[] = 'scanned_at = NOW()';
        }
        if (in_array($status, ['approved', 'rejected', 'cancelled'], true)) {
            $fields[] = 'resolved_at = NOW()';
        }
        if ($status === 'consumed') {
            $fields[] = 'consumed_at = NOW()';
        }

        if (isset($extra['approver_user_id'])) {
            $fields[] = 'approver_user_id = ?';
            $values[] = $extra['approver_user_id'];
        }
        if (isset($extra['issued_access_token'])) {
            $fields[] = 'issued_access_token = ?';
            $values[] = $extra['issued_access_token'];
        }
        if (isset($extra['issued_refresh_token'])) {
            $fields[] = 'issued_refresh_token = ?';
            $values[] = $extra['issued_refresh_token'];
        }

        $values[] = $code;
        $stmt = $db->prepare(
            'UPDATE qr_auth_requests SET ' . implode(', ', $fields) . ' WHERE request_code = ?'
        );
        return $stmt->execute($values);
    }

    /**
     * Mark expired requests as expired (housekeeping).
     */
    public static function expireStale(): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE qr_auth_requests
             SET status = "expired"
             WHERE status = "pending" AND expires_at < NOW()'
        );
        $stmt->execute();
        return $stmt->rowCount();
    }

    /**
     * Check whether a request is still within its validity window.
     */
    public static function isExpired(array $request): bool
    {
        return strtotime($request['expires_at']) < time();
    }
}
