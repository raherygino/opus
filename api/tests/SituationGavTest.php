<?php

/**
 * Situation GAV (Sédentaire > Poste) model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/SituationGavTest.php
 *
 * Creates a scratch database (opus_test_situation_gav), applies migration
 * database/068_create_situation_gav.sql plus the prerequisite tables,
 * exercises the SituationGav and SituationGavAttachment models plus the
 * controller validation rules, then drops the scratch database.
 * Never touches the main `opus` database.
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
$scratch = 'opus_test_situation_gav';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FKs (users, personnel, garde_a_vue).
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(50) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL, grade VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
// Stub required by Personnel::getById (status subquery).
$pdo->exec('CREATE TABLE mouvement_personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, personnel_id INT UNSIGNED NULL, type_mouvement VARCHAR(100) NULL, retour VARCHAR(10) NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/048_create_garde_a_vue.sql');
$lines = explode("\n", $sql);
$codeLines = array_filter($lines, fn($l) => !preg_match('/^\s*--/', $l));
$cleanSql = implode("\n", $codeLines);
foreach (array_filter(array_map('trim', explode(';', $cleanSql))) as $stmt) {
    if ($stmt !== '') {
        $pdo->exec($stmt);
    }
}

$sql = file_get_contents($root . '/database/068_create_situation_gav.sql');
$lines = explode("\n", $sql);
$codeLines = array_filter($lines, fn($l) => !preg_match('/^\s*--/', $l));
$cleanSql = implode("\n", $codeLines);
foreach (array_filter(array_map('trim', explode(';', $cleanSql))) as $stmt) {
    if ($stmt !== '') {
        $pdo->exec($stmt);
    }
}

// Seed a personnel row and a GAV row for FK references.
$pdo->exec("INSERT INTO personnel (im, firstname, lastname, grade) VALUES ('IM001', 'Jean', 'Rabe', 'Lieutenant')");
$agentId = (int) $pdo->lastInsertId();
$pdo->exec("INSERT INTO garde_a_vue (nom, prenoms, debut_gav) VALUES ('Rakoto Jean', 'Jean Bruno', '2026-09-15 08:00:00')");
$gavId = (int) $pdo->lastInsertId();

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\SituationGav;
use App\Models\SituationGavAttachment;
use App\Controllers\SituationGavController;

// --- Helpers ----------------------------------------------------------------
function sampleSituation(array $overrides = []): array
{
    global $gavId, $agentId;
    return array_merge([
        'garde_a_vue_id' => $gavId,
        'date_controle' => '2026-09-15 12:30:00',
        'agent_controle_id' => $agentId,
        'etat_general' => 'Calme, bon état général',
        'observations' => 'Rien à signaler',
        'mesures_prises' => 'Aucune mesure particulière',
        'created_by' => null,
    ], $overrides);
}

// --- SituationGav: create / read ---------------------------------------------
echo "SituationGav CRUD\n";
$data = sampleSituation();
$id = SituationGav::create($data);
$row = SituationGav::getById($id);
check($row !== null && (int) $row['garde_a_vue_id'] === $gavId, 'create + getById');
check($row['personne_nom'] === 'Rakoto Jean', 'personne_nom joined from garde_a_vue');
check($row['personne_prenoms'] === 'Jean Bruno', 'personne_prenoms joined from garde_a_vue');
check($row['agent_controle_grade'] === 'Lieutenant', 'agent_controle_grade joined from personnel');
check($row['agent_controle_nom'] === 'Rabe', 'agent_controle_nom joined from personnel');
check($row['date_controle'] === '2026-09-15 12:30:00', 'date_controle persisted');
check($row['etat_general'] === 'Calme, bon état général', 'etat_general persisted');
check($row['observations'] === 'Rien à signaler', 'observations persisted');
check($row['mesures_prises'] === 'Aucune mesure particulière', 'mesures_prises persisted');

// --- Update -------------------------------------------------------------------
SituationGav::update($id, ['etat_general' => 'Agité', 'observations' => 'Personne agitée']);
$updated = SituationGav::getById($id);
check($updated['etat_general'] === 'Agité', 'update etat_general');
check($updated['observations'] === 'Personne agitée', 'update observations');
check($updated['date_controle'] === '2026-09-15 12:30:00', 'date_controle unchanged on update');

// --- getAll with filters -------------------------------------------------------
$all = SituationGav::getAll();
check(count($all) >= 1, 'getAll returns records');

$searchResults = SituationGav::getAll(['search' => 'Rakoto']);
check(count($searchResults) >= 1, 'search by personne nom works');

$searchResults = SituationGav::getAll(['search' => 'Rabe']);
check(count($searchResults) >= 1, 'search by agent lastname works');

$searchNoResults = SituationGav::getAll(['search' => 'NonExistent']);
check(count($searchNoResults) === 0, 'search with no match returns empty');

$byGav = SituationGav::getAll(['garde_a_vue_id' => $gavId]);
check(count($byGav) >= 1, 'filter by garde_a_vue_id works');

$byGavNone = SituationGav::getAll(['garde_a_vue_id' => 9999]);
check(count($byGavNone) === 0, 'filter by unknown garde_a_vue_id returns empty');

$dateFiltered = SituationGav::getAll(['date_from' => '2026-09-15 00:00:00', 'date_to' => '2026-09-15 23:59:59']);
check(count($dateFiltered) >= 1, 'date_from/date_to filter works');

// --- Attachments ---------------------------------------------------------------
echo "Attachments\n";
$attId = SituationGavAttachment::create([
    'situation_gav_id' => $id,
    'title' => 'Photo de contrôle',
    'filename' => 'photo_001.jpg',
    'original_filename' => 'photo.jpg',
    'mime_type' => 'image/jpeg',
    'file_size' => 2048,
]);
$atts = SituationGavAttachment::getBySituationGavId($id);
check(count($atts) === 1 && $atts[0]['title'] === 'Photo de contrôle', 'attachment created + retrieved');
check(SituationGavAttachment::belongsToSituationGav($attId, $id) === true, 'belongsToSituationGav true');
check(SituationGavAttachment::belongsToSituationGav($attId, $id + 999) === false, 'belongsToSituationGav false for wrong owner');

SituationGavAttachment::update($attId, ['title' => 'Photo modifiée']);
$att = SituationGavAttachment::getById($attId);
check($att['title'] === 'Photo modifiée', 'attachment update');

SituationGavAttachment::delete($attId);
check(SituationGavAttachment::getById($attId) === null, 'attachment delete');
check(count(SituationGavAttachment::getBySituationGavId($id)) === 0, 'attachments empty after delete');

// --- Cascade delete --------------------------------------------------------------
$attId2 = SituationGavAttachment::create([
    'situation_gav_id' => $id,
    'title' => 'Photo 2',
    'filename' => 'photo_002.jpg',
]);
SituationGav::delete($id);
check(SituationGav::getById($id) === null, 'delete removes the record');
check(SituationGavAttachment::getById($attId2) === null, 'cascade delete removes attachments');

// --- Validation --------------------------------------------------------------------
echo "Validation\n";
$validate = function (array $data, bool $isCreate = true): array {
    $method = new ReflectionMethod(SituationGavController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate);
};

check($validate(sampleSituation()) === [], 'valid payload passes');

check(isset($validate(sampleSituation(['garde_a_vue_id' => null]))['garde_a_vue_id']), 'missing garde_a_vue_id rejected');
check(isset($validate(sampleSituation(['garde_a_vue_id' => 0]))['garde_a_vue_id']), 'garde_a_vue_id=0 rejected');
check(isset($validate(sampleSituation(['garde_a_vue_id' => 9999]))['garde_a_vue_id']), 'unknown garde_a_vue_id rejected');

check(isset($validate(sampleSituation(['date_controle' => '']))['date_controle']), 'missing date_controle rejected');
check(isset($validate(sampleSituation(['date_controle' => '15/09/2026 12:30']))['date_controle']), 'invalid date_controle format rejected');
check($validate(sampleSituation(['date_controle' => '2026-09-15T12:30'])) === [], 'datetime-local format (T separator) accepted');
check($validate(sampleSituation(['date_controle' => '2026-09-15 12:30'])) === [], 'datetime without seconds accepted');

check(isset($validate(sampleSituation(['agent_controle_id' => 9999]))['agent_controle_id']), 'unknown agent_controle_id rejected');
check($validate(sampleSituation(['agent_controle_id' => null])) === [], 'null agent_controle_id accepted (optional)');
check($validate(sampleSituation(['agent_controle_id' => ''])) === [], 'empty agent_controle_id accepted (optional)');

check($validate(sampleSituation(['observations' => null])) === [], 'null observations accepted (optional)');
check($validate(sampleSituation(['mesures_prises' => null])) === [], 'null mesures_prises accepted (optional)');
check(isset($validate(sampleSituation(['etat_general' => 123]))['etat_general']), 'non-string etat_general rejected');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
