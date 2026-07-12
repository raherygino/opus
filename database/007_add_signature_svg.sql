-- Add signature_svg column to personnel table for storing vector signature data
ALTER TABLE `personnel`
    ADD COLUMN `signature_svg` LONGTEXT NULL COMMENT 'SVG vector data of the signature' AFTER `signature`;
