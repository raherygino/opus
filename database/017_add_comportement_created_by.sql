-- Track who created each comportement record so the API can notify the
-- creator when an administrator confirms or rejects their submission.

ALTER TABLE comportement_personnel
    ADD COLUMN created_by INT UNSIGNED NULL AFTER rejected_reason;

ALTER TABLE comportement_personnel
    ADD CONSTRAINT fk_comportement_created_by
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE comportement_personnel
    ADD INDEX idx_comportement_created_by (created_by);
