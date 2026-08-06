-- Device tokens table for Firebase Cloud Messaging (FCM) push notifications
-- Stores FCM registration tokens per user/device so the API can send push
-- notifications when new notifications are created.

CREATE TABLE IF NOT EXISTS `device_tokens` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT UNSIGNED NOT NULL COMMENT 'Owner of the device token',
    `token` VARCHAR(255) NOT NULL COMMENT 'FCM registration token',
    `device_name` VARCHAR(255) NULL COMMENT 'Optional human-readable device name',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_device_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_device_tokens_user` (`user_id`),
    INDEX `idx_device_tokens_token` (`token`),
    UNIQUE KEY `uq_device_tokens_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
