-- Plainte SORTIE (Police Judiciaire — outgoing processing of an ENTRÉE complaint).
-- A SORTIE is always linked to exactly one ENTRÉE via plainte_entree_id (FK).
-- Two natures share this table, distinguished by the `nature` column:
--   DAT         — DAT (date_deferrement optional)
--   DEFERREMENT — Déferrement (date_deferrement required)
-- The sortie number (numero) is generated server-side by PlainteSequence
-- using the SORTIE prefix and the format:
--   N°.../MSP/DGPN/DGA/DRSP-1/CSP/A-TRIMO/{YY}

CREATE TABLE IF NOT EXISTS `plainte_sortie` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `plainte_entree_id` INT UNSIGNED NOT NULL COMMENT 'FK to the linked ENTRÉE complaint',
    `nature` ENUM('DAT','DEFERREMENT') NOT NULL COMMENT 'Sortie nature',
    `date_sortie` DATE NOT NULL COMMENT 'Date of the sortie',
    `numero` VARCHAR(100) NOT NULL COMMENT 'Generated sortie number N°.../MSP/DGPN/DGA/DRSP-1/CSP/A-TRIMO/{YY}',
    `numero_ttr` VARCHAR(100) NULL COMMENT 'N° TTR',
    `nom_substitut` VARCHAR(255) NULL COMMENT 'Nom du Substitut',
    `date_deferrement` DATE NULL COMMENT 'Date du déferrement (DEFERREMENT only)',
    `observation` TEXT NULL COMMENT 'Observation',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the sortie',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_plainte_sortie_numero` (`numero`),
    CONSTRAINT `fk_plainte_sortie_entree` FOREIGN KEY (`plainte_entree_id`) REFERENCES `plainte_entree`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_plainte_sortie_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_plainte_sortie_entree_id` (`plainte_entree_id`),
    INDEX `idx_plainte_sortie_nature` (`nature`),
    INDEX `idx_plainte_sortie_date` (`date_sortie`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_plainte_sortie` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `plainte_sortie_id` INT UNSIGNED NOT NULL COMMENT 'FK to plainte_sortie',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_plainte_sortie` FOREIGN KEY (`plainte_sortie_id`) REFERENCES `plainte_sortie`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_plainte_sortie_id` (`plainte_sortie_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
