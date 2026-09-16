-- Perquisition (Police Judiciaire) — search warrant records.
-- numero is auto-generated via plainte_sequences (PEQ key) but may be
-- overridden by the user; it must remain unique.

CREATE TABLE IF NOT EXISTS `perquisition` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `numero` VARCHAR(255) NOT NULL COMMENT 'N° de la perquisition (auto-generated PEQ format, user-overridable)',
    `numero_ttr` VARCHAR(255) NULL COMMENT 'N° du TTR',
    `substitut` VARCHAR(255) NULL COMMENT 'Nom du Substitut',
    `affaire` VARCHAR(255) NOT NULL COMMENT 'Affaire concernée',
    `motif` TEXT NULL COMMENT 'Motif de la perquisition',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the perquisition',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_perquisition_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    UNIQUE KEY `uq_perquisition_numero` (`numero`),
    INDEX `idx_perquisition_affaire` (`affaire`),
    INDEX `idx_perquisition_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_perquisition` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `perquisition_id` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(100) NULL,
    `file_size` INT UNSIGNED NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_perquisition` FOREIGN KEY (`perquisition_id`) REFERENCES `perquisition`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_perquisition_id` (`perquisition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
