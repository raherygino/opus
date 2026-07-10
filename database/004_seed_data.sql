-- ============================================
-- Seed: Roles (only SUPER_ADMIN by default)
-- All other roles are created by admin via UI
-- ============================================
INSERT INTO `roles` (`code`, `name`, `description`) VALUES
('SUPER_ADMIN', 'Super Administrator', 'Full system access, configuration, user management, audit logs');

-- ============================================
-- Seed: Personnel (only admin user)
-- ============================================
INSERT INTO `personnel` (`im`, `matricule`, `lastname`, `firstname`, `grade`, `fonction`, `service`, `email`, `phone`) VALUES
('000000', '', 'Admin', 'System', 'Commissaire de Police', 'Super Administrator', 'Administration', 'admin@opus.test', '+221 77 000 00 01');

-- ============================================
-- Seed: Super Admin User (password = "admin123")
-- ============================================
INSERT INTO `users` (`personnel_id`, `username`, `password_hash`, `role_id`, `is_active`) VALUES
(1, 'admin', '$2y$12$s/ragHrU9Vf4gP1CzxXvvODFx5c60eFDCGXI2k8LZEKxQ.8hs1WDm', (SELECT id FROM `roles` WHERE `code` = 'SUPER_ADMIN'), 1);
