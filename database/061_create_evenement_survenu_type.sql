-- Évènement survenu types — dynamic label catalog.
-- The evenement_survenu.type_evenement column stores the label STRING
-- (not an id), so renaming a label here does not break historical entries.
-- Types are managed by users through a dedicated dialog on the form page
-- (same pattern as main_courante_categorie).

CREATE TABLE IF NOT EXISTS `evenement_survenu_type` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `label` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Type label stored verbatim in evenement_survenu.type_evenement',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed the four default event types.
INSERT INTO `evenement_survenu_type` (`label`) VALUES
    ('Infraction'),
    ('Incident'),
    ('Accident'),
    ('Autre');

-- Migrate existing rows that still store the legacy machine codes so the
-- stored value is always the label itself.
UPDATE `evenement_survenu` SET `type_evenement` = 'Infraction' WHERE `type_evenement` = 'infraction';
UPDATE `evenement_survenu` SET `type_evenement` = 'Incident'   WHERE `type_evenement` = 'incident';
UPDATE `evenement_survenu` SET `type_evenement` = 'Accident'   WHERE `type_evenement` = 'accident';
UPDATE `evenement_survenu` SET `type_evenement` = 'Autre'      WHERE `type_evenement` = 'autre';
