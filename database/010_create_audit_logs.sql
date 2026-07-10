CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT UNSIGNED NULL COMMENT 'User who performed the action',
    `action` VARCHAR(50) NOT NULL COMMENT 'Action type: login, logout, create, update, delete, etc.',
    `module` VARCHAR(50) NOT NULL COMMENT 'Module affected: auth, personnel, users, roles, mouvements',
    `entity_id` INT UNSIGNED NULL COMMENT 'ID of the affected entity',
    `description` TEXT NULL COMMENT 'Human-readable description of the action',
    `old_values` JSON NULL COMMENT 'Previous values (for updates)',
    `new_values` JSON NULL COMMENT 'New values (for creates/updates)',
    `ip_address` VARCHAR(45) NULL COMMENT 'IP address of the requester',
    `user_agent` VARCHAR(500) NULL COMMENT 'User-Agent header',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_audit_user` (`user_id`),
    INDEX `idx_audit_action` (`action`),
    INDEX `idx_audit_module` (`module`),
    INDEX `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
