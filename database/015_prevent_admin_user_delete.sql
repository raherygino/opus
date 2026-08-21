-- ============================================
-- Migration 015: Prevent deletion of admin users
--
-- Enforces at the DATABASE level that no user account whose role is an
-- administrator role (SUPER_ADMIN or STATION_ADMIN) can ever be deleted,
-- regardless of which client or process issues the DELETE.
--
-- This is a defence-in-depth measure: the PHP UserController already blocks
-- admin deletion, but this trigger guarantees the rule can never be bypassed
-- by a direct SQL statement, a future endpoint, or a bug in the API.
-- ============================================

DELIMITER $$

CREATE TRIGGER trg_before_user_delete
BEFORE DELETE ON `users`
FOR EACH ROW
BEGIN
    DECLARE is_admin INT DEFAULT 0;

    SELECT COUNT(*) INTO is_admin
    FROM `roles`
    WHERE `id` = OLD.role_id
      AND `code` IN ('SUPER_ADMIN', 'STATION_ADMIN');

    IF is_admin > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un compte administrateur ne peut pas être supprimé';
    END IF;
END$$

DELIMITER ;
