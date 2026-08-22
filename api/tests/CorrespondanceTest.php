<?php

/**
 * Correspondance model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/CorrespondanceTest.php
 *
 * Creates a scratch database (opus_test_correspondance), applies migration
 * database/019_create_correspondance.sql, exercises the Correspondance and
 * CorrespondanceAttachment models plus the controller validation rules,
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
$scratch = 'opus_test_correspondance';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FK to users and the model's joins
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/019_create_correspondance.sql');
foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
    if (preg_match('/^\s*(CREATE|ALTER)/i', preg_replace('/^--.*$/m', '', $stmt))) {
        $pdo->exec($stmt);
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\Correspondance;
use App\Models\CorrespondanceAttachment;

function sampleRow(array $overrides = []): array
{
    return array_merge([
        'date_correspondance' => '2026-08-22',
        'heure_enregistrement' => '09:30',
        'sens' => 'Entrant',
        'reference' => 'ENT-0001',
        'emetteur_destinataire' => 'Ministère de l\'Intérieur',
        'objet' => 'Demande de renseignements',
        'statut' => 'Enregistré',
        'created_by' => null,
    ], $overrides);
}

// --- Model: create / read ----------------------------------------------------
echo "Model CRUD\n";
$id = Correspondance::create(sampleRow());
$row = Correspondance::getById($id);
check($row !== null && $row['reference'] === 'ENT-0001', 'create + getById');
check($row['sens'] === 'Entrant', 'sens persisted (Entrant)');

$idSortant = Correspondance::create(sampleRow([
    'sens' => 'Sortant',
    'reference' => 'ENT-0001', // same number allowed: registries are per-sens
    'emetteur_destinataire' => 'Préfecture',
]));
check($idSortant > 0 && $idSortant !== $id, 'same reference allowed across sens');

$dupRejected = false;
try {
    Correspondance::create(sampleRow()); // duplicate Entrant/ENT-0001
} catch (PDOException $e) {
    $dupRejected = true;
}
check($dupRejected, 'duplicate (sens, reference) rejected by unique key');

// --- Model: update / filters / delete ----------------------------------------
check(Correspondance::update($id, ['statut' => 'Traité']), 'update statut');
check(Correspondance::getById($id)['statut'] === 'Traité', 'statut persisted');
check(count(Correspondance::getAll(['sens' => 'Entrant'])) === 1, 'filter by sens');
check(count(Correspondance::getAll(['statut' => 'Traité'])) === 1, 'filter by statut');
check(count(Correspondance::getAll(['search' => 'Ministère'])) === 1, 'search filter');
check(count(Correspondance::getAll(['search' => 'no-such-thing'])) === 0, 'search no match');

// --- Attachments ---------------------------------------------------------------
echo "Attachments\n";
$attId = CorrespondanceAttachment::create([
    'correspondance_id' => $id,
    'title' => 'Scan du courrier',
    'filename' => 'scan_abc123.pdf',
    'original_filename' => 'scan.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 1234,
]);
check(CorrespondanceAttachment::belongsToCorrespondance($attId, $id), 'belongsToCorrespondance true');
check(!CorrespondanceAttachment::belongsToCorrespondance($attId, $idSortant), 'belongsToCorrespondance false for other record');
check(count(CorrespondanceAttachment::getByCorrespondanceId($id)) === 1, 'getByCorrespondanceId');

Correspondance::delete($id);
check(Correspondance::getById($id) === null, 'delete correspondance');
check(count(CorrespondanceAttachment::getByCorrespondanceId($id)) === 0, 'attachments cascade-deleted');

// --- Controller validation rules (private static, via reflection) --------------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\CorrespondanceController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true, ?int $excludeId = null): array =>
    $method->invoke(null, $data, $isCreate, $excludeId);

// Fixture for uniqueness checks: an Entrant record with reference DUP-1
$dupId = Correspondance::create(sampleRow(['reference' => 'DUP-1']));

check($validate(sampleRow(['reference' => 'NEW-1'])) === [], 'valid payload passes');
check(isset($validate(sampleRow(['sens' => 'X', 'reference' => 'NEW-1']))['sens']), 'invalid sens rejected');
check($validate(sampleRow(['sens' => 'Sortant', 'reference' => 'NEW-2'])) === [], 'Sortant accepted');
check(isset($validate(sampleRow(['heure_enregistrement' => '25:00']))['heure_enregistrement']), 'invalid heure rejected');
check(isset($validate(sampleRow(['heure_enregistrement' => '']))['heure_enregistrement']), 'missing heure rejected');
check(isset($validate(sampleRow(['date_correspondance' => '22/08/2026']))['date_correspondance']), 'invalid date format rejected');
check(isset($validate(sampleRow(['reference' => ' ']))['reference']), 'blank reference rejected');
check(isset($validate(sampleRow(['emetteur_destinataire' => '']))['emetteur_destinataire']), 'blank emetteur_destinataire rejected');
check(isset($validate(sampleRow(['objet' => '']))['objet']), 'blank objet rejected');
check(isset($validate(sampleRow(['statut' => 'Nope', 'reference' => 'NEW-1']))['statut']), 'invalid statut rejected');
check(isset($validate(sampleRow(['reference' => 'DUP-1']))['reference']), 'duplicate reference (same sens) rejected');
check($validate(sampleRow(['sens' => 'Sortant', 'reference' => 'DUP-1'])) === [], 'duplicate reference allowed for other sens');
check($validate(sampleRow(['reference' => 'DUP-1']), false, $dupId) === [], 'update excluding self passes uniqueness');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
