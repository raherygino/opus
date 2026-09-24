-- Attachments for "Activités" (Service Général) — patrouilles et interventions.
-- Mirrors attach_evenement_survenu: files on disk live under
-- uploads/activites and are removed by the controller on delete.

CREATE TABLE IF NOT EXISTS `attach_activite` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `activite_id` INT UNSIGNED NOT NULL COMMENT 'FK to activite',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_activite` FOREIGN KEY (`activite_id`) REFERENCES `activite`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_activite_id` (`activite_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
