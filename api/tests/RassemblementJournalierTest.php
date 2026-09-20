<?php

/**
 * RassemblementJournalier model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/RassemblementJournalierTest.php
 *
 * Creates a scratch database (opus_test_rassemblement), applies migrations
 * database/056_create_rassemblement_journalier.sql then
 * database/057_merge_situation_prise_arme_into_rassemblement.sql (exercising
 * the migration path), exercises the RassemblementJournalier model plus the
 * controller validation rules, then drops the scratch database. Never touches
 * the main `opus` database.
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
$scratch = 'opus_test_rassemblement';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FKs and the model's joins
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

foreach (['056_create_rassemblement_journalier.sql', '057_merge_situation_prise_arme_into_rassemblement.sql'] as $file) {
    $sql = file_get_contents($root . '/database/' . $file);
    foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
        if (preg_match('/^\s*(CREATE|ALTER|UPDATE|INSERT|DROP)/i', preg_replace('/^--.*$/m', '', $stmt))) {
            $pdo->exec($stmt);
        }
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\RassemblementJournalier;

function sampleRow(array $overrides = []): array
{
    return array_merge([
        'date_rassemblement' => '2026-09-20',
        'heure_rassemblement' => '07:30',
        'brigade_service' => 'Brigade A',
        'officier_permanence' => 'Cdt Rakoto',
        'inspecteur_permanence' => 'Insp Rabe',
        'chef_poste' => 'Adj Ranaivo',
        'instructions_autorite' => 'Vigilance renforcée',
        'effectif_theorique' => 40,
        'present' => 38,
        'absent' => 2,
        'motif_absence' => 'Congé | Mission',
        'created_by' => null,
    ], $overrides);
}

// --- Migration structure ------------------------------------------------------
echo "Migration\n";
$tables = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
check(!in_array('situation_prise_arme', $tables, true), 'situation_prise_arme table dropped');
check(!in_array('repartition_secteur_diurne', $tables, true), 'repartition_secteur_diurne table dropped');
check(!in_array('repartition_secteur_nocturne', $tables, true), 'repartition_secteur_nocturne table dropped');
check(in_array('repartition_secteur', $tables, true), 'repartition_secteur table created');
$columns = $pdo->query('SHOW COLUMNS FROM rassemblement_journalier')->fetchAll(PDO::FETCH_COLUMN);
foreach (['effectif_theorique', 'present', 'absent', 'motif_absence'] as $col) {
    check(in_array($col, $columns, true), "rassemblement_journalier.$col column added");
}

// --- Model: create / read ------------------------------------------------------
echo "Model CRUD\n";
$id = RassemblementJournalier::create(sampleRow());
$row = RassemblementJournalier::find($id);
check($row !== null && $row['brigade_service'] === 'Brigade A', 'create + find');
check((int) $row['effectif_theorique'] === 40 && (int) $row['present'] === 38 && (int) $row['absent'] === 2, 'situation de prise d\'arme persisted on the main record');
check($row['motif_absence'] === 'Congé | Mission', 'motif_absence persisted');

$idDefaults = RassemblementJournalier::create(sampleRow(['effectif_theorique' => null, 'present' => null, 'absent' => null, 'motif_absence' => null]));
$rowDefaults = RassemblementJournalier::find($idDefaults);
check((int) $rowDefaults['effectif_theorique'] === 0 && (int) $rowDefaults['present'] === 0 && (int) $rowDefaults['absent'] === 0, 'situation defaults to zeros when omitted');

check(RassemblementJournalier::update($id, sampleRow(['present' => 39, 'absent' => 1, 'motif_absence' => 'Mission'])), 'update');
$row = RassemblementJournalier::find($id);
check((int) $row['present'] === 39 && $row['motif_absence'] === 'Mission', 'situation fields updated');

check(count(RassemblementJournalier::all(['search' => 'Rakoto'])) === 2, 'search filter');

// --- Repartitions (Diurne / Nocturne) ------------------------------------------
echo "Repartitions\n";
RassemblementJournalier::replaceRepartitions($id, [
    ['type' => 'diurne', 'secteur' => 'Secteur 1', 'effectif_engage' => '10', 'chef_element_contact' => 'Adj Ranaivo', 'controle_contact' => 'Brig Rabe', 'materiels_armements' => '1 FM24', 'missions' => 'Patrouille'],
    ['type' => 'diurne', 'secteur' => 'Secteur 2'],
    ['type' => 'nocturne', 'secteur' => 'Secteur 3', 'effectif_engage' => '6'],
    ['type' => 'invalide', 'secteur' => 'Ignoré'],
]);
$rows = RassemblementJournalier::repartitions($id);
check(count($rows) === 3, 'invalid repartition type rows are skipped');
check(count(RassemblementJournalier::repartitions($id, 'diurne')) === 2, 'filter by type diurne');
check(count(RassemblementJournalier::repartitions($id, 'nocturne')) === 1, 'filter by type nocturne');
check($rows[0]['type'] === 'diurne' && $rows[0]['secteur'] === 'Secteur 1', 'repartition row persisted');
check($rows[2]['type'] === 'nocturne', 'type differentiated on shared structure');

RassemblementJournalier::replaceRepartitions($id, [['type' => 'nocturne', 'secteur' => 'Secteur 9']]);
check(count(RassemblementJournalier::repartitions($id)) === 1, 'replace clears previous rows');

RassemblementJournalier::delete($id);
check(RassemblementJournalier::find($id) === null, 'delete rassemblement');
check(count(RassemblementJournalier::repartitions($id)) === 0, 'repartitions cascade-deleted');

// --- Controller validation rules (private static, via reflection) --------------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\RassemblementJournalierController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true): array => $method->invoke(null, $data, $isCreate);

check($validate(sampleRow()) === [], 'valid payload passes');
check(isset($validate(sampleRow(['date_rassemblement' => '']))['date_rassemblement']), 'missing date rejected');
check(isset($validate(sampleRow(['heure_rassemblement' => '']))['heure_rassemblement']), 'missing heure rejected');
check(isset($validate(sampleRow(['brigade_service' => ' ']))['brigade_service']), 'blank brigade rejected');
check(isset($validate(sampleRow(['effectif_theorique' => -1]))['effectif_theorique']), 'negative effectif_theorique rejected');
check(isset($validate(sampleRow(['present' => 'abc']))['present']), 'non-numeric present rejected');
check($validate(sampleRow(['effectif_theorique' => null])) === [], 'omitted situation fields pass on create');
check(isset($validate(['repartitions' => [['type' => 'matin']]], false)['repartitions.0.type']), 'invalid repartition type rejected');
check($validate(['repartitions' => [['type' => 'diurne'], ['type' => 'nocturne']]], false) === [], 'diurne/nocturne repartitions accepted');
check($validate([], false) === [], 'partial update payload passes');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
