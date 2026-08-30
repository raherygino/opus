-- Add code_secret_hash column to personnel table.
-- The "Code secret" is a per-personnel PIN used only to verify the identity
-- of an agent when they receive a weapon (Armement perception). It is
-- independent of the application login/password — a personnel without a
-- user account can still have a code secret and receive an arme.
-- The code is stored as a bcrypt hash (never plaintext) and is never
-- returned by any API response. Verification uses password_verify().

ALTER TABLE `personnel`
    ADD COLUMN `code_secret_hash` VARCHAR(255) NULL COMMENT 'Bcrypt hash of the personnel secret code (for armement identity verification)' AFTER `signature_svg`;
