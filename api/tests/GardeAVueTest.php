<?php

/**
 * Garde à Vue (Police Judiciaire) model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/GardeAVueTest.php
 *
 * Creates a scratch database (opus_test_garde_a_vue), applies migration
 * database/048_create_garde_a_vue.sql, exercises the GardeAVue and
 * GardeAVueAttachment models plus the controller validation rules
 * (including the GAV datetime logical consistency checks), then drops
 * the scratch database. Never touches the main `opus` database.
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
$scratch = 'opus_test_garde_a_vue';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FKs to users and personnel (for model joins).
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(50) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL, grade VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/048_create_garde_a_vue.sql');
$lines = explode("\n", $sql);
$codeLines = array_filter($lines, fn($l) => !preg_match('/^\s*--/', $l));
$cleanSql = implode("\n", $codeLines);
foreach (array_filter(array_map('trim', explode(';', $cleanSql))) as $stmt) {
    if ($stmt !== '') {
        $pdo->exec($stmt);
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\GardeAVue;
use App\Models\GardeAVueAttachment;
use App\Controllers\GardeAVueController;

// --- Helpers ----------------------------------------------------------------
function sampleGav(array $overrides = []): array
{
    return array_merge([
        'nom' => 'Rakoto Jean',
        'prenoms' => 'Jean Bruno',
        'date_naissance' => '1990-01-15',
        'adresse' => 'Lot 123 Antananarivo',
        'enqueteur_permance' => 'I. Sow',
        'opj_gav' => 'M. Kane',
        'motif' => 'Vol aggravé',
        'etat_sante' => 'Bon',
        'droits_notifies' => 'Droits notifiés à 14h00',
        'personne_contacter' => 'Rabe Pierre',
        'debut_gav' => '2026-09-15 08:00:00',
        'fin_gav' => '2026-09-15 20:00:00',
        'prolongation_gav' => null,
        'created_by' => null,
    ], $overrides);
}

// --- GardeAVue: create / read -----------------------------------------------
echo "GardeAVue CRUD\n";
$data = sampleGav();
$id = GardeAVue::create($data);
$row = GardeAVue::getById($id);
check($row !== null && $row['nom'] === 'Rakoto Jean', 'create + getById');
check($row['prenoms'] === 'Jean Bruno', 'prenoms persisted');
check($row['debut_gav'] === '2026-09-15 08:00:00', 'debut_gav persisted');
check($row['fin_gav'] === '2026-09-15 20:00:00', 'fin_gav persisted');
check($row['prolongation_gav'] === null, 'prolongation_gav null persisted');

// --- Update -----------------------------------------------------------------
GardeAVue::update($id, ['nom' => 'Rakoto Jean Updated', 'adresse' => 'Lot 456']);
$updated = GardeAVue::getById($id);
check($updated['nom'] === 'Rakoto Jean Updated', 'update nom');
check($updated['adresse'] === 'Lot 456', 'update adresse');
check($updated['debut_gav'] === '2026-09-15 08:00:00', 'debut_gav unchanged on update');

// --- getAll with filters ----------------------------------------------------
$all = GardeAVue::getAll();
check(count($all) >= 1, 'getAll returns records');

$searchResults = GardeAVue::getAll(['search' => 'Rakoto']);
check(count($searchResults) >= 1, 'search by nom works');

$searchNoResults = GardeAVue::getAll(['search' => 'NonExistentPerson']);
check(count($searchNoResults) === 0, 'search with no match returns empty');

// --- Attachments ------------------------------------------------------------
echo "Attachments\n";
$attId = GardeAVueAttachment::create([
    'garde_a_vue_id' => $id,
    'title' => 'Procès-verbal',
    'filename' => 'proces_verbal_001.txt',
    'original_filename' => 'PV.txt',
    'mime_type' => 'text/plain',
    'file_size' => 1024,
]);
$atts = GardeAVueAttachment::getByGardeAVueId($id);
check(count($atts) === 1 && $atts[0]['title'] === 'Procès-verbal', 'attachment created + retrieved');
check(GardeAVueAttachment::belongsToGardeAVue($attId, $id) === true, 'belongsToGardeAVue true');
check(GardeAVueAttachment::belongsToGardeAVue($attId, $id + 999) === false, 'belongsToGardeAVue false for wrong owner');

GardeAVueAttachment::update($attId, ['title' => 'PV modifié']);
$att = GardeAVueAttachment::getById($attId);
check($att['title'] === 'PV modifié', 'attachment update');

GardeAVueAttachment::delete($attId);
check(GardeAVueAttachment::getById($attId) === null, 'attachment delete');
check(count(GardeAVueAttachment::getByGardeAVueId($id)) === 0, 'attachments empty after delete');

// --- Delete -----------------------------------------------------------------
GardeAVue::delete($id);
check(GardeAVue::getById($id) === null, 'delete removes the record');

// --- Validation -------------------------------------------------------------
echo "Validation\n";
$validate = function (array $data, bool $isCreate = true): array {
    $method = new ReflectionMethod(GardeAVueController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate);
};

check($validate(sampleGav()) === [], 'valid payload passes');

check(isset($validate(sampleGav(['nom' => '']))['nom']), 'missing nom rejected');

check(isset($validate(sampleGav(['date_naissance' => '13/09/2026']))['date_naissance']), 'invalid date_naissance format rejected');
check($validate(sampleGav(['date_naissance' => ''])) === [], 'empty date_naissance accepted (optional)');

// datetime logical consistency
check(isset($validate(sampleGav([
    'debut_gav' => '2026-09-15 20:00:00',
    'fin_gav' => '2026-09-15 08:00:00',
]))['fin_gav']), 'fin_gav before debut_gav rejected');

check($validate(sampleGav([
    'debut_gav' => '2026-09-15 08:00:00',
    'fin_gav' => '2026-09-15 20:00:00',
])) === [], 'fin_gav after debut_gav accepted');

check(isset($validate(sampleGav([
    'fin_gav' => '2026-09-15 20:00:00',
    'prolongation_gav' => '2026-09-15 18:00:00',
]))['prolongation_gav']), 'prolongation before fin_gav rejected');

check($validate(sampleGav([
    'fin_gav' => '2026-09-15 20:00:00',
    'prolongation_gav' => '2026-09-16 08:00:00',
])) === [], 'prolongation after fin_gav accepted');

// datetime-local format (T separator) accepted
check($validate(sampleGav([
    'debut_gav' => '2026-09-15T08:00',
    'fin_gav' => '2026-09-15T20:00',
])) === [], 'datetime-local format (T separator) accepted');

// invalid datetime format
check(isset($validate(sampleGav(['debut_gav' => '15/09/2026 08:00']))['debut_gav']), 'invalid datetime format rejected');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
