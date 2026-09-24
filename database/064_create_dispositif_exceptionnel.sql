-- Dispositif exceptionnel (Service Général) — exceptional security operations
-- (VIP visits, events, emergencies). One main record per operation plus a
-- child table of "Effectif engagé" rows:
--   * dispositif_exceptionnel          (main record: nature + période)
--   * dispositif_exceptionnel_effectif (1-to-many sector rows)

CREATE TABLE IF NOT EXISTS `dispositif_exceptionnel` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `nature_evenement` VARCHAR(255) NOT NULL COMMENT 'Nature de l''évènement (ex: visite VIP, manifestation)',
    `date_debut` DATE NOT NULL COMMENT 'Début de la période du dispositif',
    `date_fin` DATE NOT NULL COMMENT 'Fin de la période du dispositif',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the dispositif',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_dispositif_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_dispositif_date_debut` (`date_debut`),
    INDEX `idx_dispositif_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dispositif_exceptionnel_effectif` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `dispositif_id` INT UNSIGNED NOT NULL,
    `secteur` VARCHAR(255) NOT NULL COMMENT 'Secteur',
    `chef_element_contact` VARCHAR(255) NULL COMMENT 'Chef d''élément avec contact',
    `controle_contact` VARCHAR(255) NULL COMMENT 'Contrôle avec contact',
    `materiels_armements` TEXT NULL COMMENT 'Matériels et armements',
    `missions` TEXT NULL COMMENT 'Missions',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_dispositif_effectif` FOREIGN KEY (`dispositif_id`) REFERENCES `dispositif_exceptionnel`(`id`) ON DELETE CASCADE,
    INDEX `idx_dispositif_effectif_dispositif` (`dispositif_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
