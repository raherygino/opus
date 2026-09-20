-- Rassemblement Journalier (Service Général) — daily briefing records.
-- A rassemblement journalier has one main record plus three child tables:
--   * situation_prise_arme      (1-to-many, weapon-taking situation per row)
--   * repartition_secteur_diurne  (1-to-many, day-shift sector allocation)
--   * repartition_secteur_nocturne (1-to-many, night-shift sector allocation)

CREATE TABLE IF NOT EXISTS `rassemblement_journalier` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `date_rassemblement` DATE NOT NULL COMMENT 'Date du rassemblement',
    `heure_rassemblement` VARCHAR(10) NOT NULL COMMENT 'Heure du rassemblement (HH:MM)',
    `brigade_service` VARCHAR(255) NOT NULL COMMENT 'Brigade de service',
    `officier_permanence` VARCHAR(255) NULL COMMENT 'Officier de permanence',
    `inspecteur_permanence` VARCHAR(255) NULL COMMENT 'Inspecteur de permanence',
    `chef_poste` VARCHAR(255) NULL COMMENT 'Chef de poste',
    `instructions_autorite` TEXT NULL COMMENT 'Instructions de l''autorité',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the rassemblement',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_rassemblement_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_rassemblement_date` (`date_rassemblement`),
    INDEX `idx_rassemblement_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `situation_prise_arme` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `rassemblement_id` INT UNSIGNED NOT NULL,
    `effectif_theorique` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Effectif théorique',
    `present` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Présent',
    `absent` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Absent',
    `motif_absence` TEXT NULL COMMENT 'Motif d''absence',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_situation_prise_arme` FOREIGN KEY (`rassemblement_id`) REFERENCES `rassemblement_journalier`(`id`) ON DELETE CASCADE,
    INDEX `idx_situation_rassemblement` (`rassemblement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `repartition_secteur_diurne` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `rassemblement_id` INT UNSIGNED NOT NULL,
    `secteur` VARCHAR(255) NOT NULL COMMENT 'Secteur',
    `effectif_engage` VARCHAR(100) NULL COMMENT 'Effectif engagé',
    `chef_element_contact` VARCHAR(255) NULL COMMENT 'Chef d''élément avec contact',
    `controle_contact` VARCHAR(255) NULL COMMENT 'Contrôle avec contact',
    `materiels_armements` TEXT NULL COMMENT 'Matériels et armements',
    `missions` TEXT NULL COMMENT 'Missions',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_repartition_diurne` FOREIGN KEY (`rassemblement_id`) REFERENCES `rassemblement_journalier`(`id`) ON DELETE CASCADE,
    INDEX `idx_repartition_diurne_rassemblement` (`rassemblement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `repartition_secteur_nocturne` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `rassemblement_id` INT UNSIGNED NOT NULL,
    `secteur` VARCHAR(255) NOT NULL COMMENT 'Secteur',
    `effectif_engage` VARCHAR(100) NULL COMMENT 'Effectif engagé',
    `chef_element_contact` VARCHAR(255) NULL COMMENT 'Chef d''élément avec contact',
    `controle_contact` VARCHAR(255) NULL COMMENT 'Contrôle avec contact',
    `materiels_armements` TEXT NULL COMMENT 'Matériels et armements',
    `missions` TEXT NULL COMMENT 'Missions',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_repartition_nocturne` FOREIGN KEY (`rassemblement_id`) REFERENCES `rassemblement_journalier`(`id`) ON DELETE CASCADE,
    INDEX `idx_repartition_nocturne_rassemblement` (`rassemblement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
