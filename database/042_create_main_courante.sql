-- Main courante (event logbook — Sédentaire > Secrétariat & Sédentaire > Poste)
-- A "main courante" entry records an event that occurred during a shift:
-- the period (date), the exact time, a category, and a free-text description
-- of the facts. The same table serves both the Secrétariat and Poste contexts,
-- distinguished by the `origine` column. Each context has its own permission
-- module code (sedentaire_secretariat_main_courante / sedentaire_poste_main_courante)
-- checked client-side, exactly like the other Sédentaire features.

CREATE TABLE IF NOT EXISTS `main_courante` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `date_evenement` DATE NOT NULL COMMENT 'Période — date associated with the entry',
    `heure_evenement` TIME NOT NULL COMMENT 'Heure précise — exact time of the event',
    `categorie` VARCHAR(50) NOT NULL COMMENT 'Catégorie de lévénement (Entrée/Sortie de tiers, Incident au poste, Renseignement reçu)',
    `description` TEXT NOT NULL COMMENT 'Description des faits',
    `origine` VARCHAR(20) NOT NULL DEFAULT 'Secretariat' COMMENT 'Secretariat or Poste — which context owns this entry',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the entry',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_main_courante_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_main_courante_date` (`date_evenement`),
    INDEX `idx_main_courante_origine` (`origine`),
    INDEX `idx_main_courante_categorie` (`categorie`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_main_courante` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `main_courante_id` INT UNSIGNED NOT NULL COMMENT 'FK to main_courante',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_main_courante` FOREIGN KEY (`main_courante_id`) REFERENCES `main_courante`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_main_courante_id` (`main_courante_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
