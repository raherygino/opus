-- ============================================
-- Seed: personnel (demo agents)
-- Idempotent: uses INSERT ... ON DUPLICATE KEY UPDATE
-- on the unique `im` column, so the script can be
-- re-run safely.
--
-- NOTE: this seed starts at id=2 — id=1 is reserved
-- for the SUPER_ADMIN personnel created by
-- 004_seed_data.sql (im = '000000'). Do NOT reuse
-- im='000000' here.
--
-- code_secret_hash is a real bcrypt hash of '1234'
-- for every row, so the armement perception flow
-- (which calls password_verify on the agent's code)
-- works out of the box on the dev DB. The hash is
-- never returned by any API response (Personnel::
-- stripSecret removes it).
-- ============================================

INSERT INTO `personnel`
    (`id`, `im`, `grade`, `lastname`, `firstname`, `affectation`, `phone`, `address`, `code_secret_hash`)
VALUES
-- ---------- Service Général (SG) ----------
(2,  '100001', 'Agent de Police',                          'RAKOTO',     'Jean',       'Service Général (SG)', '+261 34 11 00 001', 'Lot II A 23 Bis, Antananarivo',         '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(3,  '100002', 'Sous-Brigadier de Police',                 'RAVELOSON',  'Faly',       'Service Général (SG)', '+261 34 11 00 002', 'Lot IV B 12, Antananarivo',              '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(4,  '100003', 'Brigadier de Police',                      'ANDRIAM',    'Hery',       'Service Général (SG)', '+261 34 11 00 003', 'Lot I C 45, Antananarivo',               '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(5,  '100004', 'Brigadier Chef de Police',                 'RASOLOFO',   'Tahina',     'Service Général (SG)', '+261 34 11 00 004', 'Lot III D 7, Antananarivo',              '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(6,  '100005', 'Agent de Police',                          'RAKOTONIRINA','Sitraka',   'Service Général (SG)', '+261 34 11 00 005', 'Lot V A 18, Antananarivo',               '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
-- ---------- Police Judiciaire (PJ) ----------
(7,  '100006', 'Inspecteur de Police',                     'RANDRIA',    'Nirina',     'Police Judiciaire (PJ)','+261 34 22 00 001', 'Lot II B 34, Antananarivo',              '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(8,  '100007', 'Inspecteur Principale de Police',          'RAVELO',     'Olivier',    'Police Judiciaire (PJ)','+261 34 22 00 002', 'Lot IV C 21, Antananarivo',              '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(9,  '100008', 'Officier de Police',                       'ANDRY',      'Tiana',      'Police Judiciaire (PJ)','+261 34 22 00 003', 'Lot I D 56, Antananarivo',               '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(10, '100009', 'Officier Principale de Police',            'RANAIVO',    'Lalaina',    'Police Judiciaire (PJ)','+261 34 22 00 004', 'Lot III A 9, Antananarivo',              '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(11, '100010', 'Inspecteur de Police',                     'RABARY',     'Naina',      'Police Judiciaire (PJ)','+261 34 22 00 005', 'Lot V B 27, Antananarivo',               '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
-- ---------- Sédentaire ----------
(12, '100011', 'Agent de Police',                          'RANDRIAN',   'Solofo',     'Sédentaire',           '+261 34 33 00 001', 'Lot II C 11, Antananarivo',              '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(13, '100012', 'Brigadier de Police',                      'RAKOTOBE',   'Hanitra',    'Sédentaire',           '+261 34 33 00 002', 'Lot IV D 33, Antananarivo',              '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(14, '100013', 'Inspecteur de Police',                     'ANDRIANA',   'Tahiana',    'Sédentaire',           '+261 34 33 00 003', 'Lot I A 67, Antananarivo',               '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(15, '100014', 'Commissaire de Police',                    'RAVELOARISON','Eric',      'Sédentaire',           '+261 34 33 00 004', 'Lot III B 14, Antananarivo',             '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
-- ---------- Administration ----------
(16, '100015', 'Commissaire Principale de Police',         'RAKOTONDRA', 'Hery',       'Administration',       '+261 34 44 00 001', 'Lot II D 41, Antananarivo',              '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(17, '100016', 'Commissaire Divisionaire de Police',       'RANDRIAMBOLO','Niraina',   'Administration',       '+261 34 44 00 002', 'Lot IV A 5, Antananarivo',               '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(18, '100017', 'Officier Principale de Police',            'RASOANIRINA','Patrick',    'Administration',       '+261 34 44 00 003', 'Lot I B 78, Antananarivo',               '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(19, '100018', 'Inspecteur Principale de Police',          'ANDRIAMBOLO','Vololona',   'Administration',       '+261 34 44 00 004', 'Lot III C 22, Antananarivo',             '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi'),
(20, '100019', 'Brigadier Chef de Police',                 'RAVELOMANANA','Sahondra',  'Administration',       '+261 34 44 00 005', 'Lot V D 31, Antananarivo',               '$2y$10$mw.aBe914co8uNBjCXyweet0yeDodZ1YBnnNkSJMRIv0lxC0N8MJi')
ON DUPLICATE KEY UPDATE
    `grade`            = VALUES(`grade`),
    `lastname`         = VALUES(`lastname`),
    `firstname`        = VALUES(`firstname`),
    `affectation`      = VALUES(`affectation`),
    `phone`            = VALUES(`phone`),
    `address`          = VALUES(`address`),
    `code_secret_hash` = VALUES(`code_secret_hash`);
