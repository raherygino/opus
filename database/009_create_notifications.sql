CREATE TABLE IF NOT EXISTS `notifications` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `message` TEXT NULL,
    `type` ENUM('info', 'success', 'warning', 'error') NOT NULL DEFAULT 'info',
    `service` VARCHAR(50) NOT NULL COMMENT 'Originating service: PJ, SG, Sedentaire, System',
    `user_id` INT UNSIGNED NULL COMMENT 'Target user (NULL = broadcast to all admins)',
    `personnel_id` INT UNSIGNED NULL COMMENT 'Related personnel record',
    `created_by` INT UNSIGNED NULL COMMENT 'User who triggered the notification',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notifications_personnel` FOREIGN KEY (`personnel_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    INDEX `idx_notifications_user` (`user_id`),
    INDEX `idx_notifications_service` (`service`),
    INDEX `idx_notifications_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
