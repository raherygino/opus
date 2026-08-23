<?php

namespace App\Models;

use App\Database;
use PDO;

class Notification
{
    public static function getAll(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT n.*, p.im AS personnel_im, p.lastname AS personnel_nom,
                       p.firstname AS personnel_prenoms, p.grade AS personnel_grade,
                       p.photo AS personnel_photo,
                       u.username AS created_by_username,
                       cp.firstname AS created_by_firstname,
                       cp.photo AS created_by_photo,
                       cu.personnel_id AS created_by_personnel_id
                FROM notifications n
                LEFT JOIN personnel p ON n.personnel_id = p.id
                LEFT JOIN users u ON n.created_by = u.id
                LEFT JOIN users cu ON n.created_by = cu.id
                LEFT JOIN personnel cp ON cu.personnel_id = cp.id
                WHERE 1=1';
        $params = [];

        if (!empty($filters['service'])) {
            $sql .= ' AND n.service = ?';
            $params[] = $filters['service'];
        }
        if (!empty($filters['user_id'])) {
            $sql .= ' AND (n.user_id = ? OR n.user_id IS NULL)';
            $params[] = $filters['user_id'];
        }
        if (isset($filters['is_read']) && $filters['is_read'] !== '') {
            $sql .= ' AND n.is_read = ?';
            $params[] = (int) $filters['is_read'];
        }

        $sql .= ' ORDER BY n.created_at DESC';
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    public static function getById(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT n.*, p.im AS personnel_im, p.lastname AS personnel_nom,
                    p.firstname AS personnel_prenoms, p.grade AS personnel_grade,
                    p.photo AS personnel_photo,
                    u.username AS created_by_username,
                    cp.firstname AS created_by_firstname,
                    cp.photo AS created_by_photo,
                    cu.personnel_id AS created_by_personnel_id
             FROM notifications n
             LEFT JOIN personnel p ON n.personnel_id = p.id
             LEFT JOIN users u ON n.created_by = u.id
             LEFT JOIN users cu ON n.created_by = cu.id
             LEFT JOIN personnel cp ON cu.personnel_id = cp.id
             WHERE n.id = ?'
        );
        $stmt->execute([$id]);
        $notif = $stmt->fetch();
        return $notif ?: null;
    }

    public static function getForUser(int $userId, ?string $roleCode = null, ?string $service = null): array
    {
        $db = Database::getInstance()->getConnection();

        if ($roleCode === 'SUPER_ADMIN' || $roleCode === 'STATION_ADMIN') {
            $sql = 'SELECT n.*, p.im AS personnel_im, p.lastname AS personnel_nom,
                           p.firstname AS personnel_prenoms, p.grade AS personnel_grade,
                           p.photo AS personnel_photo,
                           u.username AS created_by_username,
                           cp.firstname AS created_by_firstname,
                           cp.photo AS created_by_photo,
                           cu.personnel_id AS created_by_personnel_id
                    FROM notifications n
                    LEFT JOIN personnel p ON n.personnel_id = p.id
                    LEFT JOIN users u ON n.created_by = u.id
                    LEFT JOIN users cu ON n.created_by = cu.id
                    LEFT JOIN personnel cp ON cu.personnel_id = cp.id
                    ORDER BY n.created_at DESC';
            $stmt = $db->query($sql);
        } else {
            $sql = 'SELECT n.*, p.im AS personnel_im, p.lastname AS personnel_nom,
                           p.firstname AS personnel_prenoms, p.grade AS personnel_grade,
                           p.photo AS personnel_photo,
                           u.username AS created_by_username,
                           cp.firstname AS created_by_firstname,
                           cp.photo AS created_by_photo,
                           cu.personnel_id AS created_by_personnel_id
                    FROM notifications n
                    LEFT JOIN personnel p ON n.personnel_id = p.id
                    LEFT JOIN users u ON n.created_by = u.id
                    LEFT JOIN users cu ON n.created_by = cu.id
                    LEFT JOIN personnel cp ON cu.personnel_id = cp.id
                    WHERE n.user_id = ? OR (n.user_id IS NULL AND (? IS NULL OR n.service = ?))
                    ORDER BY n.created_at DESC';
            $stmt = $db->prepare($sql);
            $stmt->execute([$userId, $service, $service]);
        }
        return $stmt->fetchAll();
    }

