ALTER TABLE `personnel`
    ADD COLUMN `thumbnail` VARCHAR(255) NULL COMMENT 'Thumbnail photo filename' AFTER `photo`;
