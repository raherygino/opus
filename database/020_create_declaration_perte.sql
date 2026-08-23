-- Déclaration de perte (loss declaration registry — Sédentaire > Secrétariat)
-- The "numéro d'attestation" is unique: each attestation delivered to a
-- declarant gets its own number in the registry.

CREATE TABLE IF NOT EXISTS `declaration_perte` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `date_declaration` DATE NOT NULL COMMENT 'Date of the declaration',
    `heure_declaration` TIME NOT NULL COMMENT 'Time at which the declaration was made',
    `identite_declarant` VARCHAR(255) NOT NULL COMMENT 'Identity of the declarant',
    `nature_objet` VARCHAR(255) NOT NULL COMMENT 'Nature of the lost item',
    `description_objet` TEXT NOT NULL COMMENT 'Description of the lost item',
    `date_perte` DATE NOT NULL COMMENT 'Presumed date of the loss',
    `lieu_perte` VARCHAR(255) NOT NULL COMMENT 'Presumed place of the loss',
    `numero_attestation` VARCHAR(100) NOT NULL COMMENT 'Numéro de l''attestation délivrée',
    `nom_agent` VARCHAR(255) NOT NULL COMMENT 'Name of the agent who received the declaration',
    `created_by` INT UNSIGNED NULL COMMENT 'User who registered the declaration',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_declaration_perte_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    UNIQUE KEY `uq_declaration_perte_attestation` (`numero_attestation`),
    INDEX `idx_declaration_perte_date` (`date_declaration`),
    INDEX `idx_declaration_perte_declarant` (`identite_declarant`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
