-- Main courante categories — dynamic label catalog.
-- The main_courante.categorie column stores the label STRING (not an id),
-- so renaming a label here does not break historical entries. Categories
-- are managed by users through a dedicated dialog on the form page.

CREATE TABLE IF NOT EXISTS `main_courante_categorie` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `label` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Category label stored verbatim in main_courante.categorie',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed the three required default categories.
INSERT INTO `main_courante_categorie` (`label`) VALUES
    ('Entrée/Sortie de tiers'),
    ('Incident au poste'),
    ('Renseignement reçu');
