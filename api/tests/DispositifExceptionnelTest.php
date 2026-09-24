<?php

/**
 * DispositifExceptionnel model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/DispositifExceptionnelTest.php
 *
 * Creates a scratch database (opus_test_dispositif), applies migration
 * database/064_create_dispositif_exceptionnel.sql, exercises the
 * DispositifExceptionnel model plus the controller validation rules, then
 * drops the scratch database. Never touches the main `opus` database.
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
$scratch = 'opus_test_dispositif';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FKs and the model's joins
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/064_create_dispositif_exceptionnel.sql');
foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
    if (preg_match('/^\s*(CREATE|ALTER|UPDATE|INSERT|DROP)/i', preg_replace('/^--.*$/m', '', $stmt))) {
        $pdo->exec($stmt);
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\DispositifExceptionnel;

function sampleRow(array $overrides = []): array
{
    return array_merge([
        'nature_evenement' => 'Visite VIP',
        'date_debut' => '2026-10-01',
        'date_fin' => '2026-10-03',
        'created_by' => null,
    ], $overrides);
}

// --- Migration structure ------------------------------------------------------
echo "Migration\n";
$tables = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
check(in_array('dispositif_exceptionnel', $tables, true), 'dispositif_exceptionnel table created');
check(in_array('dispositif_exceptionnel_effectif', $tables, true), 'dispositif_exceptionnel_effectif table created');

// --- Model: create / read ------------------------------------------------------
echo "Model CRUD\n";
$id = DispositifExceptionnel::create(sampleRow());
$row = DispositifExceptionnel::find($id);
check($row !== null && $row['nature_evenement'] === 'Visite VIP', 'create + find');
check($row['date_debut'] === '2026-10-01' && $row['date_fin'] === '2026-10-03', 'période persisted');

check(DispositifExceptionnel::update($id, sampleRow(['nature_evenement' => 'Match de football', 'date_fin' => '2026-10-05'])), 'update');
$row = DispositifExceptionnel::find($id);
check($row['nature_evenement'] === 'Match de football' && $row['date_fin'] === '2026-10-05', 'fields updated');

check(count(DispositifExceptionnel::all(['search' => 'football'])) === 1, 'search filter');
check(count(DispositifExceptionnel::all(['search' => 'VIP'])) === 0, 'search filter miss');

// --- Effectifs (Effectif engagé rows) -----------------------------------------
echo "Effectifs\n";
DispositifExceptionnel::replaceEffectifs($id, [
    ['secteur' => 'Secteur A', 'chef_element_contact' => 'Adj Ranaivo 034...', 'controle_contact' => 'Brig Rabe', 'materiels_armements' => '2 VHL, 4 PM', 'missions' => 'Filtration accès VIP'],
    ['secteur' => 'Secteur B'],
]);
$rows = DispositifExceptionnel::effectifs($id);
check(count($rows) === 2, 'effectif rows persisted');
check($rows[0]['secteur'] === 'Secteur A' && $rows[0]['missions'] === 'Filtration accès VIP', 'effectif row content persisted');
check($rows[1]['chef_element_contact'] === null, 'optional effectif fields default to null');

DispositifExceptionnel::replaceEffectifs($id, [['secteur' => 'Secteur C']]);
check(count(DispositifExceptionnel::effectifs($id)) === 1, 'replace clears previous rows');

DispositifExceptionnel::delete($id);
check(DispositifExceptionnel::find($id) === null, 'delete dispositif');
check(count(DispositifExceptionnel::effectifs($id)) === 0, 'effectifs cascade-deleted');

// --- Controller validation rules (private static, via reflection) --------------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\DispositifExceptionnelController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true): array => $method->invoke(null, $data, $isCreate);

check($validate(sampleRow()) === [], 'valid payload passes');
check(isset($validate(sampleRow(['nature_evenement' => '']))['nature_evenement']), 'missing nature rejected');
check(isset($validate(sampleRow(['date_debut' => '']))['date_debut']), 'missing date_debut rejected');
check(isset($validate(sampleRow(['date_fin' => '']))['date_fin']), 'missing date_fin rejected');
check(isset($validate(sampleRow(['date_debut' => 'not-a-date']))['date_debut']), 'invalid date_debut rejected');
check(isset($validate(sampleRow(['date_fin' => '2026-09-30']))['date_fin']), 'date_fin before date_debut rejected');
check($validate(sampleRow(['date_debut' => '2026-10-03', 'date_fin' => '2026-10-03'])) === [], 'same-day période accepted');
check(isset($validate(sampleRow(['effectifs' => [['secteur' => '  ']]]))['effectifs.0.secteur']), 'effectif row without secteur rejected');
check($validate(sampleRow(['effectifs' => [['secteur' => 'Secteur A', 'missions' => 'Escorte']]])) === [], 'effectif row with secteur accepted');
check(isset($validate(sampleRow(['effectifs' => [['secteur' => 'Secteur A', 'missions' => 42]]]))['effectifs.0.missions']), 'non-string effectif field rejected');
check($validate([], false) === [], 'empty partial update payload passes');
check(isset($validate(['date_debut' => ''], false)['date_debut']), 'blank date_debut rejected on update');
// Range check uses the merged payload on update (existing row supplies date_fin).
check(isset($validate(['date_debut' => '2026-10-10', 'date_fin' => '2026-10-01'], false)['date_fin']), 'inverted range rejected on update');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
