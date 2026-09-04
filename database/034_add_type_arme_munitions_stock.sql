-- Move ammunition stock management to the type_arme level.
-- Munitions are shared across all weapons of the same type (e.g. all
-- "Pistolet PA 9mm" use the same 9mm rounds regardless of matricule).
-- The arme.munitions_stock column is kept for backward compatibility
-- with historical data but is no longer the active stock — the
-- type_arme.munitions_stock column is the source of truth.

ALTER TABLE `type_arme`
    ADD COLUMN `munitions_stock` INT UNSIGNED NOT NULL DEFAULT 0
    COMMENT 'Shared ammunition stock for all weapons of this type'
    AFTER `description`;
