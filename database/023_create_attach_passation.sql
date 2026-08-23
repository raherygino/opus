-- Attachments for "Passation" (Sédentaire > Poste).
-- Mirrors attach_declaration_perte / attach_correspondance: files on disk
-- live under uploads/passations and are removed by the controller on delete.

CREATE TABLE IF NOT EXISTS `attach_passation` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `passation_id` INT UNSIGNED NOT NULL COMMENT 'FK to passation',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_passation` FOREIGN KEY (`passation_id`) REFERENCES `passation`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_passation_id` (`passation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
