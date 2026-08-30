-- Add agent verification + signature columns to the armement table.
-- When a weapon is handed over (perception), the agent preneur's identity
-- is verified using their personnel "Code secret". The verification result
-- and timestamp are stored permanently on the armement record. The agent's
-- signature is captured after successful verification and stored as SVG
-- vector data (consistent with personnel.signature_svg). These fields are
-- set once at perception time and cannot be modified by the regular update
-- endpoint — they are protected the same way as the reintegration columns.

ALTER TABLE `armement`
    ADD COLUMN `agent_verifie` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether the agent preneur identity was verified via code secret (0=non, 1=oui)' AFTER `etat_perception`,
    ADD COLUMN `agent_verifie_at` TIMESTAMP NULL COMMENT 'When the agent identity was verified' AFTER `agent_verifie`,
    ADD COLUMN `signature_svg` LONGTEXT NULL COMMENT 'SVG vector data of the agent signature captured at perception' AFTER `agent_verifie_at`;
