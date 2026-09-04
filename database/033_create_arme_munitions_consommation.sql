-- Ammunition consumption history (auditable log of every ammunition
-- consumption event). Each row records which weapon (arme_id) was involved,
-- which agent (personnel) consumed the ammunition, how many rounds were
-- consumed, when, and — when the consumption happened during an armement
-- reintegration — which perception (armement_id) triggered it.
-- This table is the audit trail; the arme.munitions_stock column is the
-- current live stock. The two are kept consistent inside a single database
-- transaction on every consumption (see ArmeController::consommation and
-- ArmementController::reintegrate). ON DELETE RESTRICT on arme_id prevents
-- deleting a weapon that has consumption history; armement_id is SET NULL
-- so the history survives even if the perception record is removed.

CREATE TABLE IF NOT EXISTS `arme_munitions_consommation` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `arme_id` INT UNSIGNED NOT NULL COMMENT 'FK to the weapon whose ammunition was consumed',
    `agent_id` INT UNSIGNED NULL COMMENT 'FK to personnel who consumed the ammunition (agent preneur)',
    `armement_id` INT UNSIGNED NULL COMMENT 'FK to the perception that triggered this consumption (NULL for standalone consumption)',
    `quantite` INT UNSIGNED NOT NULL COMMENT 'Number of rounds consumed',
    `date_consommation` DATETIME NOT NULL COMMENT 'When the consumption occurred',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_consommation_arme` FOREIGN KEY (`arme_id`) REFERENCES `arme`(`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_consommation_agent` FOREIGN KEY (`agent_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_consommation_armement` FOREIGN KEY (`armement_id`) REFERENCES `armement`(`id`) ON DELETE SET NULL,
    INDEX `idx_consommation_arme` (`arme_id`),
    INDEX `idx_consommation_agent` (`agent_id`),
    INDEX `idx_consommation_date` (`date_consommation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
