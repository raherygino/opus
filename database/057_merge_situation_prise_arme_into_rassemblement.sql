-- Rassemblement Journalier — integrate "Situation de prise d'arme" into the main
-- rassemblement_journalier table and unify the two sector-allocation tables
-- (diurne / nocturne) into a single repartition_secteur table differentiated
-- by a `type` column.
--
-- After this migration the feature has:
--   * rassemblement_journalier  (main record, incl. situation de prise d'arme)
--   * repartition_secteur       (1-to-many rows, type = 'diurne' | 'nocturne')

ALTER TABLE `rassemblement_journalier`
    ADD COLUMN `effectif_theorique` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Effectif théorique (situation de prise d''arme)' AFTER `instructions_autorite`,
    ADD COLUMN `present` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Présent (situation de prise d''arme)' AFTER `effectif_theorique`,
    ADD COLUMN `absent` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Absent (situation de prise d''arme)' AFTER `present`,
    ADD COLUMN `motif_absence` TEXT NULL COMMENT 'Motif d''absence (situation de prise d''arme)' AFTER `absent`;

-- Migrate existing situation rows into the main table (aggregated per rassemblement).
UPDATE `rassemblement_journalier` r
JOIN (
    SELECT `rassemblement_id`,
           SUM(`effectif_theorique`) AS `effectif_theorique`,
           SUM(`present`) AS `present`,
           SUM(`absent`) AS `absent`,
           NULLIF(GROUP_CONCAT(NULLIF(`motif_absence`, '') SEPARATOR ' | '), '') AS `motif_absence`
    FROM `situation_prise_arme`
    GROUP BY `rassemblement_id`
) s ON s.`rassemblement_id` = r.`id`
SET r.`effectif_theorique` = s.`effectif_theorique`,
    r.`present` = s.`present`,
    r.`absent` = s.`absent`,
    r.`motif_absence` = s.`motif_absence`;

DROP TABLE IF EXISTS `situation_prise_arme`;

CREATE TABLE IF NOT EXISTS `repartition_secteur` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `rassemblement_id` INT UNSIGNED NOT NULL,
    `type` ENUM('diurne','nocturne') NOT NULL COMMENT 'Diurne ou Nocturne',
    `secteur` VARCHAR(255) NOT NULL COMMENT 'Secteur',
    `effectif_engage` VARCHAR(100) NULL COMMENT 'Effectif engagé',
    `chef_element_contact` VARCHAR(255) NULL COMMENT 'Chef d''élément avec contact',
    `controle_contact` VARCHAR(255) NULL COMMENT 'Contrôle avec contact',
    `materiels_armements` TEXT NULL COMMENT 'Matériels et armements',
    `missions` TEXT NULL COMMENT 'Missions',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_repartition_secteur` FOREIGN KEY (`rassemblement_id`) REFERENCES `rassemblement_journalier`(`id`) ON DELETE CASCADE,
    INDEX `idx_repartition_rassemblement_type` (`rassemblement_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrate existing day-shift rows.
INSERT INTO `repartition_secteur`
    (`rassemblement_id`, `type`, `secteur`, `effectif_engage`, `chef_element_contact`, `controle_contact`, `materiels_armements`, `missions`)
SELECT `rassemblement_id`, 'diurne', `secteur`, `effectif_engage`, `chef_element_contact`, `controle_contact`, `materiels_armements`, `missions`
FROM `repartition_secteur_diurne`;

-- Migrate existing night-shift rows.
INSERT INTO `repartition_secteur`
    (`rassemblement_id`, `type`, `secteur`, `effectif_engage`, `chef_element_contact`, `controle_contact`, `materiels_armements`, `missions`)
SELECT `rassemblement_id`, 'nocturne', `secteur`, `effectif_engage`, `chef_element_contact`, `controle_contact`, `materiels_armements`, `missions`
FROM `repartition_secteur_nocturne`;

DROP TABLE IF EXISTS `repartition_secteur_diurne`;
DROP TABLE IF EXISTS `repartition_secteur_nocturne`;
