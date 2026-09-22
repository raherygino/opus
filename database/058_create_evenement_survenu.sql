-- Évènements survenus (Service Général) — events that occurred on public roads.
-- Single-table feature: one record per event with the identities of the
-- parties involved (auteurs présumés / victimes / témoins) stored as text.
--
-- type_evenement is a code among: infraction | incident | accident | autre
-- (labels are rendered in French by the clients).

CREATE TABLE IF NOT EXISTS `evenement_survenu` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `date_evenement` DATE NOT NULL COMMENT 'Date de l''événement',
    `heure_evenement` VARCHAR(10) NOT NULL COMMENT 'Heure de l''événement (HH:MM)',
    `type_evenement` VARCHAR(100) NOT NULL COMMENT 'Type d''événement (infraction|incident|accident|autre)',
    `lieu_exact` VARCHAR(255) NOT NULL COMMENT 'Lieu exact de l''événement',
    `auteurs_presumes` TEXT NULL COMMENT 'Identités des auteur(s) présumé(s)',
    `victimes` TEXT NULL COMMENT 'Identités des victime(s)',
    `temoins` TEXT NULL COMMENT 'Identités des témoin(s)',
    `mesures_prises` TEXT NULL COMMENT 'Mesures prises',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the event',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_evenement_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_evenement_date` (`date_evenement`),
    INDEX `idx_evenement_type` (`type_evenement`),
    INDEX `idx_evenement_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
