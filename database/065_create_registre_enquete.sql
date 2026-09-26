-- Registre d'enquête (Police Judiciaire) — investigation case-file register.
-- numero is auto-generated via plainte_sequence (ENQ key) but may be
-- overridden by the user; it must remain unique.
-- OPJ and Enquêteur are FK references to personnel (same pattern as plainte).

CREATE TABLE IF NOT EXISTS `registre_enquete` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `numero` VARCHAR(255) NOT NULL COMMENT 'N° d''enregistrement (auto-generated ENQ format, user-overridable)',
    `date_ouverture` DATE NOT NULL COMMENT 'Date d''ouverture du dossier d''enquête',
    `numero_dossier` VARCHAR(100) NULL COMMENT 'N° du dossier rattaché (plainte / TTR)',
    `nature_infraction` VARCHAR(255) NOT NULL COMMENT 'Nature de l''infraction',
    `date_lieu_faits` TEXT NULL COMMENT 'Date et lieu des faits',
    `plaignant` VARCHAR(255) NULL COMMENT 'Plaignant / partie civile',
    `mise_en_cause` VARCHAR(255) NULL COMMENT 'Personne(s) mise(s) en cause',
    `enqueteur_personnel_id` INT UNSIGNED NULL COMMENT 'FK to personnel — Enquêteur chargé de l''enquête',
    `opj_personnel_id` INT UNSIGNED NULL COMMENT 'FK to personnel — OPJ',
    `statut` ENUM('EN_COURS','SUSPENDUE','TRANSMISE','CLOTUREE') NOT NULL DEFAULT 'EN_COURS' COMMENT 'État de l''enquête',
    `observations` TEXT NULL COMMENT 'Observations',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the entry',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_registre_enquete_enqueteur` FOREIGN KEY (`enqueteur_personnel_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_registre_enquete_opj` FOREIGN KEY (`opj_personnel_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_registre_enquete_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    UNIQUE KEY `uq_registre_enquete_numero` (`numero`),
    INDEX `idx_registre_enquete_date` (`date_ouverture`),
    INDEX `idx_registre_enquete_statut` (`statut`),
    INDEX `idx_registre_enquete_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_registre_enquete` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `registre_enquete_id` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(100) NULL,
    `file_size` INT UNSIGNED NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_registre_enquete` FOREIGN KEY (`registre_enquete_id`) REFERENCES `registre_enquete`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_registre_enquete_id` (`registre_enquete_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
