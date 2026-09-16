-- Personne Recherchée (Police Judiciaire).
-- Tracks wanted persons with a dedicated multi-image table. Unlike the
-- generic PJ attachment system, photos here use a dedicated table with
-- image-specific metadata (caption, capture source, dimensions).

CREATE TABLE IF NOT EXISTS `personne_recherchee` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `nom` VARCHAR(255) NOT NULL COMMENT 'Nom de la personne recherchée',
    `adresse` TEXT NULL COMMENT 'Dernière adresse connue',
    `motif` TEXT NOT NULL COMMENT 'Motif de la recherche',
    `created_by` INT UNSIGNED NULL COMMENT 'Agent who recorded the entry',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_pr_created_by` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_pr_nom` (`nom`),
    INDEX `idx_pr_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dedicated image table for Personne Recherchée photos.
-- NOT the generic PJ attachment system — this is image-specific.
CREATE TABLE IF NOT EXISTS `personne_recherchee_photo` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `personne_recherchee_id` INT UNSIGNED NOT NULL COMMENT 'FK to personne_recherchee',
    `caption` VARCHAR(255) NULL COMMENT 'Optional caption for the photo',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Stored filename on disk',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original upload filename',
    `mime_type` VARCHAR(100) NULL COMMENT 'Image MIME type (always image/*)',
    `file_size` INT UNSIGNED NULL COMMENT 'File size in bytes',
    `width` INT UNSIGNED NULL COMMENT 'Image width in pixels',
    `height` INT UNSIGNED NULL COMMENT 'Image height in pixels',
    `capture_source` ENUM('CAMERA','GALLERY') NULL COMMENT 'How the image was acquired',
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Display order',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_pr_photo_personne` FOREIGN KEY (`personne_recherchee_id`) REFERENCES `personne_recherchee`(`id`) ON DELETE CASCADE,
    INDEX `idx_pr_photo_personne_id` (`personne_recherchee_id`),
    INDEX `idx_pr_photo_sort` (`personne_recherchee_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
