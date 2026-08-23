-- Attachments for "Déclaration de perte" (Sédentaire > Secrétariat).
-- Mirrors attach_correspondance: files on disk live under
-- uploads/declarations-perte and are removed by the controller on delete.

CREATE TABLE IF NOT EXISTS `attach_declaration_perte` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `declaration_id` INT UNSIGNED NOT NULL COMMENT 'FK to declaration_perte',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_declaration_perte` FOREIGN KEY (`declaration_id`) REFERENCES `declaration_perte`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_declaration_perte_id` (`declaration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
