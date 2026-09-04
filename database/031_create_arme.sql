-- Arme (individual weapon instance — the physical weapon identified by its
-- unique matricule/serial number). Each arme belongs to a type_arme and
-- tracks its current ammunition stock (munitions_stock). The stock is the
-- source of truth for how many rounds are currently available for this
-- weapon; it is decreased atomically when ammunition is consumed (either
-- via an armement reintegration recording munitions_consommees, or via the
-- dedicated POST /armes/{id}/consommation endpoint).
-- The matricule is unique across all weapons. Deleting an arme is rejected
-- when perceptions (armement) or consumption history reference it, so the
-- historical registry stays intact (ON DELETE RESTRICT on the FK from
-- armement.arme_id and arme_munitions_consommation.arme_id).

CREATE TABLE IF NOT EXISTS `arme` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `type_arme_id` INT UNSIGNED NOT NULL COMMENT 'FK to type_arme',
    `matricule` VARCHAR(100) NOT NULL COMMENT 'Unique weapon serial / matricule',
    `munitions_stock` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Current ammunition stock for this weapon',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_arme_type_arme` FOREIGN KEY (`type_arme_id`) REFERENCES `type_arme`(`id`) ON DELETE RESTRICT,
    UNIQUE KEY `uq_arme_matricule` (`matricule`),
    INDEX `idx_arme_type` (`type_arme_id`),
    INDEX `idx_arme_stock` (`munitions_stock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
