-- Add GPS coordinates to armement records.
-- On Android, the agent must enable location services and the app
-- captures the device's latitude/longitude when an armement perception
-- is created. Desktop does not capture location (no GPS hardware). The
-- fields are nullable so desktop-created armements remain valid.

ALTER TABLE `armement`
    ADD COLUMN `latitude` DECIMAL(10, 7) NULL COMMENT 'GPS latitude captured at perception time (mobile only)' AFTER `signature_svg`,
    ADD COLUMN `longitude` DECIMAL(10, 7) NULL COMMENT 'GPS longitude captured at perception time (mobile only)' AFTER `latitude`;
