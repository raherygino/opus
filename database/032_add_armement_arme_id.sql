-- Link the existing armement (perception) table to the exact arme perceived.
-- The arme_id is nullable so legacy armement records (created before this
-- feature) remain valid. The existing snapshot columns type_arme and
-- matricule_arme stay authoritative for historical display — when an arme
-- is selected at perception time, those columns are snapshotted server-side
-- from the arme + type_arme so the record stays accurate even if the arme
-- is later modified or removed. ON DELETE SET NULL keeps the perception
-- record intact if the arme is ever deleted (the snapshot columns preserve
-- the weapon identity).

ALTER TABLE `armement`
    ADD COLUMN `arme_id` INT UNSIGNED NULL COMMENT 'FK to the exact arme perceived (nullable for legacy records)' AFTER `matricule_arme`,
    ADD CONSTRAINT `fk_armement_arme` FOREIGN KEY (`arme_id`) REFERENCES `arme`(`id`) ON DELETE SET NULL,
    ADD INDEX `idx_armement_arme` (`arme_id`);
