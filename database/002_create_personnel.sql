CREATE TABLE IF NOT EXISTS `personnel` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `im` VARCHAR(20) NOT NULL UNIQUE COMMENT 'Indice Matricule unique',
    `grade` VARCHAR(100) NOT NULL COMMENT 'Grade / Rank',
    `lastname` VARCHAR(100) NOT NULL COMMENT 'Nom de famille',
    `firstname` VARCHAR(100) NOT NULL COMMENT 'Prénoms',
    `affectation` VARCHAR(200) NULL COMMENT 'Affectation (Sédentaire, Police Judiciaire, Service Général)',
    `phone` VARCHAR(50) NULL,
    `address` TEXT NULL COMMENT 'Adresse personnelle',
    `photo` VARCHAR(255) NULL COMMENT 'Photo filename',
    `signature` VARCHAR(255) NULL COMMENT 'Signature image filename',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_personnel_grade` (`grade`),
    INDEX `idx_personnel_affectation` (`affectation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
