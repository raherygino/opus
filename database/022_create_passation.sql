-- Passation (handover between two post/shift leaders — Sédentaire > Poste)
-- A passation records the handover from the "chef de poste descendant" (the
-- user logging out) to the "chef de poste montant" (the user authenticating
-- in). Both identities are snapshotted (grade + name) at passation time so
-- the historical record stays accurate even if the personnel's grade/name
-- changes later. The user_id FKs are kept for referential integrity and
-- audit. The snapshot columns are the authoritative display values.

CREATE TABLE IF NOT EXISTS `passation` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `date_passation` DATE NOT NULL COMMENT 'Date of the handover',
    `heure_passation` TIME NOT NULL COMMENT 'Time at which the handover was recorded',
    `chef_descendant_user_id` INT UNSIGNED NULL COMMENT 'User logging out (chef de poste descendant)',
    `chef_descendant_grade` VARCHAR(100) NULL COMMENT 'Snapshot of the descendant chef grade',
    `chef_descendant_lastname` VARCHAR(255) NULL COMMENT 'Snapshot of the descendant chef lastname',
    `chef_montant_user_id` INT UNSIGNED NULL COMMENT 'Authenticated incoming user (chef de poste montant)',
    `chef_montant_grade` VARCHAR(100) NULL COMMENT 'Snapshot of the montant chef grade',
    `chef_montant_lastname` VARCHAR(255) NULL COMMENT 'Snapshot of the montant chef lastname',
    `instructions_autorite` TEXT NULL COMMENT 'Instructions from the authority',
    `incidents_survenus` TEXT NULL COMMENT 'Incidents that occurred during the shift',
    `created_by` INT UNSIGNED NULL COMMENT 'User who recorded the passation (chef descendant)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_passation_chef_descendant` FOREIGN KEY (`chef_descendant_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_passation_chef_montant` FOREIGN KEY (`chef_montant_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_passation_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_passation_date` (`date_passation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
