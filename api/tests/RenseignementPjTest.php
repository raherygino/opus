<?php

/**
 * Renseignement PJ (Police Judiciaire) model + validation tests.
 *
 * Usage: php api/tests/RenseignementPjTest.php
 *
 * Creates a scratch database (opus_test_renseignement_pj), applies migration
 * database/053_create_renseignement_pj.sql, exercises the RenseignementPj +
 * attachment models plus the controller validation rules, then drops the
 * scratch database. Never touches the main `opus` database.
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
$scratch = 'opus_test_renseignement_pj';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, prenoms VARCHAR(100) NULL, nom VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/053_create_renseignement_pj.sql');
$lines = explode("\n", $sql);
$codeLines = array_filter($lines, fn($l) => !preg_match('/^\s*--/', $l));
$cleanSql = implode("\n", $codeLines);
foreach (array_filter(array_map('trim', explode(';', $cleanSql))) as $stmt) {
    if ($stmt !== '') {
        $pdo->exec($stmt);
    }
}

putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\RenseignementPj;
use App\Models\RenseignementPjAttachment;
use App\Controllers\RenseignementPjController;

// --- Helpers ----------------------------------------------------------------
function sampleRenseignement(array $overrides = []): array
{
    return array_merge([
        'nature_infraction' => 'Vol à main armée',
        'date_lieu_faits' => '12/01/2026, Antananarivo',
        'circonstances' => 'Individu armé repéré sur les lieux',
        'prejudices' => 'Perte matérielle évaluée à 5M Ar',
        'created_by' => null,
    ], $overrides);
}

// --- RenseignementPj: CRUD ---------------------------------------------------
echo "RenseignementPj CRUD\n";
$id = RenseignementPj::create(sampleRenseignement());
$row = RenseignementPj::find($id);
check($row !== null && $row['nature_infraction'] === 'Vol à main armée', 'create + find');
check($row['date_lieu_faits'] === '12/01/2026, Antananarivo', 'date_lieu_faits persisted');
check($row['prejudices'] === 'Perte matérielle évaluée à 5M Ar', 'prejudices persisted');

RenseignementPj::update($id, sampleRenseignement(['circonstances' => 'Individu interpellé']));
$updated = RenseignementPj::find($id);
check($updated['circonstances'] === 'Individu interpellé', 'update circonstances');

RenseignementPj::create(sampleRenseignement(['nature_infraction' => 'Coups et blessures']));
$all = RenseignementPj::all();
check(count($all) >= 2, 'all returns at least 2 records');

$search = RenseignementPj::all(['search' => 'blessures']);
check(count($search) >= 1, 'search by nature_infraction works');

RenseignementPj::delete($id);
check(RenseignementPj::find($id) === null, 'delete removes the record');

// --- Validation --------------------------------------------------------------
echo "RenseignementPj Validation\n";
$validate = function (array $data, bool $isCreate = true): array {
    $method = new ReflectionMethod(RenseignementPjController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate);
};

check($validate(sampleRenseignement()) === [], 'valid payload passes');
check(isset($validate(sampleRenseignement(['nature_infraction' => '']))['nature_infraction']), 'missing nature_infraction rejected');
check(isset($validate(sampleRenseignement(['nature_infraction' => '   ']))['nature_infraction']), 'blank nature_infraction rejected');
check($validate(sampleRenseignement(['date_lieu_faits' => null])) === [], 'null date_lieu_faits passes');
check($validate(sampleRenseignement(['circonstances' => null, 'prejudices' => null])) === [], 'null circonstances/prejudices pass');

// --- Attachments -------------------------------------------------------------
echo "RenseignementPj Attachments\n";
$rensId = RenseignementPj::create(sampleRenseignement(['nature_infraction' => 'Rapt']));
$attId = RenseignementPjAttachment::create([
    'renseignement_id' => $rensId,
    'title' => 'Déposition témoin',
    'filename' => 'deposition.pdf',
    'original_filename' => 'deposition.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 2048,
]);
check(RenseignementPjAttachment::find($attId) !== null, 'attachment create + find');
check(RenseignementPjAttachment::belongsToRenseignement($attId, $rensId), 'belongsToRenseignement true');
check(!RenseignementPjAttachment::belongsToRenseignement($attId, 999999), 'belongsToRenseignement false for other');
check(count(RenseignementPjAttachment::listForRenseignement($rensId)) === 1, 'listForRenseignement returns 1');
RenseignementPjAttachment::updateTitle($attId, 'Déposition updated');
check(RenseignementPjAttachment::find($attId)['title'] === 'Déposition updated', 'updateTitle');
RenseignementPjAttachment::delete($attId);
check(RenseignementPjAttachment::find($attId) === null, 'delete attachment');

// --- Teardown ----------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
