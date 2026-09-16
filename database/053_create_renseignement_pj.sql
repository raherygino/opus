-- Renseignement PJ (Police Judiciaire) — renseignements judiciaires.
-- Free-text record of an infraction for the Police Judiciaire module.
-- Table is namespaced `renseignement_pj` to avoid colliding with the
-- Sédentaire/SG "renseignement" features.

CREATE TABLE IF NOT EXISTS `renseignement_pj` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `nature_infraction` VARCHAR(255) NOT NULL COMMENT 'Nature de l''infraction',
    `date_lieu_faits` TEXT NULL COMMENT 'Date et lieu des faits',
    `circonstances` TEXT NULL COMMENT 'Circonstances de l''affaire',
    `prejudices` TEXT NULL COMMENT 'Préjudices causés',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the renseignement',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_renseignement_pj_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_renseignement_pj_nature` (`nature_infraction`),
    INDEX `idx_renseignement_pj_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_renseignement_pj` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `renseignement_id` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(100) NULL,
    `file_size` INT UNSIGNED NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_renseignement_pj` FOREIGN KEY (`renseignement_id`) REFERENCES `renseignement_pj`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_renseignement_pj_id` (`renseignement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
