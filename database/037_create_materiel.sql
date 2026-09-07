-- Matériel (equipment assignment & return — Sédentaire > Poste)
--
-- This feature manages the assignment (perception) and return (réintégration)
-- of equipment/materials to agents. One assignment can contain MULTIPLE
-- material types (e.g. Radio + Bâton + Gilet), each with its own unique
-- numéro_materiel (ID Matériel) and individual condition states.
--
-- Normalised as: Agent → AffectationMateriel (header) → Lignes (line items)
--
-- type_materiel        : catalogue of equipment types (Radio, Bâton, Gilet, …)
-- affectation_materiel : assignment header (agent, perception/reintegration dates, statut)
-- affectation_materiel_ligne : one row per material in the assignment
--   (type_materiel_id + numero_materiel + etat_emport + etat_reintegration)

CREATE TABLE IF NOT EXISTS `type_materiel` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `nom` VARCHAR(100) NOT NULL COMMENT 'Type name (e.g. Radio, Bâton, Gilet)',
    `description` TEXT NULL COMMENT 'Optional description',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_type_materiel_nom` (`nom`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `affectation_materiel` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `agent_personnel_id` INT UNSIGNED NOT NULL COMMENT 'FK to personnel (the agent receiving the equipment)',
    `agent_im` VARCHAR(20) NULL COMMENT 'Snapshot of the agent IM (matricule)',
    `agent_grade` VARCHAR(100) NULL COMMENT 'Snapshot of the agent grade',
    `agent_nom` VARCHAR(255) NULL COMMENT 'Snapshot of the agent full name',
    `date_perception` DATE NOT NULL COMMENT 'Date the equipment was handed over',
    `heure_perception` TIME NOT NULL COMMENT 'Time the equipment was handed over',
    `date_reintegration` DATE NULL COMMENT 'Date the equipment was returned (NULL = still assigned)',
    `heure_reintegration` TIME NULL COMMENT 'Time the equipment was returned (NULL = still assigned)',
    `statut` ENUM('Assigné','Réintégré') NOT NULL DEFAULT 'Assigné' COMMENT 'Assignment status',
    `observations` TEXT NULL COMMENT 'Optional observations',
    `created_by` INT UNSIGNED NULL COMMENT 'User who recorded the assignment',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_affectation_materiel_agent` FOREIGN KEY (`agent_personnel_id`) REFERENCES `personnel`(`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_affectation_materiel_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_affectation_materiel_agent` (`agent_personnel_id`),
    INDEX `idx_affectation_materiel_date` (`date_perception`),
    INDEX `idx_affectation_materiel_statut` (`statut`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `affectation_materiel_ligne` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `affectation_id` INT UNSIGNED NOT NULL COMMENT 'FK to affectation_materiel',
    `type_materiel_id` INT UNSIGNED NOT NULL COMMENT 'FK to type_materiel',
    `type_materiel_nom` VARCHAR(100) NOT NULL COMMENT 'Snapshot of the type name at assignment time',
    `numero_materiel` VARCHAR(100) NOT NULL COMMENT 'Unique ID Matériel (serial / inventory number)',
    `etat_emport` VARCHAR(100) NULL COMMENT 'Condition state at issue (perception)',
    `etat_reintegration` VARCHAR(100) NULL COMMENT 'Condition state at return (réintégration)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_am_ligne_affectation` FOREIGN KEY (`affectation_id`) REFERENCES `affectation_materiel`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_am_ligne_type_materiel` FOREIGN KEY (`type_materiel_id`) REFERENCES `type_materiel`(`id`) ON DELETE RESTRICT,
    INDEX `idx_am_ligne_affectation` (`affectation_id`),
    INDEX `idx_am_ligne_type` (`type_materiel_id`),
    INDEX `idx_am_ligne_numero` (`numero_materiel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
