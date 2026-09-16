-- Convocation (Police Judiciaire — summonses).
-- Two convocation types share this table, distinguished by the `type` column:
--   ST_PARQUET      — ST Parquet (numero format: N°.../MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/ST/MC/COV/{YY})
--   PLAINTE_DIRECTE — Plainte directe (numero format: N°.../MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/MC/COV/{YY})
-- Both types share the exact same fields; only the numero format differs.
-- The numero is generated server-side by PlainteSequence using the per-type
-- prefix (COV_ST / COV_PD) and stored here.

CREATE TABLE IF NOT EXISTS `convocation` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `type` ENUM('ST_PARQUET','PLAINTE_DIRECTE') NOT NULL COMMENT 'Convocation type',
    `date_convocation` DATE NOT NULL COMMENT 'Date of the convocation',
    `numero` VARCHAR(100) NOT NULL COMMENT 'Generated numero N°.../MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/{ST|PD}/MC/COV/{YY}',
    `nom` VARCHAR(255) NOT NULL COMMENT 'Nom de la personne convoquée',
    `adresse` TEXT NULL COMMENT 'Adresse',
    `infraction` VARCHAR(255) NULL COMMENT 'Infraction',
    `personne_accuse_recu` VARCHAR(255) NULL COMMENT 'Nom de la personne ayant accusée réception de la convocation',
    `numero_dossier` VARCHAR(100) NULL COMMENT 'Numéro du dossier rattaché',
    `observation` TEXT NULL COMMENT 'Observation',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the convocation',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_convocation_numero` (`numero`),
    CONSTRAINT `fk_convocation_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_convocation_type` (`type`),
    INDEX `idx_convocation_date` (`date_convocation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_convocation` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `convocation_id` INT UNSIGNED NOT NULL COMMENT 'FK to convocation',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_convocation` FOREIGN KEY (`convocation_id`) REFERENCES `convocation`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_convocation_id` (`convocation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
