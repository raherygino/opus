<?php

/**
 * Main courante model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/MainCouranteTest.php
 *
 * Creates a scratch database (opus_test_main_courante), applies migration
 * database/042_create_main_courante.sql, exercises the MainCourante and
 * MainCouranteAttachment models plus the controller validation rules,
 * then drops the scratch database. Never touches the main `opus` database.
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
$scratch = 'opus_test_main_courante';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FK to users and the model's joins
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/042_create_main_courante.sql');
foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
    if (preg_match('/^\s*(CREATE|ALTER)/i', preg_replace('/^--.*$/m', '', $stmt))) {
        $pdo->exec($stmt);
    }
}

// Apply the categorie catalog migration and seed the three defaults.
$sqlCat = file_get_contents($root . '/database/043_create_main_courante_categorie.sql');
foreach (array_filter(array_map('trim', explode(';', $sqlCat))) as $stmt) {
    if (preg_match('/^\s*(CREATE|INSERT)/i', preg_replace('/^--.*$/m', '', $stmt))) {
        $pdo->exec($stmt);
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\MainCourante;
use App\Models\MainCouranteAttachment;

function sampleRow(array $overrides = []): array
{
    return array_merge([
        'date_evenement' => '2026-09-13',
        'heure_evenement' => '14:30',
        'categorie' => 'Incident au poste',
        'description' => 'Tiers venu déposer une plainte verbale',
        'origine' => 'Secretariat',
        'created_by' => null,
    ], $overrides);
}

// --- Model: create / read ----------------------------------------------------
echo "Model CRUD\n";
$id = MainCourante::create(sampleRow());
$row = MainCourante::getById($id);
check($row !== null && $row['categorie'] === 'Incident au poste', 'create + getById');
check($row['origine'] === 'Secretariat', 'origine persisted (Secretariat)');

$idPoste = MainCourante::create(sampleRow([
    'origine' => 'Poste',
    'categorie' => 'Renseignement reçu',
    'description' => 'Renseignement reçu de la part du commissaire',
]));
check($idPoste > 0 && $idPoste !== $id, 'create with Poste origine');

// --- Model: update / filters / delete ----------------------------------------
check(MainCourante::update($id, ['description' => 'Description modifiée']), 'update description');
check(MainCourante::getById($id)['description'] === 'Description modifiée', 'description persisted');
check(count(MainCourante::getAll(['origine' => 'Secretariat'])) === 1, 'filter by origine Secretariat');
check(count(MainCourante::getAll(['origine' => 'Poste'])) === 1, 'filter by origine Poste');
check(count(MainCourante::getAll(['categorie' => 'Incident au poste'])) === 1, 'filter by categorie');
check(count(MainCourante::getAll(['search' => 'modifiée'])) === 1, 'search filter');
check(count(MainCourante::getAll(['search' => 'no-such-thing'])) === 0, 'search no match');
check(count(MainCourante::getAll(['date_from' => '2026-09-13'])) === 2, 'date_from filter');
check(count(MainCourante::getAll(['date_from' => '2026-09-14'])) === 0, 'date_from no match');

// --- Attachments ---------------------------------------------------------------
echo "Attachments\n";
$attId = MainCouranteAttachment::create([
    'main_courante_id' => $id,
    'title' => 'Procès-verbal',
    'filename' => 'pv_abc123.pdf',
    'original_filename' => 'pv.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 5678,
]);
check(MainCouranteAttachment::belongsToMainCourante($attId, $id), 'belongsToMainCourante true');
check(!MainCouranteAttachment::belongsToMainCourante($attId, $idPoste), 'belongsToMainCourante false for other record');
check(count(MainCouranteAttachment::getByMainCouranteId($id)) === 1, 'getByMainCouranteId');

MainCourante::delete($id);
check(MainCourante::getById($id) === null, 'delete main courante');
check(count(MainCouranteAttachment::getByMainCouranteId($id)) === 0, 'attachments cascade-deleted');

// --- Controller validation rules (private static, via reflection) --------------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\MainCouranteController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true): array =>
    $method->invoke(null, $data, $isCreate);

check($validate(sampleRow()) === [], 'valid payload passes');
check(isset($validate(sampleRow(['categorie' => 'X']))['categorie']), 'invalid categorie rejected');
check(isset($validate(sampleRow(['categorie' => '']))['categorie']), 'missing categorie rejected');
check(isset($validate(sampleRow(['heure_evenement' => '25:00']))['heure_evenement']), 'invalid heure rejected');
check(isset($validate(sampleRow(['heure_evenement' => '']))['heure_evenement']), 'missing heure rejected');
check(isset($validate(sampleRow(['date_evenement' => '13/09/2026']))['date_evenement']), 'invalid date format rejected');
check(isset($validate(sampleRow(['date_evenement' => '']))['date_evenement']), 'missing date rejected');
check(isset($validate(sampleRow(['description' => '']))['description']), 'blank description rejected');
check(isset($validate(sampleRow(['description' => '   ']))['description']), 'whitespace-only description rejected');
check(isset($validate(sampleRow(['origine' => 'Nope']))['origine']), 'invalid origine rejected');
check($validate(sampleRow(['origine' => 'Poste'])) === [], 'Poste origine accepted');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
