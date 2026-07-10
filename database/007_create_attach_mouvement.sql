CREATE TABLE IF NOT EXISTS `attach_mouvement` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `mouvement_id` INT UNSIGNED NOT NULL COMMENT 'FK to mouvement_personnel',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_mouvement` FOREIGN KEY (`mouvement_id`) REFERENCES `mouvement_personnel`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_mouvement_id` (`mouvement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
