-- Activités (Service Général) — patrouilles et interventions.
-- Single-table feature: one record per activity. Patrol itineraries are
-- stored as six nullable columns — one per (type, mode) pair:
--   patrouille_{diurne|nocturne}_{motorisee|pedestre|portee}_itineraire
-- A NULL column means the patrol mode was NOT selected. An empty string
-- means the mode was selected without a detailed itinerary. This lets the
-- clients distinguish "mode not used" from "mode used, itinerary blank"
-- without a separate flag column.

CREATE TABLE IF NOT EXISTS `activite` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `date_activite` DATE NOT NULL COMMENT 'Date de l''activité',
    `heure_activite` VARCHAR(10) NOT NULL COMMENT 'Heure de l''activité (HH:MM)',
    `patrouille_diurne_motorisee_itineraire` TEXT NULL COMMENT 'Itinéraire patrouille diurne motorisée (NULL = non sélectionnée)',
    `patrouille_diurne_pedestre_itineraire` TEXT NULL COMMENT 'Itinéraire patrouille diurne pédestre (NULL = non sélectionnée)',
    `patrouille_diurne_portee_itineraire` TEXT NULL COMMENT 'Itinéraire patrouille diurne portée (NULL = non sélectionnée)',
    `patrouille_nocturne_motorisee_itineraire` TEXT NULL COMMENT 'Itinéraire patrouille nocturne motorisée (NULL = non sélectionnée)',
    `patrouille_nocturne_pedestre_itineraire` TEXT NULL COMMENT 'Itinéraire patrouille nocturne pédestre (NULL = non sélectionnée)',
    `patrouille_nocturne_portee_itineraire` TEXT NULL COMMENT 'Itinéraire patrouille nocturne portée (NULL = non sélectionnée)',
    `operation_ciblee` TEXT NULL COMMENT 'Opération ciblée (ex: Contrôle CIN, Contrôle débit de boissons)',
    `faits_constates` TEXT NULL COMMENT 'Faits constatés durant la patrouille ou l''opération',
    `compte_rendu_hierarchie` TEXT NULL COMMENT 'Compte-rendu temps réel à l''autorité et/ou au second',
    `conduite_a_tenir` TEXT NULL COMMENT 'Instructions données par l''autorité et/ou le second',
    `nature_intervention` TEXT NULL COMMENT 'Nature de l''intervention (ex: tapage nocturne, braquage en cours)',
    `suites_donnees` TEXT NULL COMMENT 'Suites données (ex: conduite au poste, interpellation, RAS)',
    `latitude` DECIMAL(10, 7) NULL COMMENT 'GPS latitude captured at record time (mobile only)',
    `longitude` DECIMAL(10, 7) NULL COMMENT 'GPS longitude captured at record time (mobile only)',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the activity',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_activite_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_activite_date` (`date_activite`),
    INDEX `idx_activite_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
