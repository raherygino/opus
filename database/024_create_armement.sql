-- Armement (perception / réintégration d'armes — Sédentaire > Poste)
-- An armement records the "perception" (a weapon being handed to an agent)
-- and later its "réintégration" (the weapon being returned). The agent
-- preneur identity (IM + grade + name) is snapshotted from the personnel
-- table at perception time so the historical record stays accurate even if
-- the personnel's grade/name changes later. The personnel_id FK is kept for
-- referential integrity and audit. The snapshot columns are the
-- authoritative display values.
-- A weapon is "en cours de perception" while heure_reintegration IS NULL and
-- "réintégrée" once the reintegration fields are filled. Reintegration is a
-- one-way transition handled by ArmementController::reintegrate() — the
-- regular update endpoint cannot touch the reintegration columns.

CREATE TABLE IF NOT EXISTS `armement` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `date_perception` DATE NOT NULL COMMENT 'Date of the weapon handout (perception)',
    `heure_perception` TIME NOT NULL COMMENT 'Time at which the weapon was handed over',
    `agent_preneur_personnel_id` INT UNSIGNED NULL COMMENT 'Personnel taking the weapon (agent preneur)',
    `agent_preneur_im` VARCHAR(20) NULL COMMENT 'Snapshot of the agent preneur IM (matricule)',
    `agent_preneur_grade` VARCHAR(100) NULL COMMENT 'Snapshot of the agent preneur grade',
    `agent_preneur_nom` VARCHAR(255) NULL COMMENT 'Snapshot of the agent preneur full name',
    `type_arme` VARCHAR(100) NOT NULL COMMENT 'Weapon type (e.g. Pistolet PA 9mm)',
    `matricule_arme` VARCHAR(100) NOT NULL COMMENT 'Weapon serial / matricule',
    `munitions` INT UNSIGNED NULL COMMENT 'Rounds handed over at perception',
    `secteur_mission` VARCHAR(255) NULL COMMENT 'Secteur / mission the weapon is issued for',
    `etat_perception` TEXT NULL COMMENT 'Weapon state at perception',
    `heure_reintegration` TIME NULL COMMENT 'Time at which the weapon was returned (NULL = en cours)',
    `etat_reintegration` TEXT NULL COMMENT 'Weapon state at reintegration',
    `munitions_consommees` INT UNSIGNED NULL COMMENT 'Rounds used during the mission',
    `created_by` INT UNSIGNED NULL COMMENT 'User who recorded the perception',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_armement_agent_preneur` FOREIGN KEY (`agent_preneur_personnel_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_armement_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_armement_date` (`date_perception`),
    INDEX `idx_armement_reintegration` (`heure_reintegration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
