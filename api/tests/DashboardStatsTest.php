<?php

/**
 * Dashboard stats tests — aggregated KPI counts for the main dashboard.
 * Self-contained: builds a scratch database (opus_test_dashboard), applies
 * the migrations needed by DashboardStats, seeds rows, checks the counts,
 * then drops the scratch database. Never touches the main `opus` database.
 *
 * Usage: php api/tests/DashboardStatsTest.php
 */

$root = dirname(__DIR__, 2);
$dbConfig = require $root . '/api/config/database.php';

$failures = 0;
function check(bool $cond, string $label): void
{
    global $failures;
    if ($cond) {
        echo "  PASS  $label\n";
    } else {
        $failures++;
        echo "  FAIL  $label\n";
    }
}

// --- Scratch database setup -------------------------------------------------
$pdo = new PDO(
    "mysql:host={$dbConfig['host']};port={$dbConfig['port']};charset={$dbConfig['charset']}",
    $dbConfig['username'],
    $dbConfig['password'],
    [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
);
$scratch = 'opus_test_dashboard';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// --- Apply migrations in dependency order -----------------------------------
$migrations = [
    '001_create_roles.sql',
    '002_create_personnel.sql',
    '003_create_users.sql',
    '006_create_mouvement_personnel.sql',
    '008_create_role_permissions.sql',
    '009_create_notifications.sql',
    '018_add_notification_link.sql',
    '019_create_correspondance.sql',
    '020_create_declaration_perte.sql',
    '024_create_armement.sql',
    '039_create_materiel_roulant.sql',
    '042_create_main_courante.sql',
    '044_create_plainte.sql',
    '045_create_plainte_sortie.sql',
    '048_create_garde_a_vue.sql',
    '050_create_personne_recherchee.sql',
    '058_create_evenement_survenu.sql',
    '062_create_activite.sql',
];
foreach ($migrations as $file) {
    $sql = file_get_contents($root . '/database/' . $file);
    foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
        if (preg_match('/^\s*(CREATE|ALTER|INSERT)/i', preg_replace('/^--.*$/m', '', $stmt))) {
            $pdo->exec($stmt);
        }
    }
}

// Point the app's Database singleton at the scratch DB.
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

set_exception_handler(function (Throwable $e) {
    fwrite(STDERR, 'ERROR: ' . $e->getMessage() . "\n");
    exit(1);
});

use App\Models\DashboardStats;

// --- Seed data ----------------------------------------------------------------
$pdo->exec("INSERT INTO roles (code, name) VALUES ('SUPER_ADMIN', 'Super Admin'), ('OFFICER', 'Officier')");
$roleAdmin = (int) $pdo->query("SELECT id FROM roles WHERE code='SUPER_ADMIN'")->fetchColumn();
$roleOfficer = (int) $pdo->query("SELECT id FROM roles WHERE code='OFFICER'")->fetchColumn();

