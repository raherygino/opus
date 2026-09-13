<?php

/**
 * Matériel roulant model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/MaterielRoulantTest.php
 *
 * Creates a scratch database (opus_test_materiel_roulant), applies migration
 * database/039_create_materiel_roulant.sql, exercises the MaterielRoulant
 * model plus the controller validation rules (perception + reintegration),
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
$scratch = 'opus_test_materiel_roulant';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for FKs and joins
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(20) NULL, grade VARCHAR(100) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL, code_secret_hash VARCHAR(255) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/039_create_materiel_roulant.sql');
foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
    if (preg_match('/^\s*(CREATE|ALTER)/i', preg_replace('/^--.*$/m', '', $stmt))) {
        $pdo->exec($stmt);
    }
}

// Apply the verification columns migration (040)
$sql040 = file_get_contents($root . '/database/040_add_materiel_roulant_verification.sql');
foreach (array_filter(array_map('trim', explode(';', $sql040))) as $stmt) {
    if (preg_match('/^\s*(CREATE|ALTER)/i', preg_replace('/^--.*$/m', '', $stmt))) {
        $pdo->exec($stmt);
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\MaterielRoulant;
use App\Models\Personnel;

// --- Seed personnel ---------------------------------------------------------
$pdo->exec("INSERT INTO personnel (im, grade, firstname, lastname) VALUES ('IM001', 'Brigadier', 'Jean', 'Dupont')");
$pdo->exec("INSERT INTO personnel (im, grade, firstname, lastname) VALUES ('IM002', 'Inspecteur', 'Marie', 'Curie')");
$agent1Id = (int) $pdo->query('SELECT id FROM personnel WHERE im = \'IM001\'')->fetchColumn();
$agent2Id = (int) $pdo->query('SELECT id FROM personnel WHERE im = \'IM002\'')->fetchColumn();

// --- MaterielRoulant CRUD ---------------------------------------------------
echo "MaterielRoulant CRUD\n";

// Create a VHL perception with driver + chef de bord + departure readings
$mrId = MaterielRoulant::create([
    'date_perception' => '2026-09-01',
    'heure_perception' => '08:00',
    'type_materiel' => 'VHL',
    'numero_immatriculation' => '1234 AB 75',
    'description_vehicule' => 'S.U.V 4x4 — Nissan Patrol',
    'agent_conducteur_personnel_id' => $agent1Id,
    'agent_conducteur_im' => 'IM001',
    'agent_conducteur_grade' => 'Brigadier',
    'agent_conducteur_nom' => 'Jean Dupont',
    'chef_de_bord_personnel_id' => $agent2Id,
    'chef_de_bord_im' => 'IM002',
    'chef_de_bord_grade' => 'Inspecteur',
    'chef_de_bord_nom' => 'Marie Curie',
    'kilometrage_depart' => 12345.0,
    'niveau_carburant_depart' => 80.0,
    'agent_verifie' => 1,
    'agent_verifie_at' => '2026-09-01 08:00:00',
    'signature_svg' => '<svg></svg>',
    'statut' => 'En service',
    'created_by' => null,
]);

check($mrId > 0, 'create perception');

$row = MaterielRoulant::getById($mrId);
check($row !== null, 'getById perception');
check($row['statut'] === 'En service', 'statut = En service');
check($row['type_materiel'] === 'VHL', 'type_materiel = VHL');
check($row['numero_immatriculation'] === '1234 AB 75', 'numero_immatriculation stored');
check($row['description_vehicule'] === 'S.U.V 4x4 — Nissan Patrol', 'description_vehicule stored');
check($row['agent_conducteur_nom'] === 'Jean Dupont', 'agent_conducteur_nom snapshot');
check($row['chef_de_bord_nom'] === 'Marie Curie', 'chef_de_bord_nom snapshot');
check((int) $row['agent_verifie'] === 1, 'agent_verifie stored');
check($row['agent_verifie_at'] === '2026-09-01 08:00:00', 'agent_verifie_at stored');
check($row['signature_svg'] === '<svg></svg>', 'signature_svg stored');
check($row['heure_reintegration'] === null, 'heure_reintegration null (en service)');
check((float) $row['kilometrage_depart'] === 12345.0, 'kilometrage_depart stored');

// getAll with one row
$all = MaterielRoulant::getAll();
check(count($all) === 1, 'getAll returns 1 row');

// Filter by statut
check(count(MaterielRoulant::getAll(['statut' => 'en_service'])) === 1, 'filter statut=en_service');
check(count(MaterielRoulant::getAll(['statut' => 'reintegre'])) === 0, 'filter statut=reintegre (none)');

// Filter by type_materiel
check(count(MaterielRoulant::getAll(['type_materiel' => 'VHL'])) === 1, 'filter type=VHL');
check(count(MaterielRoulant::getAll(['type_materiel' => 'Moto'])) === 0, 'filter type=Moto (none)');

// Filter by agent_conducteur_personnel_id
check(count(MaterielRoulant::getAll(['agent_conducteur_personnel_id' => $agent1Id])) === 1, 'filter by driver');
check(count(MaterielRoulant::getAll(['agent_conducteur_personnel_id' => $agent2Id])) === 0, 'filter by non-driver (none)');

// Date range filter
check(count(MaterielRoulant::getAll(['date_from' => '2026-09-01'])) === 1, 'filter date_from');
check(count(MaterielRoulant::getAll(['date_from' => '2026-09-02'])) === 0, 'filter date_from (none)');
check(count(MaterielRoulant::getAll(['date_to' => '2026-09-01'])) === 1, 'filter date_to');
check(count(MaterielRoulant::getAll(['date_to' => '2026-08-31'])) === 0, 'filter date_to (none)');

// Search filter
check(count(MaterielRoulant::getAll(['search' => 'Dupont'])) === 1, 'search by driver name');
check(count(MaterielRoulant::getAll(['search' => 'Curie'])) === 1, 'search by chef de bord name');
check(count(MaterielRoulant::getAll(['search' => '1234 AB'])) === 1, 'search by numero_immatriculation');
check(count(MaterielRoulant::getAll(['search' => 'Nissan'])) === 1, 'search by description_vehicule');

// --- Update perception fields (only allowed fields) -------------------------
echo "Update perception\n";
MaterielRoulant::update($mrId, [
    'kilometrage_depart' => 13000.0,
    'niveau_carburant_depart' => 90.0,
    'numero_immatriculation' => '5678 CD 75',
    'description_vehicule' => 'Berline — Toyota Camry',
]);
$updated = MaterielRoulant::getById($mrId);
check((float) $updated['kilometrage_depart'] === 13000.0, 'kilometrage_depart updated');
check((float) $updated['niveau_carburant_depart'] === 90.0, 'niveau_carburant_depart updated');
check($updated['numero_immatriculation'] === '5678 CD 75', 'numero_immatriculation updated');
check($updated['description_vehicule'] === 'Berline — Toyota Camry', 'description_vehicule updated');
check($updated['statut'] === 'En service', 'statut unchanged after update');

// Attempt to update a reintegration-only field via update() must be ignored
// (the model's allow-list excludes these columns).
MaterielRoulant::update($mrId, ['heure_reintegration' => '17:00']);
$afterIgnore = MaterielRoulant::getById($mrId);
check($afterIgnore['heure_reintegration'] === null, 'update() cannot set heure_reintegration');

// update() with no allowed fields returns false
check(MaterielRoulant::update($mrId, ['heure_reintegration' => '17:00']) === false, 'update() returns false when no allowed fields');

// --- Reintegration (one-way transition) -------------------------------------
echo "Reintegration\n";
$reintOk = MaterielRoulant::reintegrate($mrId, [
    'date_reintegration' => '2026-09-05',
    'heure_reintegration' => '17:00',
    'kilometrage_retour' => 13100.0,
    'niveau_carburant_retour' => 40.0,
    'observations_techniques' => 'RAS',
    'defaillances' => 'Phare gauche éteint',
]);
check($reintOk, 'reintegrate succeeds');
$reintRow = MaterielRoulant::getById($mrId);
check($reintRow['statut'] === 'Réintégré', 'statut = Réintégré after reintegration');
check($reintRow['heure_reintegration'] !== null, 'heure_reintegration filled');
check($reintRow['date_reintegration'] === '2026-09-05', 'date_reintegration filled');
check((float) $reintRow['kilometrage_retour'] === 13100.0, 'kilometrage_retour filled');
check($reintRow['observations_techniques'] === 'RAS', 'observations_techniques filled');
check($reintRow['defaillances'] === 'Phare gauche éteint', 'defaillances filled');

// Reintegration is one-way
$reintAgain = MaterielRoulant::reintegrate($mrId, [
    'date_reintegration' => '2026-09-10',
    'heure_reintegration' => '10:00',
    'kilometrage_retour' => 13200.0,
]);
check(!$reintAgain, 'reintegrate rejected (already reintegrated)');
$reintRow2 = MaterielRoulant::getById($mrId);
check($reintRow2['date_reintegration'] === '2026-09-05', 'reintegration fields not overwritten');

// After reintegration, statut filter reflects the new state
check(count(MaterielRoulant::getAll(['statut' => 'reintegre'])) === 1, 'filter statut=reintegre after reintegration');
check(count(MaterielRoulant::getAll(['statut' => 'en_service'])) === 0, 'filter statut=en_service (none) after reintegration');

// --- Delete -----------------------------------------------------------------
echo "Delete\n";
MaterielRoulant::delete($mrId);
check(MaterielRoulant::getById($mrId) === null, 'delete perception');

// --- Controller validation (perception) via reflection ----------------------
echo "Controller validation (perception)\n";
$method = new ReflectionMethod(\App\Controllers\MaterielRoulantController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true): array =>
    $method->invoke(null, $data, $isCreate);

$validPayload = [
    'date_perception' => '2026-09-01',
    'heure_perception' => '08:00',
    'type_materiel' => 'VHL',
    'agent_conducteur_personnel_id' => $agent1Id,
];
check($validate($validPayload) === [], 'valid perception payload passes');

check(isset($validate(['date_perception' => '', 'heure_perception' => '08:00', 'type_materiel' => 'VHL', 'agent_conducteur_personnel_id' => $agent1Id])['date_perception']), 'missing date_perception rejected');
check(isset($validate(['date_perception' => '2026-13-01', 'heure_perception' => '08:00', 'type_materiel' => 'VHL', 'agent_conducteur_personnel_id' => $agent1Id])['date_perception']), 'invalid date rejected');
check(isset($validate(['date_perception' => '2026-09-01', 'heure_perception' => '', 'type_materiel' => 'VHL', 'agent_conducteur_personnel_id' => $agent1Id])['heure_perception']), 'missing heure_perception rejected');
check(isset($validate(['date_perception' => '2026-09-01', 'heure_perception' => '25:00', 'type_materiel' => 'VHL', 'agent_conducteur_personnel_id' => $agent1Id])['heure_perception']), 'invalid heure rejected');
check(isset($validate(['date_perception' => '2026-09-01', 'heure_perception' => '08:00', 'type_materiel' => '', 'agent_conducteur_personnel_id' => $agent1Id])['type_materiel']), 'missing type_materiel rejected');
check(isset($validate(['date_perception' => '2026-09-01', 'heure_perception' => '08:00', 'type_materiel' => 'Camion', 'agent_conducteur_personnel_id' => $agent1Id])['type_materiel']), 'invalid type_materiel rejected');
check(isset($validate(['date_perception' => '2026-09-01', 'heure_perception' => '08:00', 'type_materiel' => 'VHL', 'agent_conducteur_personnel_id' => 0])['agent_conducteur_personnel_id']), 'missing driver rejected on create');
check(isset($validate(['date_perception' => '2026-09-01', 'heure_perception' => '08:00', 'type_materiel' => 'VHL', 'agent_conducteur_personnel_id' => $agent1Id, 'kilometrage_depart' => -5])['kilometrage_depart']), 'negative kilometrage_depart rejected');
check(isset($validate(['date_perception' => '2026-09-01', 'heure_perception' => '08:00', 'type_materiel' => 'VHL', 'agent_conducteur_personnel_id' => $agent1Id, 'niveau_carburant_depart' => 150])['niveau_carburant_depart']), 'carburant > 100 rejected');

// On update (isCreate=false), missing driver is allowed (driver not required to be re-sent)
check($validate(['kilometrage_depart' => 20000], false) === [], 'update payload without driver passes');

// --- Controller validation (reintegration) via reflection -------------------
echo "Controller validation (reintegration)\n";
$reintMethod = new ReflectionMethod(\App\Controllers\MaterielRoulantController::class, 'validateReintegration');
$reintMethod->setAccessible(true);

// A perception record used as the "current state" for reintegration validation
$baseMateriel = [
    'date_perception' => '2026-09-01',
    'heure_perception' => '08:00',
    'kilometrage_depart' => 12345.0,
    'heure_reintegration' => null,
];

$validReint = fn(array $data): array =>
    $reintMethod->invoke(null, $data, $baseMateriel);

$validReintPayload = [
    'date_reintegration' => '2026-09-05',
    'heure_reintegration' => '17:00',
    'kilometrage_retour' => 13100,
];
check($validReint($validReintPayload) === [], 'valid reintegration payload passes');

check(isset($validReint(['date_reintegration' => '', 'heure_reintegration' => '17:00', 'kilometrage_retour' => 13100])['date_reintegration']), 'missing date_reintegration rejected');
check(isset($validReint(['date_reintegration' => '2026-08-31', 'heure_reintegration' => '17:00', 'kilometrage_retour' => 13100])['date_reintegration']), 'date_reintegration before perception rejected');
check(isset($validReint(['date_reintegration' => '2026-09-01', 'heure_reintegration' => '07:00', 'kilometrage_retour' => 13100])['heure_reintegration']), 'heure_reintegration before perception (same day) rejected');
check(isset($validReint(['date_reintegration' => '2026-09-05', 'heure_reintegration' => '', 'kilometrage_retour' => 13100])['heure_reintegration']), 'missing heure_reintegration rejected');
check(isset($validReint(['date_reintegration' => '2026-09-05', 'heure_reintegration' => '25:00', 'kilometrage_retour' => 13100])['heure_reintegration']), 'invalid heure_reintegration rejected');
check(isset($validReint(['date_reintegration' => '2026-09-05', 'heure_reintegration' => '17:00', 'kilometrage_retour' => ''])['kilometrage_retour']), 'missing kilometrage_retour rejected');
check(isset($validReint(['date_reintegration' => '2026-09-05', 'heure_reintegration' => '17:00', 'kilometrage_retour' => -10])['kilometrage_retour']), 'negative kilometrage_retour rejected');
check(isset($validReint(['date_reintegration' => '2026-09-05', 'heure_reintegration' => '17:00', 'kilometrage_retour' => 1000])['kilometrage_retour']), 'kilometrage_retour < depart rejected');
check(isset($validReint(['date_reintegration' => '2026-09-05', 'heure_reintegration' => '17:00', 'kilometrage_retour' => 13100, 'niveau_carburant_retour' => 150])['niveau_carburant_retour']), 'carburant_retour > 100 rejected');
check(isset($validReint(['date_reintegration' => '2026-09-05', 'heure_reintegration' => '17:00', 'kilometrage_retour' => 13100, 'observations_techniques' => 'Panne', 'defaillances' => 'Panne'])['defaillances']), 'identical observations/defaillances rejected');

// --- Conducteur code secret verification -------------------------------------
echo "Conducteur code secret verification\n";
check(!Personnel::verifyCodeSecret($agent1Id, '4321'), 'verifyCodeSecret false when no code set');
check(Personnel::setCodeSecret($agent1Id, '4321'), 'setCodeSecret succeeds');
check(Personnel::verifyCodeSecret($agent1Id, '4321'), 'verifyCodeSecret true with correct code');
check(!Personnel::verifyCodeSecret($agent1Id, '1234'), 'verifyCodeSecret false with wrong code');

// A perception created with verification fields retains them
$mrVerified = MaterielRoulant::create([
    'date_perception' => '2026-09-02',
    'heure_perception' => '09:00',
    'type_materiel' => 'Moto',
    'agent_conducteur_personnel_id' => $agent1Id,
    'agent_conducteur_im' => 'IM001',
    'agent_conducteur_grade' => 'Brigadier',
    'agent_conducteur_nom' => 'Jean Dupont',
    'kilometrage_depart' => 5000.0,
    'niveau_carburant_depart' => 90.0,
    'agent_verifie' => 1,
    'agent_verifie_at' => '2026-09-02 09:00:00',
    'signature_svg' => '<svg path="d="M0 0"></svg>',
    'statut' => 'En service',
    'created_by' => null,
]);
$rowV = MaterielRoulant::getById($mrVerified);
check((int) $rowV['agent_verifie'] === 1, 'verified perception agent_verifie = 1');
check($rowV['agent_verifie_at'] === '2026-09-02 09:00:00', 'verified perception agent_verifie_at stored');
check($rowV['signature_svg'] === '<svg path="d="M0 0"></svg>', 'verified perception signature_svg stored');

// A perception created without verification fields defaults to non-verified
$mrUnverified = MaterielRoulant::create([
    'date_perception' => '2026-09-03',
    'heure_perception' => '10:00',
    'type_materiel' => 'VHL',
    'agent_conducteur_personnel_id' => $agent2Id,
    'agent_conducteur_im' => 'IM002',
    'agent_conducteur_grade' => 'Inspecteur',
    'agent_conducteur_nom' => 'Marie Curie',
    'kilometrage_depart' => 1000.0,
    'niveau_carburant_depart' => 50.0,
    'statut' => 'En service',
    'created_by' => null,
]);
$rowU = MaterielRoulant::getById($mrUnverified);
check((int) $rowU['agent_verifie'] === 0, 'unverified perception agent_verifie = 0');
check($rowU['agent_verifie_at'] === null, 'unverified perception agent_verifie_at null');
check($rowU['signature_svg'] === null, 'unverified perception signature_svg null');

// --- Teardown ---------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
