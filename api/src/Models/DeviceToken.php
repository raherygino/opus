<?php

namespace App\Models;

use App\Database;
use PDO;

/**
 * DeviceToken model — stores FCM registration tokens per user/device.
 *
 * Used by FcmSender to deliver push notifications to Android devices.
 */
class DeviceToken
{
    /**
     * Get all active tokens for a given user.
     *
     * @return array<int, array{id: int, user_id: int, token: string, device_name: ?string}>
     */
    public static function getActiveByUserId(int $userId): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT id, user_id, token, device_name
             FROM device_tokens
             WHERE user_id = ? AND is_active = 1'
        );
        $stmt->execute([$userId]);
        return $stmt->fetchAll();
    }

    /**
     * Get all active tokens for a list of user IDs.
     *
     * @param int[] $userIds
     * @return array<int, array{id: int, user_id: int, token: string, device_name: ?string}>
     */
    public static function getActiveByUserIds(array $userIds): array
    {
        if (empty($userIds)) {
            return [];
        }
        $db = Database::getInstance()->getConnection();
        $placeholders = implode(',', array_fill(0, count($userIds), '?'));
        $stmt = $db->prepare(
            "SELECT id, user_id, token, device_name
             FROM device_tokens
             WHERE user_id IN ($placeholders) AND is_active = 1"
        );
        $stmt->execute($userIds);
        return $stmt->fetchAll();
    }

    /**
     * Get all active tokens (for broadcast notifications to admins).
     *
     * @return array<int, array{id: int, user_id: int, token: string, device_name: ?string}>
     */
    public static function getAllActive(): array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->query(
            'SELECT id, user_id, token, device_name
             FROM device_tokens
             WHERE is_active = 1'
        );
        return $stmt->fetchAll();
    }

    /**
     * Get all active tokens belonging to admin users (SUPER_ADMIN / STATION_ADMIN).
     *
     * @return array<int, array{id: int, user_id: int, token: string, device_name: ?string}>
     */
    public static function getActiveForAdmins(): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT dt.id, dt.user_id, dt.token, dt.device_name
                FROM device_tokens dt
                JOIN users u ON dt.user_id = u.id
                JOIN roles r ON u.role_id = r.id
                WHERE dt.is_active = 1
                  AND u.is_active = 1
                  AND r.code IN ("SUPER_ADMIN", "STATION_ADMIN")';
        $stmt = $db->query($sql);
        return $stmt->fetchAll();
    }

    /**
     * Register (upsert) a device token for a user.
     * If the token already exists for another user, reassign it to the new user
     * (a single physical device can only belong to one user at a time).
     */
    public static function register(int $userId, string $token, ?string $deviceName = null): int
    {
        $db = Database::getInstance()->getConnection();

        // Check if token already exists
        $stmt = $db->prepare('SELECT id, user_id FROM device_tokens WHERE token = ?');
        $stmt->execute([$token]);
        $existing = $stmt->fetch();

        if ($existing) {
            // Update existing token — reassign to current user, refresh device name, re-activate
            $stmt = $db->prepare(
                'UPDATE device_tokens SET user_id = ?, device_name = ?, is_active = 1 WHERE id = ?'
            );
            $stmt->execute([$userId, $deviceName, $existing['id']]);
            return (int) $existing['id'];
        }

        // Insert new token
        $stmt = $db->prepare(
            'INSERT INTO device_tokens (user_id, token, device_name, is_active)
             VALUES (?, ?, ?, 1)'
        );
        $stmt->execute([$userId, $token, $deviceName]);
        return (int) $db->lastInsertId();
    }

    /**
     * Deactivate a token (soft delete — keeps history, stops push delivery).
     */
    public static function deactivate(string $token): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE device_tokens SET is_active = 0 WHERE token = ?');
        return $stmt->execute([$token]);
    }

    /**
     * Deactivate all tokens for a user (e.g. on logout).
     */
    public static function deactivateByUserId(int $userId): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE device_tokens SET is_active = 0 WHERE user_id = ?');
        return $stmt->execute([$userId]);
    }

    /**
     * Hard-delete a token.
     */
    public static function delete(string $token): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM device_tokens WHERE token = ?');
        return $stmt->execute([$token]);
    }
}
