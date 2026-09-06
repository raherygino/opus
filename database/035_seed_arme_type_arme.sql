-- ============================================
-- Seed: type_arme + arme catalogue
-- Idempotent: uses INSERT ... ON DUPLICATE KEY UPDATE
-- so the script can be re-run safely.
-- ============================================

-- ---------- type_arme ----------
-- munitions_stock is the shared stock per type (migration 034).
INSERT INTO `type_arme` (`id`, `nom`, `description`, `munitions_stock`) VALUES
(1, 'Pistolet PA 9mm',      'Pistolet automatique calibre 9mm — arme de poing réglementaire', 500),
(2, 'Fusil AK-47',          'Fusil d''assaut 7.62x39mm — arme d''épaule',                    1200),
(3, 'Fusil M16',            'Fusil d''assaut 5.56x45mm OTAN',                                1000),
(4, 'Pistolet Makarov',     'Pistolet semi-automatique 9x18mm Makarov',                     300),
(5, 'Fusil à pompe Mossberg','Fusil à pompe calibre 12 — arme d''épaule',                   250),
(6, 'Mitraillette UZI',     'Pistolet-mitrailleur 9mm',                                     800),
(7, 'Fusil de précision Dragunov','Fusil semi-automatique de précision 7.62x54mm',          400),
(8, 'Grenade lacrymogène',  'Grenade à main de dispersion de gaz lacrymogène',              600)
ON DUPLICATE KEY UPDATE
    `description`     = VALUES(`description`),
    `munitions_stock` = VALUES(`munitions_stock`);

-- ---------- arme ----------
-- Each row is a physical weapon instance identified by its unique matricule.
-- type_arme_id references the seeded type_arme rows above.
-- arme.munitions_stock is kept for backward compatibility (migration 034 moved
-- the active stock to type_arme.munitions_stock); we still seed it for history.
INSERT INTO `arme` (`id`, `type_arme_id`, `matricule`, `munitions_stock`) VALUES
-- Pistolet PA 9mm (type 1)
(1,  1, 'PA-0001', 15),
(2,  1, 'PA-0002', 15),
(3,  1, 'PA-0003', 15),
(4,  1, 'PA-0004', 15),
(5,  1, 'PA-0005', 15),
-- Fusil AK-47 (type 2)
(6,  2, 'AK-1001', 30),
(7,  2, 'AK-1002', 30),
(8,  2, 'AK-1003', 30),
(9,  2, 'AK-1004', 30),
(10, 2, 'AK-1005', 30),
-- Fusil M16 (type 3)
(11, 3, 'M16-2001', 30),
(12, 3, 'M16-2002', 30),
(13, 3, 'M16-2003', 30),
-- Pistolet Makarov (type 4)
(14, 4, 'MK-3001', 8),
(15, 4, 'MK-3002', 8),
-- Fusil à pompe Mossberg (type 5)
(16, 5, 'MS-4001', 5),
(17, 5, 'MS-4002', 5),
-- Mitraillette UZI (type 6)
(18, 6, 'UZ-5001', 25),
(19, 6, 'UZ-5002', 25),
-- Fusil de précision Dragunov (type 7)
(20, 7, 'DVG-6001', 10),
-- Grenade lacrymogène (type 8)
(21, 8, 'GR-7001', 0),
(22, 8, 'GR-7002', 0)
ON DUPLICATE KEY UPDATE
    `type_arme_id`    = VALUES(`type_arme_id`),
    `munitions_stock` = VALUES(`munitions_stock`);
