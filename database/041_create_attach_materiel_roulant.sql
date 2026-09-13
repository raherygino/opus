-- Attachments for "Matériel roulant" (Sédentaire > Poste).
-- Mirrors attach_armement: files on disk live under uploads/materiels-roulants
-- and are removed by the controller on delete.

CREATE TABLE IF NOT EXISTS `attach_materiel_roulant` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `materiel_roulant_id` INT UNSIGNED NOT NULL COMMENT 'FK to materiel_roulant',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_materiel_roulant` FOREIGN KEY (`materiel_roulant_id`) REFERENCES `materiel_roulant`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_materiel_roulant_id` (`materiel_roulant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
