-- Matériel roulant (perception / réintégration de véhicules — Sédentaire > Poste)
-- A "matériel roulant" records the "perception" (a vehicle — VHL or Moto — being
-- handed to a driver + chef de bord) and later its "réintégration" (the vehicle
-- being returned). The driver (agent conducteur) and chef de bord identities
-- (IM + grade + name) are snapshotted from the personnel table at perception
-- time so the historical record stays accurate even if the personnel's
-- grade/name changes later. The personnel_id FKs are kept for referential
-- integrity and audit. The snapshot columns are the authoritative display values.
-- A vehicle is "En service" while heure_reintegration IS NULL and
-- "Réintégré" once the reintegration fields are filled. Reintegration is a
-- one-way transition handled by MaterielRoulantController::reintegrate() — the
-- regular update endpoint cannot touch the reintegration columns.
-- Operational history is preserved: the update endpoint can only modify the
-- perception fields. The reintegration fields (heure/kilometrage/carburant de
-- retour) and the technical observations/defaillances captured at return are
-- set once via the reintegration endpoint and never overwritten.

CREATE TABLE IF NOT EXISTS `materiel_roulant` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `date_perception` DATE NOT NULL COMMENT 'Date of the vehicle handout (perception)',
    `heure_perception` TIME NOT NULL COMMENT 'Time at which the vehicle was handed over',
    `type_materiel` VARCHAR(20) NOT NULL COMMENT 'Vehicle type: VHL or Moto',
    `numero_immatriculation` VARCHAR(50) NULL COMMENT 'Vehicle registration plate (numéro d immatriculation)',
    `description_vehicule` VARCHAR(255) NULL COMMENT 'Vehicle description (body type, brand, model — e.g. SUV, 4x4, Berline, Nissan, Toyota)',
    `agent_conducteur_personnel_id` INT UNSIGNED NULL COMMENT 'Personnel driving the vehicle (agent conducteur)',
    `agent_conducteur_im` VARCHAR(20) NULL COMMENT 'Snapshot of the driver IM (matricule)',
    `agent_conducteur_grade` VARCHAR(100) NULL COMMENT 'Snapshot of the driver grade',
    `agent_conducteur_nom` VARCHAR(255) NULL COMMENT 'Snapshot of the driver full name',
    `chef_de_bord_personnel_id` INT UNSIGNED NULL COMMENT 'Personnel acting as chef de bord (crew chief)',
    `chef_de_bord_im` VARCHAR(20) NULL COMMENT 'Snapshot of the chef de bord IM (matricule)',
    `chef_de_bord_grade` VARCHAR(100) NULL COMMENT 'Snapshot of the chef de bord grade',
    `chef_de_bord_nom` VARCHAR(255) NULL COMMENT 'Snapshot of the chef de bord full name',
    `kilometrage_depart` DECIMAL(10,1) NULL COMMENT 'Odometer reading at departure (km)',
    `niveau_carburant_depart` DECIMAL(5,2) NULL COMMENT 'Fuel level at departure (0-100%)',
    `heure_reintegration` TIME NULL COMMENT 'Time at which the vehicle was returned (NULL = en service)',
    `date_reintegration` DATE NULL COMMENT 'Date at which the vehicle was returned',
    `kilometrage_retour` DECIMAL(10,1) NULL COMMENT 'Odometer reading at return (km)',
    `niveau_carburant_retour` DECIMAL(5,2) NULL COMMENT 'Fuel level at return (0-100%)',
    `observations_techniques` TEXT NULL COMMENT 'General technical observations (distinct from defaillances)',
    `defaillances` TEXT NULL COMMENT 'Reported defects/failures (distinct from general observations)',
    `statut` VARCHAR(20) NOT NULL DEFAULT 'En service' COMMENT 'En service or Réintégré',
    `created_by` INT UNSIGNED NULL COMMENT 'User who recorded the perception',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_mr_agent_conducteur` FOREIGN KEY (`agent_conducteur_personnel_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_mr_chef_de_bord` FOREIGN KEY (`chef_de_bord_personnel_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_mr_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_mr_date` (`date_perception`),
    INDEX `idx_mr_statut` (`statut`),
    INDEX `idx_mr_agent_conducteur` (`agent_conducteur_personnel_id`),
    INDEX `idx_mr_reintegration` (`heure_reintegration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