    public static function getUnreadCount(int $userId, ?string $roleCode = null, ?string $service = null): int
    {
        $db = Database::getInstance()->getConnection();

        if ($roleCode === 'SUPER_ADMIN' || $roleCode === 'STATION_ADMIN') {
            $sql = 'SELECT COUNT(*) FROM notifications WHERE is_read = 0';
            $stmt = $db->query($sql);
        } else {
            $sql = 'SELECT COUNT(*) FROM notifications WHERE is_read = 0 AND (user_id = ? OR (user_id IS NULL AND (? IS NULL OR service = ?)))';
            $stmt = $db->prepare($sql);
            $stmt->execute([$userId, $service, $service]);
        }
        return (int) $stmt->fetchColumn();
    }

    public static function create(array $data): int
    {
        // ── Source-level self-notification guard ──────────────────────────
        // A user must NEVER receive a notification for an action they
        // performed themselves. This is enforced here — at the single entry
        // point for all notification creation — so that even if a controller
        // accidentally includes the actor in the recipient list, the
        // notification is silently dropped instead of delivered.
        $userId   = isset($data['user_id']) ? (int) $data['user_id'] : null;
        $creator  = isset($data['created_by']) ? (int) $data['created_by'] : null;
        if ($userId !== null && $creator !== null && $userId === $creator) {
            return 0;
        }

        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO notifications (title, message, link, type, service, user_id, personnel_id, created_by, is_read)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)'
        );
        $stmt->execute([
            $data['title'],
            $data['message'] ?? null,
            $data['link'] ?? null,
            $data['type'] ?? 'info',
            $data['service'] ?? 'System',
            $data['user_id'] ?? null,
            $data['personnel_id'] ?? null,
            $data['created_by'] ?? null,
        ]);
        $id = (int) $db->lastInsertId();

        // --- Send FCM push notification ---
        // Centralized here so that EVERY notification creation — regardless of
        // which controller triggered it — automatically delivers a push to the
        // relevant Android device(s).
        self::sendPush($id, $data);

        return $id;
    }

    /**
     * Send an FCM push notification for a newly created notification record.
     *
     * Delivery logic:
     *  - If user_id is set → push to that specific user.
     *  - If user_id is NULL (broadcast) → push to all admin users.
     *
     * This is wrapped in try/catch so that FCM failures (network errors, missing
     * service account config, etc.) never prevent the notification record from
     * being created or the API response from being returned.
     */
    private static function sendPush(int $id, array $data): void
    {
        try {
            // Enrich the push payload with the creator's personnel info so the
            // Android app can display the sender's first name and profile photo
            // in both the in-app notification list and the status-bar notification.
            $creatorFirstname = '';
            $creatorPersonnelId = '';
            $creatorHasPhoto = '0';

            if (!empty($data['created_by'])) {
                $creatorUser = \App\Models\User::getById((int) $data['created_by']);
                if ($creatorUser && !empty($creatorUser['personnel_id'])) {
                    $creatorPersonnel = \App\Models\Personnel::getById((int) $creatorUser['personnel_id']);
                    if ($creatorPersonnel) {
                        $creatorFirstname = (string) ($creatorPersonnel['firstname'] ?? '');
                        $creatorPersonnelId = (string) $creatorPersonnel['id'];
                        $creatorHasPhoto = !empty($creatorPersonnel['photo']) ? '1' : '0';
                    }
                }
            }

            $payload = [
                'title' => $data['title'] ?? 'OPUS',
                'body'  => $data['message'] ?? '',
                'data'  => [
                    'notification_id'         => (string) $id,
                    'type'                    => (string) ($data['type'] ?? 'info'),
                    'service'                 => (string) ($data['service'] ?? ''),
                    'click_action'            => 'OPEN_NOTIFICATIONS',
                    'link'                    => (string) ($data['link'] ?? ''),
                    'creator_firstname'       => $creatorFirstname,
                    'creator_personnel_id'    => $creatorPersonnelId,
                    'creator_has_photo'       => $creatorHasPhoto,
                ],
            ];

            if (!empty($data['user_id'])) {
                \App\Helpers\FcmSender::sendToUser((int) $data['user_id'], $payload);
            } else {
                \App\Helpers\FcmSender::sendToAdmins($payload);
            }
        } catch (\Throwable $e) {
            // Log but never throw — push delivery is best-effort.
            error_log('[Notification::sendPush] Failed: ' . $e->getMessage());
        }
    }

    public static function markAsRead(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE id = ?');
        return $stmt->execute([$id]);
    }

    /**
     * Mark every notification pointing to $link as read for the given user.
     * Used to auto-dismiss a notification once the target entity has been
     * viewed (e.g. a correspondance detail screen opened by an admin).
     */
    public static function markAsReadByLink(string $link, int $userId): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE notifications SET is_read = 1 WHERE link = ? AND user_id = ? AND is_read = 0'
        );
        $stmt->execute([$link, $userId]);
        return $stmt->rowCount();
    }

    public static function markAllAsRead(int $userId, ?string $roleCode = null, ?string $service = null): bool
    {
        $db = Database::getInstance()->getConnection();

        if ($roleCode === 'SUPER_ADMIN' || $roleCode === 'STATION_ADMIN') {
            $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE is_read = 0');
            return $stmt->execute();
        } else {
            $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE is_read = 0 AND (user_id = ? OR (user_id IS NULL AND (? IS NULL OR service = ?)))');
            return $stmt->execute([$userId, $service, $service]);
        }
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM notifications WHERE id = ?');
        return $stmt->execute([$id]);
    }

    public static function getAdminUsers(): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT u.id, u.username, u.personnel_id, r.code AS role_code
                FROM users u
                JOIN roles r ON u.role_id = r.id
                WHERE r.code IN ("SUPER_ADMIN", "STATION_ADMIN") AND u.is_active = 1';
        $stmt = $db->query($sql);
        return $stmt->fetchAll();
    }

    /**
     * Get every active, NON-admin user whose role grants at least can_view
     * on the given module (e.g. "personnel", "sedentaire_secretariat_correspondance").
     *
     * These are the "affected regular users" for a feature: people who can see
     * the feature and therefore should be informed when an administrator
     * changes something inside it. Administrators are intentionally excluded
     * because they are notified separately via getAdminUsers() / notifyAdmins().
     */
    public static function getUsersWithModulePermission(string $module): array
    {
        return self::getUsersWithAnyModulePermission([$module]);
    }

    /**
     * Same as getUsersWithModulePermission but accepts multiple modules at
     * once. A user whose role grants can_view on ANY of the listed modules
     * is returned exactly once (deduplicated). Administrators are excluded.
     */
    public static function getUsersWithAnyModulePermission(array $modules): array
    {
        if (empty($modules)) {
            return [];
        }
        $db = Database::getInstance()->getConnection();
        $modules = array_values(array_unique($modules));
        $placeholders = implode(',', array_fill(0, count($modules), '?'));
        $sql = "SELECT DISTINCT u.id, u.username, u.personnel_id, r.code AS role_code
                FROM users u
                JOIN roles r ON u.role_id = r.id
                JOIN role_permissions rp ON rp.role_id = r.id
                WHERE u.is_active = 1
                  AND rp.can_view = 1
                  AND rp.module IN ($placeholders)
                  AND r.code NOT IN ('SUPER_ADMIN', 'STATION_ADMIN')";
        $stmt = $db->prepare($sql);
        $stmt->execute($modules);
        return $stmt->fetchAll();
    }

    /**
     * Get every active user whose role matches the given role ID.
     * Used for role/permission change notifications: when an admin modifies
     * a role or its permissions, every user who currently HAS that role is
     * notified (except the admin who made the change).
     */
    public static function getUsersByRoleId(int $roleId): array
    {
        $db = Database::getInstance()->getConnection();
        $sql = 'SELECT u.id, u.username, u.personnel_id, r.code AS role_code
                FROM users u
                JOIN roles r ON u.role_id = r.id
                WHERE u.is_active = 1 AND u.role_id = ?';
        $stmt = $db->prepare($sql);
        $stmt->execute([$roleId]);
        return $stmt->fetchAll();
    }

    /**
     * Core deduplicated recipient sender.
     *
     * Sends one notification to each unique user ID in $userIds, excluding:
     *  - the actor (the user who triggered the change)
     *  - any user ID in $excludeIds (e.g. a creator already notified via
     *    a targeted message, to avoid duplicates)
     *
     * Deduplication is handled here so that even if a user qualifies through
     * multiple roles or relationships, they receive exactly ONE notification
     * per change. The self-notification guard in create() is an additional
     * safety net.
     */
    public static function notifyRecipients(array $userIds, array $data, ?int $actorId, array $excludeIds = []): void
    {
        $exclude = array_map('intval', $excludeIds);
        if ($actorId !== null) {
            $exclude[] = (int) $actorId;
        }
        $exclude = array_unique($exclude);

        $uniqueIds = array_unique(array_map('intval', $userIds));
        foreach ($uniqueIds as $uid) {
            if (in_array($uid, $exclude, true)) {
                continue;
            }
            self::create([
                'title'        => $data['title'],
                'message'      => $data['message'] ?? null,
                'link'         => $data['link'] ?? null,
                'type'         => $data['type'] ?? 'info',
                'service'      => $data['service'] ?? 'System',
                'user_id'      => $uid,
                'personnel_id' => $data['personnel_id'] ?? null,
                'created_by'   => $actorId,
            ]);
        }
    }

    /**
     * Broadcast a notification to every active administrator except the actor.
     *
     * Shared helper so the admin → admin direction stays consistent across all
     * controllers. $excludeIds can be used to prevent duplicates when a
     * targeted notification (e.g. comportement notifyCreator) already informed
     * a specific user who also happens to be an admin.
     */
    public static function notifyAdmins(array $data, ?int $actorId, array $excludeIds = []): void
    {
        $admins = self::getAdminUsers();
        $userIds = array_map(fn($a) => (int) $a['id'], $admins);
        self::notifyRecipients($userIds, $data, $actorId, $excludeIds);
    }

    /**
     * Broadcast a notification to every active, non-admin user whose role
     * grants can_view on any of the given modules, except the actor and any
     * user in $excludeIds.
     *
     * $modules accepts a single string or an array of module codes. When
     * multiple modules are provided, a user with permission on ANY of them
     * receives exactly ONE notification (deduplicated via notifyRecipients).
     *
     * This covers BOTH the admin → user and the peer → peer direction: any
     * user with view permission on the feature is notified when ANY other
     * user (admin or peer) changes that feature. The actor is always excluded.
     */
    public static function notifyModuleUsers($modules, array $data, ?int $actorId, array $excludeIds = []): void
    {
        $modules = is_array($modules) ? $modules : [$modules];
        $users = self::getUsersWithAnyModulePermission($modules);
        $userIds = array_map(fn($u) => (int) $u['id'], $users);
        self::notifyRecipients($userIds, $data, $actorId, $excludeIds);
    }

    /**
     * Unified, reusable feature-change notification.
     *
     * This is the single entry point every controller should use when a
     * feature's data is created, edited, or otherwise changed. It handles
     * ALL recipient rules in one call:
     *
     *   1. Admins       — every active admin is notified (admin ↔ admin flow).
     *   2. Feature users — every active, non-admin user whose role grants
     *      can_view on ANY of $modules is notified (peer-to-peer flow).
     *   3. Actor exclusion — the user who made the change never receives
     *      their own notification (enforced in notifyRecipients + create()).
     *   4. Deduplication — each recipient gets exactly ONE notification,
     *      even if they qualify through multiple modules or relationships.
     *
     * $adminData and $userData allow different wording for the two groups
     * (admins get a concise summary; feature users get a "Veuillez en prendre
     * connaissance" call-to-action). Pass the same array for both if the
     * wording should be identical.
     *
     * @param string|array $modules    The feature module code(s) that
     *                                 determine eligibility for feature users.
     * @param array        $adminData  Notification payload for admins.
     * @param array        $userData   Notification payload for feature users.
     * @param int|null     $actorId    The user who made the change (excluded).
     * @param int[]        $excludeIds Additional user IDs to exclude (e.g. a
     *                                 creator already notified via a targeted
     *                                 message, to avoid duplicates).
     */
    public static function notifyFeatureChange(
        $modules,
        array $adminData,
        array $userData,
        ?int $actorId,
        array $excludeIds = []
    ): void {
        // Admins and feature users are mutually exclusive groups (the module-
        // user query excludes admin role codes), so there is no cross-group
        // duplication. Each group is deduplicated within itself by
        // notifyRecipients.
        self::notifyAdmins($adminData, $actorId, $excludeIds);
        self::notifyModuleUsers($modules, $userData, $actorId, $excludeIds);
    }
}
