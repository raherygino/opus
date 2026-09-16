-- Arrestation (Police Judiciaire) — arrest records.
-- numero is auto-generated via plainte_sequence (ARR key) but may be
-- overridden by the user; it must remain unique.

CREATE TABLE IF NOT EXISTS `arrestation` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `numero` VARCHAR(255) NOT NULL COMMENT 'N° de l''arrestation (auto-generated ARR format, user-overridable)',
    `date_heure_arrestation` DATETIME NOT NULL COMMENT 'Date et heure de l''arrestation',
    `personne_nom` VARCHAR(255) NOT NULL COMMENT 'Nom et prénom de la personne arrêtée',
    `lieu_arrestation` VARCHAR(255) NULL COMMENT 'Adresse ou lieu d''arrestation',
    `motif` TEXT NULL COMMENT 'Motif de l''arrestation',
    `policiers` TEXT NULL COMMENT 'Noms et grades des policiers ayant procédé à l''arrestation (un par ligne)',
    `numero_dossier` VARCHAR(100) NULL COMMENT 'N° du dossier concerné (référence au dossier rattaché)',
    `observations` TEXT NULL COMMENT 'Observations',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the arrestation',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_arrestation_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    UNIQUE KEY `uq_arrestation_numero` (`numero`),
    INDEX `idx_arrestation_date_heure` (`date_heure_arrestation`),
    INDEX `idx_arrestation_personne` (`personne_nom`),
    INDEX `idx_arrestation_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_arrestation` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `arrestation_id` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(100) NULL,
    `file_size` INT UNSIGNED NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_arrestation` FOREIGN KEY (`arrestation_id`) REFERENCES `arrestation`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_arrestation_id` (`arrestation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
