-- Correspondance (entrant/sortant mail & document tracking)
-- The "numéro d'ordre" (reference) is unique per sens: the Entrant and
-- Sortant registries each keep their own numbering sequence.

CREATE TABLE IF NOT EXISTS `correspondance` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `date_correspondance` DATE NOT NULL COMMENT 'Date of the correspondence',
    `heure_enregistrement` TIME NOT NULL COMMENT 'Time at which it was registered',
    `sens` ENUM('Entrant','Sortant') NOT NULL COMMENT 'Incoming (Entrant) or outgoing (Sortant)',
    `reference` VARCHAR(100) NOT NULL COMMENT 'Numéro d''ordre / Référence',
    `emetteur_destinataire` VARCHAR(255) NOT NULL COMMENT 'Émetteur (Entrant) / Destinataire (Sortant)',
    `objet` VARCHAR(255) NOT NULL COMMENT 'Subject / purpose',
    `statut` ENUM('Enregistré','En traitement','Traité','Archivé') NOT NULL DEFAULT 'Enregistré',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent secrétariat (user who registered it)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_correspondance_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    UNIQUE KEY `uq_correspondance_sens_reference` (`sens`, `reference`),
    INDEX `idx_correspondance_date` (`date_correspondance`),
    INDEX `idx_correspondance_statut` (`statut`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attach_correspondance` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `correspondance_id` INT UNSIGNED NOT NULL COMMENT 'FK to correspondance',
    `title` VARCHAR(255) NOT NULL COMMENT 'Attachment title / description',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'File MIME type',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_attach_correspondance` FOREIGN KEY (`correspondance_id`) REFERENCES `correspondance`(`id`) ON DELETE CASCADE,
    INDEX `idx_attach_correspondance_id` (`correspondance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
