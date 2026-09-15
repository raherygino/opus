-- Garde à Vue (Police Judiciaire — custody records).
-- Tracks the full GAV lifecycle for a person: identity, investigation,
-- health/rights, and the start/end/prolongation datetimes.

CREATE TABLE IF NOT EXISTS `garde_a_vue` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `nom` VARCHAR(255) NOT NULL COMMENT 'Nom de la personne en GAV',
    `prenoms` VARCHAR(255) NULL COMMENT 'Prénom(s)',
    `date_naissance` DATE NULL COMMENT 'Date de naissance',
    `adresse` TEXT NULL COMMENT 'Adresse',
    `enqueteur_permance` VARCHAR(255) NULL COMMENT 'Enquêteur de permanence',
    `opj_gav` VARCHAR(255) NULL COMMENT 'OPJ ayant décidé la garde à vue',
    `motif` TEXT NULL COMMENT 'Motif de la garde à vue',
    `etat_sante` TEXT NULL COMMENT 'État de santé',
    `droits_notifies` TEXT NULL COMMENT 'Droits notifiés',
    `personne_contacter` VARCHAR(255) NULL COMMENT 'Personne à contacter',
    `debut_gav` DATETIME NULL COMMENT 'Début de la garde à vue',
    `fin_gav` DATETIME NULL COMMENT 'Fin de la garde à vue',
    `prolongation_gav` DATETIME NULL COMMENT 'Prolongation de la garde à vue',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the GAV',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_gav_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_gav_nom` (`nom`),
    INDEX `idx_gav_debut_gav` (`debut_gav`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_garde_a_vue` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `garde_a_vue_id` INT UNSIGNED NOT NULL COMMENT 'FK to garde_a_vue',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_garde_a_vue` FOREIGN KEY (`garde_a_vue_id`) REFERENCES `garde_a_vue`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_garde_a_vue_id` (`garde_a_vue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
