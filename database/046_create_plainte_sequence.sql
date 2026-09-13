-- Plainte sequence counter — reusable dossier/record number generation.
-- Each (type_key, year) pair has its own independent counter that resets
-- to 1 at the start of each new year. The unique constraint on
-- (type_key, year) guarantees correctness under concurrent inserts via
-- INSERT ... ON DUPLICATE KEY UPDATE.
--
-- type_key values: ST, PD, RP (ENTRÉE types) and SORTIE (sortie number).

CREATE TABLE IF NOT EXISTS `plainte_sequence` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `type_key` VARCHAR(20) NOT NULL COMMENT 'Sequence type: ST, PD, RP, SORTIE',
    `year` SMALLINT UNSIGNED NOT NULL COMMENT '2-digit year (e.g. 26 for 2026)',
    `last_number` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Last assigned sequence number',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_plainte_sequence_type_year` (`type_key`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
