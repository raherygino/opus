<?php

namespace App\Helpers;

use App\Database;
use App\Models\User;

/**
 * EditWindow — 24-hour edit lock for business records.
 *
 * A record may only be modified (update, workflow action, attachment
 * add/update/delete) within 24 hours of its creation (created_at).
 * Once the window has elapsed the record becomes read-only.
 *
 * Administrator accounts (SUPER_ADMIN / STATION_ADMIN) bypass the lock.
 *
 * The age check is computed by MySQL (TIMESTAMPDIFF against NOW()) so it is
 * independent of the PHP/MySQL timezone configuration.
 */
class EditWindow
{
    public const WINDOW_SECONDS = 24 * 3600;

    /**
     * Terminate the request with 403 when the record is older than 24h and
     * the caller is not an administrator.
     *
     * @param string $table   Business table holding the record (hardcoded
     *                        callers only — validated against [a-z_0-9]).
     * @param int    $id      Record id.
     * @param array  $authUser Authenticated user payload (from
     *                        AuthController::getAuthenticatedUser()).
     */
    public static function assertEditable(string $table, int $id, array $authUser): void
    {
        if (!preg_match('/^[a-z0-9_]+$/', $table)) {
            Response::error('Invalid table', 500);
        }

        $uid = (int) ($authUser['sub'] ?? 0);
        if ($uid > 0 && User::isAdminUser($uid)) {
            return;
        }

        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare("SELECT TIMESTAMPDIFF(SECOND, created_at, NOW()) FROM `$table` WHERE id = ?");
        $stmt->execute([$id]);
        $age = $stmt->fetchColumn();

        // Missing row: let the caller's own 404 handling report it.
        if ($age === false || $age === null) {
            return;
        }

        if ((int) $age > self::WINDOW_SECONDS) {
            Response::forbidden(
                "Le délai de modification de 24 heures est écoulé. "
                . "Cet enregistrement est désormais en lecture seule."
            );
        }
    }

    /**
     * Non-terminating variant — returns true when the given created_at
     * timestamp ('Y-m-d H:i:s', DB time) is still inside the edit window.
     */
    public static function isWithinWindow(string $table, int $id): bool
    {
        if (!preg_match('/^[a-z0-9_]+$/', $table)) {
            return true;
        }

        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare("SELECT TIMESTAMPDIFF(SECOND, created_at, NOW()) FROM `$table` WHERE id = ?");
        $stmt->execute([$id]);
        $age = $stmt->fetchColumn();

        return $age === false || (int) $age <= self::WINDOW_SECONDS;
    }
}
