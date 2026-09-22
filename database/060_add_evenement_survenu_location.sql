-- GPS location for "Évènements survenus" (Service Général).
-- The mobile app captures the device's latitude/longitude when an
-- évènement is recorded (same pattern as 028_add_armement_location).

ALTER TABLE `evenement_survenu`
    ADD COLUMN `latitude` DECIMAL(10, 7) NULL COMMENT 'GPS latitude captured at record time (mobile only)' AFTER `mesures_prises`,
    ADD COLUMN `longitude` DECIMAL(10, 7) NULL COMMENT 'GPS longitude captured at record time (mobile only)' AFTER `latitude`;