// 3 personnel — one will be "en mouvement" (retour = Non)
$pdo->exec("INSERT INTO personnel (im, grade, lastname, firstname, affectation) VALUES
    ('IM1', 'Officier', 'Rakoto', 'Jean', 'Sédentaire'),
    ('IM2', 'Brigadier', 'Rabe', 'Paul', 'Service Général'),
    ('IM3', 'Inspecteur', 'Randria', 'Luc', 'Police Judiciaire')");
$pdo->exec("INSERT INTO mouvement_personnel (personnel_id, im, type_mouvement, retour) VALUES (2, 'IM2', 'Mission', 'Non')");
$pdo->exec("INSERT INTO mouvement_personnel (personnel_id, im, type_mouvement, retour) VALUES (3, 'IM3', 'Congé', 'Oui')");

// 2 users — one inactive
$pdo->exec("INSERT INTO users (personnel_id, username, password_hash, role_id, is_active) VALUES
    (1, 'admin', 'x', $roleAdmin, 1),
    (2, 'officer', 'x', $roleOfficer, 0)");
$adminId = (int) $pdo->query("SELECT id FROM users WHERE username='admin'")->fetchColumn();

// GAV: 1 en cours (fin NULL), 1 prolongée (future), 1 terminée (fin passée)
$pdo->exec("INSERT INTO garde_a_vue (nom, debut_gav, fin_gav) VALUES
    ('A', NOW(), NULL),
    ('B', NOW(), DATE_SUB(NOW(), INTERVAL 2 HOUR))");
$pdo->exec("UPDATE garde_a_vue SET prolongation_gav = DATE_ADD(NOW(), INTERVAL 12 HOUR) WHERE nom='B'");
$pdo->exec("INSERT INTO garde_a_vue (nom, debut_gav, fin_gav) VALUES
    ('C', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY))");

// Armement: 2 perçues, dont 1 réintégrée
$pdo->exec("INSERT INTO armement (date_perception, heure_perception, type_arme, matricule_arme, heure_reintegration) VALUES
    (CURDATE(), '08:00', 'PA', 'A1', NULL),
    (CURDATE(), '09:00', 'PA', 'A2', '18:00')");

// Matériel roulant: 2 en service, 1 réintégré
$pdo->exec("INSERT INTO materiel_roulant (date_perception, heure_perception, type_materiel, statut) VALUES
    (CURDATE(), '08:00', 'VHL', 'En service'),
    (CURDATE(), '08:30', 'Moto', 'En service'),
    (CURDATE(), '09:00', 'VHL', 'Réintégré')");

// Activités: 1 aujourd'hui, 1 il y a 3 jours, 1 il y a 30 jours
$pdo->exec("INSERT INTO activite (date_activite, heure_activite) VALUES
    (CURDATE(), '10:00'),
    (DATE_SUB(CURDATE(), INTERVAL 3 DAY), '10:00'),
    (DATE_SUB(CURDATE(), INTERVAL 30 DAY), '10:00')");

// Événement survenu aujourd'hui + main courante aujourd'hui
$pdo->exec("INSERT INTO evenement_survenu (date_evenement, heure_evenement, type_evenement, lieu_exact) VALUES
    (CURDATE(), '11:00', 'incident', 'Centre-ville')");
$pdo->exec("INSERT INTO main_courante (date_evenement, heure_evenement, categorie, description) VALUES
    (CURDATE(), '11:30', 'Incident au poste', 'Test')");

// Correspondances + déclarations
$pdo->exec("INSERT INTO correspondance (date_correspondance, heure_enregistrement, sens, reference, emetteur_destinataire, objet) VALUES
    (CURDATE(), '09:00', 'Entrant', 'REF1', 'Ministère', 'Objet 1'),
    (CURDATE(), '10:00', 'Sortant', 'REF2', 'Prefecture', 'Objet 2')");
$pdo->exec("INSERT INTO declaration_perte (date_declaration, heure_declaration, identite_declarant, nature_objet, description_objet, date_perte, lieu_perte, numero_attestation, nom_agent) VALUES
    (CURDATE(), '11:00', 'D1', 'CIN', 'Carte d identité', CURDATE(), 'Marché', 'ATT-1', 'Agent X')");

// Plaintes: 2 entrées, 1 avec sortie
$pdo->exec("INSERT INTO plainte_entree (type, date_plainte, numero_dossier) VALUES
    ('PLAINTE_DIRECTE', CURDATE(), 'N1'), ('RAPPORT_POLICE', CURDATE(), 'N2')");
$pdo->exec("INSERT INTO plainte_sortie (plainte_entree_id, nature, date_sortie, numero) VALUES (1, 'DAT', CURDATE(), 'S1')");

// Personnes recherchées
$pdo->exec("INSERT INTO personne_recherchee (nom, motif) VALUES ('X', 'Vol'), ('Y', 'Fraude')");

// Notifications: 2 non lues ciblant l'admin, 1 lue
$pdo->exec("INSERT INTO notifications (title, service, user_id, is_read) VALUES
    ('N1', 'System', $adminId, 0),
    ('N2', 'System', $adminId, 0),
    ('N3', 'System', $adminId, 1)");

// --- Assertions ---------------------------------------------------------------
$stats = DashboardStats::get($adminId, 'SUPER_ADMIN');

check($stats['personnel_total'] === 3, 'personnel_total = 3');
check($stats['personnel_en_service'] === 2, 'personnel_en_service = 2 (1 en mouvement)');
check($stats['personnel_en_mouvement'] === 1, 'personnel_en_mouvement = 1');
check($stats['mouvements_en_cours'] === 1, 'mouvements_en_cours = 1');
check($stats['users_total'] === 2, 'users_total = 2');
check($stats['users_actifs'] === 1, 'users_actifs = 1');
check($stats['gav_en_cours'] === 2, 'gav_en_cours = 2 (fin NULL + prolongation future)');
check($stats['armes_en_service'] === 1, 'armes_en_service = 1');
check($stats['vehicules_en_service'] === 2, 'vehicules_en_service = 2');
check($stats['activites_total'] === 3, 'activites_total = 3');
check($stats['activites_7j'] === 2, 'activites_7j = 2');
check($stats['activites_aujourdhui'] === 1, 'activites_aujourdhui = 1');
check($stats['evenements_aujourdhui'] === 1, 'evenements_aujourdhui = 1');
check($stats['main_courante_aujourdhui'] === 1, 'main_courante_aujourdhui = 1');
check($stats['correspondances_total'] === 2, 'correspondances_total = 2');
check($stats['declarations_perte_total'] === 1, 'declarations_perte_total = 1');
check($stats['plaintes_en_attente'] === 1, 'plaintes_en_attente = 1 (entrée sans sortie)');
check($stats['personnes_recherchees'] === 2, 'personnes_recherchees = 2');
check($stats['notifications_non_lues'] === 2, 'notifications_non_lues = 2 (admin voit tout)');
check(!empty($stats['generated_at']), 'generated_at présent');

// --- Cleanup ------------------------------------------------------------------
$pdo->exec("DROP DATABASE `$scratch`");

if ($failures > 0) {
    echo "\n$failures test(s) FAILED\n";
    exit(1);
}
echo "\nAll tests passed\n";
