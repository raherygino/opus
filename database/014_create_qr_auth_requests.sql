-- QR-code-based authentication/pairing requests
-- Stores short-lived, one-time-use requests that let a phone scan a QR code
-- displayed by a desktop (or vice-versa) to approve a login. The QR code only
-- ever contains the `request_code` — never credentials or tokens.

CREATE TABLE IF NOT EXISTS `qr_auth_requests` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `request_code` CHAR(64) NOT NULL UNIQUE COMMENT 'Cryptographically random hex identifier embedded in the QR code',
    `device_type` ENUM('desktop','android') NOT NULL COMMENT 'Which device is requesting authentication',
    `device_name` VARCHAR(255) NOT NULL COMMENT 'Human-readable name shown to the approver',
    `status` ENUM('pending','scanned','approved','rejected','expired','cancelled','consumed') NOT NULL DEFAULT 'pending',
    `requester_user_id` INT UNSIGNED NULL COMMENT 'Authenticated user on the requesting device (set when desktop is already logged in, e.g. reverse flow)',
    `approver_user_id` INT UNSIGNED NULL COMMENT 'User who approved the request (the phone user)',
    `issued_access_token` TEXT NULL COMMENT 'Access token generated for the requesting device on approval',
    `issued_refresh_token` TEXT NULL COMMENT 'Refresh token generated for the requesting device on approval',
    `client_ip` VARCHAR(45) NULL COMMENT 'IP address of the requesting device',
    `user_agent` VARCHAR(255) NULL COMMENT 'User-Agent of the requesting device',
    `expires_at` TIMESTAMP NOT NULL COMMENT 'When the request becomes invalid',
    `scanned_at` TIMESTAMP NULL,
    `resolved_at` TIMESTAMP NULL COMMENT 'When the request was approved/rejected/cancelled',
    `consumed_at` TIMESTAMP NULL COMMENT 'When the requesting device retrieved its tokens (one-time)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_qr_auth_requester` FOREIGN KEY (`requester_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_qr_auth_approver` FOREIGN KEY (`approver_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_qr_auth_code` (`request_code`),
    INDEX `idx_qr_auth_status` (`status`),
    INDEX `idx_qr_auth_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
