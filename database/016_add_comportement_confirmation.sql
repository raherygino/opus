-- Add confirmation workflow to comportement_personnel.
-- Staff-created records start as "pending" and require an administrator to
-- confirm or reject them. Records created directly by an administrator are
-- auto-confirmed (status = "confirmed", confirmed_by = creator).

ALTER TABLE comportement_personnel
    ADD COLUMN status ENUM('pending','confirmed','rejected') NOT NULL DEFAULT 'pending' AFTER decision,
    ADD COLUMN confirmed_by INT UNSIGNED NULL AFTER status,
    ADD COLUMN confirmed_at TIMESTAMP NULL AFTER confirmed_by,
    ADD COLUMN rejected_reason TEXT NULL AFTER confirmed_at;

-- Existing rows are treated as already validated.
UPDATE comportement_personnel SET status = 'confirmed' WHERE status = 'pending';

ALTER TABLE comportement_personnel
    ADD CONSTRAINT fk_comportement_confirmed_by
        FOREIGN KEY (confirmed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE comportement_personnel
    ADD INDEX idx_comportement_status (status);
