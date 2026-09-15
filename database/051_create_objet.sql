-- Objet Saisi / Objet Trouvé (Police Judiciaire).
-- Two related entities shown behind a two-tab OBJET interface.
-- Each has its own generic attachment table (PJ attachment pattern).

CREATE TABLE IF NOT EXISTS `objet_saisi` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `numero_dossier` VARCHAR(255) NULL COMMENT 'N° du dossier concerné',
    `motif` TEXT NOT NULL COMMENT 'Motif de la saisie',
    `type_objet` VARCHAR(100) NOT NULL COMMENT 'Type d''objet (liste prédéfinie)',
    `proprietaire` VARCHAR(255) NULL COMMENT 'Nom et prénom du propriétaire',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the entry',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_objet_saisi_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_objet_saisi_type` (`type_objet`),
    INDEX `idx_objet_saisi_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_objet_saisi` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `objet_saisi_id` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(100) NULL,
    `file_size` INT UNSIGNED NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_objet_saisi` FOREIGN KEY (`objet_saisi_id`) REFERENCES `objet_saisi`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_objet_saisi_id` (`objet_saisi_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `objet_trouve` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `affaire` VARCHAR(255) NOT NULL COMMENT 'Affaire concernée',
    `motif_decouverte` ENUM('REQUISITION','SUR_PERSONNE','PERQUISITION') NOT NULL COMMENT 'Motif de découverte',
    `restitution` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Objet restitué (0/1)',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the entry',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_objet_trouve_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_objet_trouve_motif` (`motif_decouverte`),
    INDEX `idx_objet_trouve_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_objet_trouve` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `objet_trouve_id` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(100) NULL,
    `file_size` INT UNSIGNED NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_objet_trouve` FOREIGN KEY (`objet_trouve_id`) REFERENCES `objet_trouve`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_objet_trouve_id` (`objet_trouve_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
