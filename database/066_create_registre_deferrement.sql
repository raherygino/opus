-- Registre de déferrement (Police Judiciaire) — register of persons brought
-- before the judicial authority (défèrement / "registre spécial").
-- numero is auto-generated via plainte_sequence (DEF key) but may be
-- overridden by the user; it must remain unique.

CREATE TABLE IF NOT EXISTS `registre_deferrement` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `numero` VARCHAR(255) NOT NULL COMMENT 'N° d''enregistrement (auto-generated DEF format, user-overridable)',
    `date_heure_deferrement` DATETIME NOT NULL COMMENT 'Date et heure du déferrement (conduite devant le magistrat)',
    `personne_nom` VARCHAR(255) NOT NULL COMMENT 'Nom et prénom de la personne déférée',
    `date_lieu_naissance` VARCHAR(255) NULL COMMENT 'Date et lieu de naissance',
    `infraction` VARCHAR(255) NULL COMMENT 'Infraction / motif du déferrement',
    `numero_dossier` VARCHAR(100) NULL COMMENT 'N° du dossier / TTR rattaché',
    `autorite` VARCHAR(255) NULL COMMENT 'Autorité judiciaire saisie (magistrat / parquet)',
    `destination` VARCHAR(255) NULL COMMENT 'Juridiction ou lieu de destination',
    `escorte` TEXT NULL COMMENT 'Noms et grades des éléments d''escorte (un par ligne)',
    `suite_donnee` TEXT NULL COMMENT 'Suite donnée par le magistrat (décision)',
    `observations` TEXT NULL COMMENT 'Observations',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the entry',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_registre_deferrement_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    UNIQUE KEY `uq_registre_deferrement_numero` (`numero`),
    INDEX `idx_registre_deferrement_date` (`date_heure_deferrement`),
    INDEX `idx_registre_deferrement_personne` (`personne_nom`),
    INDEX `idx_registre_deferrement_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_registre_deferrement` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `registre_deferrement_id` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(100) NULL,
    `file_size` INT UNSIGNED NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_registre_deferrement` FOREIGN KEY (`registre_deferrement_id`) REFERENCES `registre_deferrement`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_registre_deferrement_id` (`registre_deferrement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
