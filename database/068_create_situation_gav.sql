-- Situation GAV (Sédentaire > Poste — contrôles des personnes en garde à vue).
-- Each record references an existing garde_a_vue row (personne concernée) and
-- optionally a personnel row (agent ayant effectué le contrôle). No person
-- identity is duplicated here.

CREATE TABLE IF NOT EXISTS `situation_gav` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `garde_a_vue_id` INT UNSIGNED NOT NULL COMMENT 'Personne concernée (FK garde_a_vue)',
    `date_controle` DATETIME NOT NULL COMMENT 'Date et heure du contrôle',
    `agent_controle_id` INT UNSIGNED NULL COMMENT 'Agent ayant effectué le contrôle (FK personnel)',
    `etat_general` VARCHAR(255) NULL COMMENT 'État général de la personne',
    `observations` TEXT NULL COMMENT 'Observations',
    `mesures_prises` TEXT NULL COMMENT 'Mesures / actions prises suite au contrôle',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the situation',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_situation_gav_gav` FOREIGN KEY (`garde_a_vue_id`) REFERENCES `garde_a_vue`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_situation_gav_agent` FOREIGN KEY (`agent_controle_id`) REFERENCES `personnel`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_situation_gav_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_situation_gav_gav` (`garde_a_vue_id`),
    INDEX `idx_situation_gav_date` (`date_controle`),
    INDEX `idx_situation_gav_agent` (`agent_controle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_situation_gav` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `situation_gav_id` INT UNSIGNED NOT NULL COMMENT 'FK to situation_gav',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_situation_gav` FOREIGN KEY (`situation_gav_id`) REFERENCES `situation_gav`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_situation_gav_id` (`situation_gav_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
