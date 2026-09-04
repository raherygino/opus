-- Type d'arme (weapon type catalogue — referenced by the arme table).
-- Each weapon (arme) belongs to exactly one type (e.g. "Pistolet PA 9mm",
-- "Fusil AK-47"). The type name is unique so the catalogue stays clean.

CREATE TABLE IF NOT EXISTS `type_arme` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `nom` VARCHAR(100) NOT NULL COMMENT 'Weapon type name (e.g. Pistolet PA 9mm)',
    `description` TEXT NULL COMMENT 'Optional description of the weapon type',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_type_arme_nom` (`nom`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
