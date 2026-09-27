<?php

/**
 * Rapport tests — aggregated record counts per Sédentaire module for
 * daily / weekly (ISO Mon–Sun) / monthly periods.
 * Self-contained: builds a scratch database (opus_test_rapport), applies
 * the migrations needed by RapportStats, seeds rows with controlled
 * created_at/updated_at timestamps, checks the counts and period bounds,
 * then drops the scratch database. Never touches the main `opus` database.
 *
 * Usage: php api/tests/RapportTest.php
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
$scratch = 'opus_test_rapport';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// --- Apply migrations in dependency order -----------------------------------
$migrations = [
    '001_create_roles.sql',
    '002_create_personnel.sql',
    '003_create_users.sql',
    '006_create_mouvement_personnel.sql',
    '011_create_comportement_personnel.sql',
    '019_create_correspondance.sql',
    '020_create_declaration_perte.sql',
    '022_create_passation.sql',
    '024_create_armement.sql',
    '030_create_type_arme.sql',
    '031_create_arme.sql',
    '037_create_materiel.sql',
    '039_create_materiel_roulant.sql',
    '042_create_main_courante.sql',
    '048_create_garde_a_vue.sql',
];
foreach ($migrations as $file) {
    // Strip `--` comment lines BEFORE splitting on ';' — some comments contain
    // semicolons (e.g. 031_create_arme.sql) which would corrupt the split.
    $sql = preg_replace('/^--.*$/m', '', file_get_contents($root . '/database/' . $file));
    foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
        if (preg_match('/^\s*(CREATE|ALTER|INSERT)/i', $stmt)) {
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

use App\Models\RapportStats;

// --- Seed data ----------------------------------------------------------------
// Anchor date: Wednesday 2026-09-23 (ISO week 2026-09-21 → 2026-09-27,
// month September 2026). created_at / updated_at are set explicitly so the
// report period boundaries can be exercised deterministically.
// Referenced by FK-bearing tables (mouvement, comportement, affectation);
// kept outside every tested period so it never affects the counts.
$pdo->exec("INSERT INTO personnel (im, grade, lastname, firstname, affectation, created_at, updated_at) VALUES
    ('IM1', 'Officier', 'Rakoto', 'Jean', 'Sédentaire', '2026-08-01 09:00:00', '2026-08-01 09:00:00')");

$pdo->exec("INSERT INTO type_arme (nom) VALUES ('PA 9mm')");
$typeArmeId = (int) $pdo->query("SELECT id FROM type_arme LIMIT 1")->fetchColumn();

$pdo->exec("INSERT INTO correspondance (date_correspondance, heure_enregistrement, sens, reference, emetteur_destinataire, objet, created_at, updated_at) VALUES
    ('2026-09-23', '08:00', 'Entrant', 'R-A', 'Ministère', 'A', '2026-09-23 08:00:00', '2026-09-23 08:00:00'),
    ('2026-09-21', '06:00', 'Sortant', 'R-B', 'Préfecture', 'B', '2026-09-21 06:00:00', '2026-09-23 10:00:00'),
    ('2026-09-20', '06:00', 'Entrant', 'R-C', 'Ministère', 'C', '2026-09-20 06:00:00', '2026-09-20 06:00:00')");

$pdo->exec("INSERT INTO personnel (im, grade, lastname, firstname, affectation, created_at, updated_at) VALUES
    ('IM2', 'Brigadier', 'Rabe', 'Paul', 'Sédentaire', '2026-09-23 09:00:00', '2026-09-23 09:00:00'),
    ('IM3', 'Inspecteur', 'Randria', 'Luc', 'Sédentaire', '2026-09-18 09:00:00', '2026-09-18 09:00:00'),
    ('IM4', 'Officier', 'Rasoa', 'Mialy', 'Sédentaire', '2026-09-10 09:00:00', '2026-09-24 09:00:00')");

$pdo->exec("INSERT INTO mouvement_personnel (personnel_id, im, type_mouvement, retour, created_at, updated_at) VALUES
    (1, 'IM1', 'Mission', 'Non', '2026-09-21 00:00:00', '2026-09-21 00:00:00'),
    (1, 'IM1', 'Mission', 'Oui', '2026-09-27 23:59:59', '2026-09-27 23:59:59'),
    (1, 'IM1', 'Congé', 'Oui', '2026-09-20 12:00:00', '2026-09-20 12:00:00')");

$pdo->exec("INSERT INTO comportement_personnel (personnel_id, im, type, date_comportement, motif, created_at, updated_at) VALUES
    (1, 'IM1', 'Positive', '2026-09-22', 'Motif', '2026-09-22 10:00:00', '2026-09-22 10:00:00')");

$pdo->exec("INSERT INTO declaration_perte (date_declaration, heure_declaration, identite_declarant, nature_objet, description_objet, date_perte, lieu_perte, numero_attestation, nom_agent, created_at, updated_at) VALUES
    ('2026-09-01', '08:00', 'D1', 'CIN', 'Carte', '2026-09-01', 'Marché', 'ATT-1', 'Agent X', '2026-09-01 00:00:00', '2026-09-01 00:00:00'),
    ('2026-09-30', '08:00', 'D2', 'CIN', 'Carte', '2026-09-30', 'Marché', 'ATT-2', 'Agent X', '2026-09-30 23:59:59', '2026-09-30 23:59:59'),
    ('2026-08-31', '08:00', 'D3', 'CIN', 'Carte', '2026-08-31', 'Marché', 'ATT-3', 'Agent X', '2026-08-31 12:00:00', '2026-08-31 12:00:00'),
    ('2026-10-01', '08:00', 'D4', 'CIN', 'Carte', '2026-10-01', 'Marché', 'ATT-4', 'Agent X', '2026-10-01 12:00:00', '2026-10-01 12:00:00')");

$pdo->exec("INSERT INTO main_courante (date_evenement, heure_evenement, categorie, description, origine, created_at, updated_at) VALUES
    ('2026-09-23', '11:00', 'Incident au poste', 'Test', 'Secretariat', '2026-09-23 11:00:00', '2026-09-23 11:00:00'),
    ('2026-09-23', '11:30', 'Incident au poste', 'Test', 'Poste', '2026-09-23 11:30:00', '2026-09-23 11:30:00')");

$pdo->exec("INSERT INTO passation (date_passation, heure_passation, created_at, updated_at) VALUES
    ('2026-09-23', '12:00', '2026-09-23 12:00:00', '2026-09-23 12:00:00')");

$pdo->exec("INSERT INTO armement (date_perception, heure_perception, type_arme, matricule_arme, created_at, updated_at) VALUES
    ('2026-09-23', '13:00', 'PA', 'A1', '2026-09-23 13:00:00', '2026-09-23 13:00:00')");

$pdo->exec("INSERT INTO arme (type_arme_id, matricule, created_at, updated_at) VALUES
    ($typeArmeId, 'MAT-1', '2026-09-10 10:00:00', '2026-09-10 10:00:00')");

$pdo->exec("INSERT INTO affectation_materiel (agent_personnel_id, date_perception, heure_perception, created_at, updated_at) VALUES
    (1, '2026-09-15', '10:00', '2026-09-15 10:00:00', '2026-09-15 10:00:00')");

$pdo->exec("INSERT INTO materiel_roulant (date_perception, heure_perception, type_materiel, statut, created_at, updated_at) VALUES
    ('2026-09-05', '10:00', 'VHL', 'En service', '2026-09-05 10:00:00', '2026-09-05 10:00:00')");

$pdo->exec("INSERT INTO garde_a_vue (nom, created_at, updated_at) VALUES
    ('X', '2026-09-23 14:00:00', '2026-09-23 14:00:00')");

// --- Helpers -------------------------------------------------------------------
function section(array $report, string $groupKey, string $sectionKey): ?array
{
    foreach ($report['groups'] as $group) {
        if ($group['key'] !== $groupKey) continue;
        foreach ($group['sections'] as $section) {
            if ($section['key'] === $sectionKey) return $section;
        }
    }
    return null;
}

// === Daily report (2026-09-23) =================================================
$daily = RapportStats::generate('daily', '2026-09-23');

check($daily['type'] === 'daily', 'daily: type = daily');
check($daily['type_label'] === 'Journalier', 'daily: type_label = Journalier');
check($daily['period_start'] === '2026-09-23' && $daily['period_end'] === '2026-09-23', 'daily: period = 2026-09-23');

$s = section($daily, 'secretariat', 'correspondance');
check($s['created'] === 1 && $s['updated'] === 1 && $s['total'] === 2, 'daily: correspondance created=1 updated=1 total=2');

$s = section($daily, 'secretariat', 'personnel');
check($s['created'] === 1 && $s['updated'] === 0 && $s['total'] === 1, 'daily: personnel created=1');

check(section($daily, 'secretariat', 'mouvement_personnel')['total'] === 0, 'daily: mouvement created=0');
check(section($daily, 'secretariat', 'declaration_perte')['total'] === 0, 'daily: declaration created=0');
check(section($daily, 'secretariat', 'main_courante_secretariat')['created'] === 1, 'daily: main courante secrétariat created=1');
check(section($daily, 'poste', 'main_courante_poste')['created'] === 1, 'daily: main courante poste created=1');
check(section($daily, 'poste', 'passation')['created'] === 1, 'daily: passation created=1');
check(section($daily, 'poste', 'armement')['created'] === 1, 'daily: armement created=1');
check(section($daily, 'poste', 'arme')['created'] === 0, 'daily: arme created=0');
check(section($daily, 'poste', 'affectation_materiel')['created'] === 0, 'daily: affectation matériel created=0');
check(section($daily, 'poste', 'materiel_roulant')['created'] === 0, 'daily: matériel roulant created=0');
check(section($daily, 'poste', 'situation_gav')['created'] === 1, 'daily: gav created=1');

check($daily['totals']['created'] === 7 && $daily['totals']['updated'] === 1 && $daily['totals']['total'] === 8, 'daily: totals 7/1/8');
check(!empty($daily['generated_at']), 'daily: generated_at présent');

// === Weekly report (week of 2026-09-23 → Mon 21 to Sun 27) ======================
$weekly = RapportStats::generate('weekly', '2026-09-23');

check($weekly['type_label'] === 'Hebdomadaire', 'weekly: type_label = Hebdomadaire');
check($weekly['period_start'] === '2026-09-21' && $weekly['period_end'] === '2026-09-27', 'weekly: period = 21..27 sept');

$s = section($weekly, 'secretariat', 'correspondance');
check($s['created'] === 2 && $s['updated'] === 0 && $s['total'] === 2, 'weekly: correspondance created=2 (Sun 20 excluded, created-in-period not double-counted)');
check(section($weekly, 'secretariat', 'mouvement_personnel')['created'] === 2, 'weekly: mouvement created=2 (Mon+Sun boundaries, prev Sun excluded)');
check(section($weekly, 'secretariat', 'personnel')['created'] === 1 && section($weekly, 'secretariat', 'personnel')['updated'] === 1, 'weekly: personnel created=1 updated=1 (modified pre-existing record)');
check(section($weekly, 'secretariat', 'declaration_perte')['created'] === 0, 'weekly: declaration created=0');
check($weekly['totals']['created'] === 11 && $weekly['totals']['updated'] === 1 && $weekly['totals']['total'] === 12, 'weekly: totals 11/1/12');

// A Sunday anchor must resolve to the same ISO week (Monday start).
$weeklySunday = RapportStats::generate('weekly', '2026-09-27');
check($weeklySunday['period_start'] === '2026-09-21' && $weeklySunday['period_end'] === '2026-09-27', 'weekly: Sunday anchor resolves to same ISO week');

// === Monthly report (September 2026) ===========================================
$monthly = RapportStats::generate('monthly', '2026-09-23');

check($monthly['type_label'] === 'Mensuel', 'monthly: type_label = Mensuel');
check($monthly['period_start'] === '2026-09-01' && $monthly['period_end'] === '2026-09-30', 'monthly: period = September 2026');

$s = section($monthly, 'secretariat', 'declaration_perte');
check($s['created'] === 2 && $s['total'] === 2, 'monthly: declaration created=2 (Aug 31 / Oct 1 excluded)');
check(section($monthly, 'secretariat', 'correspondance')['created'] === 3, 'monthly: correspondance created=3');
check(section($monthly, 'secretariat', 'mouvement_personnel')['created'] === 3, 'monthly: mouvement created=3');
check(section($monthly, 'secretariat', 'personnel')['created'] === 3, 'monthly: personnel created=3');
check(section($monthly, 'poste', 'arme')['created'] === 1, 'monthly: arme created=1');
check(section($monthly, 'poste', 'affectation_materiel')['created'] === 1, 'monthly: affectation matériel created=1');
check(section($monthly, 'poste', 'materiel_roulant')['created'] === 1, 'monthly: matériel roulant created=1');
check($monthly['totals']['created'] === 20 && $monthly['totals']['total'] === 20, 'monthly: totals 20/0/20');

// === Empty period ==============================================================
$empty = RapportStats::generate('daily', '2030-01-15');
check($empty['totals']['total'] === 0, 'empty period: total = 0');
check($empty['totals']['created'] === 0 && $empty['totals']['updated'] === 0, 'empty period: created/updated = 0');
check(count($empty['groups']) === 2, 'empty period: groups still present (secrétariat + poste)');
check(section($empty, 'secretariat', 'correspondance')['total'] === 0, 'empty period: section present with 0');

// --- Cleanup ------------------------------------------------------------------
$pdo->exec("DROP DATABASE `$scratch`");

if ($failures > 0) {
    echo "\n$failures test(s) FAILED\n";
    exit(1);
}
echo "\nAll tests passed\n";
