-- Add date_reintegration and reintegration GPS coordinates to armement.
-- The date of reintegration is recorded separately from the time so the
-- full timestamp (date + heure) is available. The reintegration
-- coordinates capture where the weapon was returned (required on mobile).

ALTER TABLE `armement`
    ADD COLUMN `date_reintegration` DATE NULL COMMENT 'Date of the weapon return (reintegration)' AFTER `heure_reintegration`,
    ADD COLUMN `reintegration_latitude` DECIMAL(10, 7) NULL COMMENT 'GPS latitude captured at reintegration (mobile only)' AFTER `munitions_consommees`,
    ADD COLUMN `reintegration_longitude` DECIMAL(10, 7) NULL COMMENT 'GPS longitude captured at reintegration (mobile only)' AFTER `reintegration_latitude`;
