-- Add agent verification + signature columns to affectation_materiel
-- (mirrors the armement perception flow: code secret verification + optional signature SVG)
--
-- agent_verifie      : 1 when the agent's code secret was verified at perception time
-- agent_verifie_at   : timestamp of the verification
-- signature_svg      : optional SVG signature captured after verification

ALTER TABLE `affectation_materiel`
    ADD COLUMN `agent_verifie` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether the agent code secret was verified at perception time' AFTER `observations`,
    ADD COLUMN `agent_verifie_at` DATETIME NULL COMMENT 'When the agent identity was verified' AFTER `agent_verifie`,
    ADD COLUMN `signature_svg` LONGTEXT NULL COMMENT 'Optional SVG signature captured at perception time' AFTER `agent_verifie_at`;
