-- ============================================
-- Seed: Roles (only SUPER_ADMIN by default)
-- All other roles are created by admin via UI
-- ============================================
INSERT INTO `roles` (`code`, `name`, `description`) VALUES
('SUPER_ADMIN', 'Super Administrator', 'Full system access, configuration, user management, audit logs');

-- ============================================
-- Seed: Personnel (only admin user)
-- ============================================
INSERT INTO `personnel` (`im`, `grade`, `lastname`, `firstname`, `affectation`, `phone`) VALUES
('000000', 'Commissaire de Police', 'RAKOTO', 'Harilala', 'Administration', '+261 34 00 00 001');

-- ============================================
-- Seed: Super Admin User (password = "admin123")
-- ============================================
INSERT INTO `users` (`personnel_id`, `username`, `password_hash`, `role_id`, `is_active`) VALUES
(1, 'admin', '$2y$12$Jamh6zvN2HnD5NqhWAKRveeuWyJ3ahZlXxxVTU.Yu4tw2E.yZfeqC', (SELECT id FROM `roles` WHERE `code` = 'SUPER_ADMIN'), 1);
