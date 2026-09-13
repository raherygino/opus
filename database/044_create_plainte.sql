-- Plainte ENTRÉE (Police Judiciaire — incoming complaints).
-- Three complaint types share this table, distinguished by the `type` column:
--   ST_PARQUET      — ST Parquet (has numero_st + partie_civile + adresse_pc)
--   PLAINTE_DIRECTE — Plainte directe (has partie_civile + adresse_pc, no numero_st)
--   RAPPORT_POLICE  — Rapport de police (no partie_civile, no adresse_pc, no numero_st)
-- The dossier number (numero_dossier) is generated server-side by
-- PlainteSequence using the per-type prefix (ST/PD/RP) and stored here.
-- OPJ and Enquêteur are FK references to personnel (with grade shown in UI).

CREATE TABLE IF NOT EXISTS `plainte_entree` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `type` ENUM('ST_PARQUET','PLAINTE_DIRECTE','RAPPORT_POLICE') NOT NULL COMMENT 'Complaint type',
    `date_plainte` DATE NOT NULL COMMENT 'Date of the complaint',
    `numero_dossier` VARCHAR(50) NOT NULL COMMENT 'Generated dossier number N°.../TRIMO/{ST|PD|RP}/{YY}',
    `numero_st` VARCHAR(100) NULL COMMENT 'Numéro du ST (ST_PARQUET only)',
    `opj_personnel_id` INT UNSIGNED NULL COMMENT 'FK to personnel — OPJ',
    `enqueteur_personnel_id` INT UNSIGNED NULL COMMENT 'FK to personnel — Enquêteur',
    `partie_civile` VARCHAR(255) NULL COMMENT 'Partie civile (PC) — ST_PARQUET & PLAINTE_DIRECTE',
    `mise_en_cause` VARCHAR(255) NULL COMMENT 'Mise en cause (MC)',
    `adresse_pc` TEXT NULL COMMENT 'Adresse du PC — ST_PARQUET & PLAINTE_DIRECTE',
    `infraction` VARCHAR(255) NULL COMMENT 'Infraction',
    `prejudice` TEXT NULL COMMENT 'Préjudice',
    `lieu_infraction` VARCHAR(255) NULL COMMENT 'Lieu de l''infraction',
    `heure_infraction` TIME NULL COMMENT 'Heure de l''infraction',
    `observation` TEXT NULL COMMENT 'Observation',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the complaint',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_plainte_entree_numero_dossier` (`numero_dossier`),
    CONSTRAINT `fk_plainte_entree_opj` FOREIGN KEY (`opj_personnel_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_plainte_entree_enqueteur` FOREIGN KEY (`enqueteur_personnel_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_plainte_entree_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_plainte_entree_type` (`type`),
    INDEX `idx_plainte_entree_date` (`date_plainte`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_plainte_entree` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `plainte_entree_id` INT UNSIGNED NOT NULL COMMENT 'FK to plainte_entree',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_plainte_entree` FOREIGN KEY (`plainte_entree_id`) REFERENCES `plainte_entree`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_plainte_entree_id` (`plainte_entree_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
