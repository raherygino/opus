-- Add agent verification + signature columns to materiel_roulant
-- (mirrors the armement & affectation_materiel perception flow:
--  code secret verification + optional signature SVG)
--
-- agent_verifie      : 1 when the conducteur's code secret was verified at perception time
-- agent_verifie_at   : timestamp of the verification
-- signature_svg      : optional SVG signature captured after verification

ALTER TABLE `materiel_roulant`
    ADD COLUMN `agent_verifie` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether the conducteur code secret was verified at perception time' AFTER `defaillances`,
    ADD COLUMN `agent_verifie_at` DATETIME NULL COMMENT 'When the conducteur identity was verified' AFTER `agent_verifie`,
    ADD COLUMN `signature_svg` LONGTEXT NULL COMMENT 'Optional SVG signature captured at perception time' AFTER `agent_verifie_at`;
