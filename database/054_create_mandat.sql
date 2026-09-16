-- Mandat (Police Judiciaire) — judicial warrants.
-- numero is auto-generated via plainte_sequence (MAN key) but may be
-- overridden by the user; it must remain unique.

CREATE TABLE IF NOT EXISTS `mandat` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `numero` VARCHAR(255) NOT NULL COMMENT 'N° du mandat (auto-generated MAN format, user-overridable)',
    `type` VARCHAR(50) NOT NULL COMMENT 'Objet du mandat (AMENER, COMPARUTION, ARRET, DEPOT)',
    `autorite` VARCHAR(255) NULL COMMENT 'Autorité ayant délivré le mandat',
    `personne_nom` VARCHAR(255) NOT NULL COMMENT 'Nom et prénom de la personne concernée',
    `date_lieu_naissance` VARCHAR(255) NULL COMMENT 'Date et lieu de naissance',
    `motif` TEXT NULL COMMENT 'Motif du mandat',
    `qualification_infraction` VARCHAR(255) NULL COMMENT 'Qualification de l''infraction',
    `opj_execution` VARCHAR(255) NULL COMMENT 'Nom de l''OPJ chargé de l''exécution',
    `date_heure_execution` DATETIME NULL COMMENT 'Date et heure d''exécution',
    `lieu_execution` VARCHAR(255) NULL COMMENT 'Lieu d''exécution',
    `observations` TEXT NULL COMMENT 'Observations',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the mandat',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_mandat_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    UNIQUE KEY `uq_mandat_numero` (`numero`),
    INDEX `idx_mandat_type` (`type`),
    INDEX `idx_mandat_personne` (`personne_nom`),
    INDEX `idx_mandat_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_mandat` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `mandat_id` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(100) NULL,
    `file_size` INT UNSIGNED NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_mandat` FOREIGN KEY (`mandat_id`) REFERENCES `mandat`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_mandat_id` (`mandat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
