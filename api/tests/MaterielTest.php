<?php

/**
 * Matériel model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/MaterielTest.php
 *
 * Creates a scratch database (opus_test_materiel), applies migration
 * database/037_create_materiel.sql, exercises the TypeMateriel and
 * AffectationMateriel models plus the controller validation rules,
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
$scratch = 'opus_test_materiel';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for FKs and joins
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(20) NULL, grade VARCHAR(100) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/037_create_materiel.sql');
foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
    if (preg_match('/^\s*(CREATE|ALTER)/i', preg_replace('/^--.*$/m', '', $stmt))) {
        $pdo->exec($stmt);
    }
}

// Apply migration 038 (verification + signature columns)
$sql038 = file_get_contents($root . '/database/038_add_materiel_verification.sql');
foreach (array_filter(array_map('trim', explode(';', $sql038))) as $stmt) {
    if (preg_match('/^\s*(CREATE|ALTER)/i', preg_replace('/^--.*$/m', '', $stmt))) {
        $pdo->exec($stmt);
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\TypeMateriel;
use App\Models\AffectationMateriel;

// --- Seed personnel ---------------------------------------------------------
$pdo->exec("INSERT INTO personnel (im, grade, firstname, lastname) VALUES ('IM001', 'Brigadier', 'Jean', 'Dupont')");
$pdo->exec("INSERT INTO personnel (im, grade, firstname, lastname) VALUES ('IM002', 'Inspecteur', 'Marie', 'Curie')");
$agent1Id = (int) $pdo->query('SELECT id FROM personnel WHERE im = \'IM001\'')->fetchColumn();
$agent2Id = (int) $pdo->query('SELECT id FROM personnel WHERE im = \'IM002\'')->fetchColumn();

// --- TypeMateriel CRUD ------------------------------------------------------
echo "TypeMateriel CRUD\n";
$typeRadioId = TypeMateriel::create(['nom' => 'Radio', 'description' => 'Talkie-walkie']);
$typeBatonId = TypeMateriel::create(['nom' => 'Bâton', 'description' => null]);
check($typeRadioId > 0 && $typeBatonId > 0, 'create types');
check(TypeMateriel::getById($typeRadioId)['nom'] === 'Radio', 'getById type');
check(TypeMateriel::getByNom('Bâton') !== null, 'getByNom type');
check(count(TypeMateriel::getAll()) === 2, 'getAll types');
check(count(TypeMateriel::getAll(['search' => 'Radio'])) === 1, 'search types');

// Unique nom constraint
$dupRejected = false;
try {
    TypeMateriel::create(['nom' => 'Radio']);
} catch (PDOException $e) {
    $dupRejected = true;
}
check($dupRejected, 'duplicate type nom rejected');

TypeMateriel::update($typeRadioId, ['description' => 'Talkie-walkie VHF']);
check(TypeMateriel::getById($typeRadioId)['description'] === 'Talkie-walkie VHF', 'update type description');

// countAffectationLignes before any assignment
check(TypeMateriel::countAffectationLignes($typeRadioId) === 0, 'countAffectationLignes = 0 before assignments');

// --- AffectationMateriel CRUD -----------------------------------------------
echo "AffectationMateriel CRUD\n";

// Create an assignment with multiple line items (Radio + Bâton)
$affId = AffectationMateriel::create([
    'agent_personnel_id' => $agent1Id,
    'agent_im' => 'IM001',
    'agent_grade' => 'Brigadier',
    'agent_nom' => 'Jean Dupont',
    'date_perception' => '2026-09-01',
    'heure_perception' => '08:00',
    'statut' => 'Assigné',
    'created_by' => null,
]);
AffectationMateriel::createLigne([
    'affectation_id' => $affId,
    'type_materiel_id' => $typeRadioId,
    'type_materiel_nom' => 'Radio',
    'etat_emport' => 'Bon',
]);
AffectationMateriel::createLigne([
    'affectation_id' => $affId,
    'type_materiel_id' => $typeBatonId,
    'type_materiel_nom' => 'Bâton',
    'etat_emport' => 'Neuf',
]);

$row = AffectationMateriel::getById($affId);
check($row !== null, 'create + getById assignment');
check($row['statut'] === 'Assigné', 'statut = Assigné');
check(count($row['lignes']) === 2, 'assignment has 2 lignes');

// getAll with lignes attached
$all = AffectationMateriel::getAll();
check(count($all) === 1 && count($all[0]['lignes']) === 2, 'getAll with lignes');

// Filter by statut
check(count(AffectationMateriel::getAll(['statut' => 'assigne'])) === 1, 'filter statut=assigne');
check(count(AffectationMateriel::getAll(['statut' => 'reintegre'])) === 0, 'filter statut=reintegre (none)');

// Search filter
check(count(AffectationMateriel::getAll(['search' => 'Dupont'])) === 1, 'search by agent name');

// countAffectationLignes after assignment
check(TypeMateriel::countAffectationLignes($typeRadioId) === 1, 'countAffectationLignes = 1 after assignment');

// --- Reintegration (one-way transition) -------------------------------------
echo "Reintegration\n";
$reintOk = AffectationMateriel::reintegrate($affId, [
    'date_reintegration' => '2026-09-05',
    'heure_reintegration' => '17:00',
], [
    // ligne_etats: ligneId => etat
    $row['lignes'][0]['id'] => 'Bon',
    $row['lignes'][1]['id'] => 'Bon',
]);
check($reintOk, 'reintegrate succeeds');
$reintRow = AffectationMateriel::getById($affId);
check($reintRow['statut'] === 'Réintégré', 'statut = Réintégré after reintegration');
check($reintRow['heure_reintegration'] !== null, 'heure_reintegration filled');
check($reintRow['date_reintegration'] === '2026-09-05', 'date_reintegration filled');

// Reintegration is one-way
$reintAgain = AffectationMateriel::reintegrate($affId, [
    'date_reintegration' => '2026-09-10',
    'heure_reintegration' => '10:00',
], []);
check(!$reintAgain, 'reintegrate rejected (already reintegrated)');

// --- etat_reintegration on lignes ---
check($reintRow['lignes'][0]['etat_reintegration'] === 'Bon', 'ligne 0 etat_reintegration filled');
check($reintRow['lignes'][1]['etat_reintegration'] === 'Bon', 'ligne 1 etat_reintegration filled');

// --- Update perception fields ---
echo "Update perception\n";
AffectationMateriel::update($affId, ['observations' => 'Mission spéciale']);
check(AffectationMateriel::getById($affId)['observations'] === 'Mission spéciale', 'update observations');

// --- replaceLignes ---
echo "Replace lignes\n";
AffectationMateriel::replaceLignes($affId, [
    ['type_materiel_id' => $typeRadioId, 'type_materiel_nom' => 'Radio', 'etat_emport' => 'Bon'],
    ['type_materiel_id' => $typeBatonId, 'type_materiel_nom' => 'Bâton', 'etat_emport' => 'Neuf'],
    ['type_materiel_id' => $typeRadioId, 'type_materiel_nom' => 'Radio', 'etat_emport' => 'Moyen'],
]);
$replaced = AffectationMateriel::getById($affId);
check(count($replaced['lignes']) === 3, 'replaceLignes results in 3 lignes');

// --- Delete (cascade) -------------------------------------------------------
echo "Delete (cascade)\n";
AffectationMateriel::delete($affId);
check(AffectationMateriel::getById($affId) === null, 'delete assignment');
check(count(AffectationMateriel::getLignes($affId)) === 0, 'lignes cascade-deleted');

// --- TypeMateriel delete with usage check -----------------------------------
echo "TypeMateriel delete protection\n";
// Create a new assignment to reference the type
$aff2Id = AffectationMateriel::create([
    'agent_personnel_id' => $agent2Id,
    'agent_im' => 'IM002',
    'agent_grade' => 'Inspecteur',
    'agent_nom' => 'Marie Curie',
    'date_perception' => '2026-09-02',
    'heure_perception' => '09:00',
]);
AffectationMateriel::createLigne([
    'affectation_id' => $aff2Id,
    'type_materiel_id' => $typeBatonId,
    'type_materiel_nom' => 'Bâton',
    'etat_emport' => 'Bon',
]);
check(TypeMateriel::countAffectationLignes($typeBatonId) === 1, 'type used by 1 ligne');
// Type delete should be blocked by FK RESTRICT — the controller checks countAffectationLignes first
$typeDeleteRejected = false;
try {
    // Direct DB delete should fail due to FK constraint
    $pdo->exec("DELETE FROM type_materiel WHERE id = $typeBatonId");
} catch (PDOException $e) {
    $typeDeleteRejected = true;
}
check($typeDeleteRejected, 'type delete blocked by FK when in use');

// After deleting the assignment, type can be deleted
AffectationMateriel::delete($aff2Id);
check(TypeMateriel::countAffectationLignes($typeBatonId) === 0, 'type usage = 0 after assignment deleted');
check(TypeMateriel::delete($typeBatonId), 'type delete succeeds when not in use');

// --- Controller validation rules (via reflection) ---------------------------
echo "Controller validation\n";
$method = new ReflectionMethod(\App\Controllers\AffectationMaterielController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true): array =>
    $method->invoke(null, $data, $isCreate);

$validLignes = [
    ['type_materiel_id' => $typeRadioId],
];
$validPayload = [
    'agent_personnel_id' => $agent1Id,
    'date_perception' => '2026-09-01',
    'heure_perception' => '08:00',
    'lignes' => $validLignes,
];
check($validate($validPayload) === [], 'valid payload passes');
check(isset($validate(['agent_personnel_id' => 0, 'date_perception' => '2026-09-01', 'heure_perception' => '08:00', 'lignes' => $validLignes])['agent_personnel_id']), 'missing agent rejected');
check(isset($validate(['agent_personnel_id' => $agent1Id, 'date_perception' => '', 'heure_perception' => '08:00', 'lignes' => $validLignes])['date_perception']), 'missing date rejected');
check(isset($validate(['agent_personnel_id' => $agent1Id, 'date_perception' => '2026-09-01', 'heure_perception' => '25:00', 'lignes' => $validLignes])['heure_perception']), 'invalid heure rejected');
check(isset($validate(['agent_personnel_id' => $agent1Id, 'date_perception' => '2026-09-01', 'heure_perception' => '08:00', 'lignes' => []])['lignes']), 'empty lignes rejected on create');
check(isset($validate(['agent_personnel_id' => $agent1Id, 'date_perception' => '2026-09-01', 'heure_perception' => '08:00', 'lignes' => [['type_materiel_id' => 0]]])['lignes[0].type_materiel_id']), 'ligne missing type rejected');

// TypeMateriel controller validation
echo "TypeMateriel controller validation\n";
$tmMethod = new ReflectionMethod(\App\Controllers\TypeMaterielController::class, 'validate');
$tmMethod->setAccessible(true);
$tmValidate = fn(array $data, bool $isCreate = true, ?int $excludeId = null): array =>
    $tmMethod->invoke(null, $data, $isCreate, $excludeId);

check($tmValidate(['nom' => 'Gilet']) === [], 'valid type payload passes');
check(isset($tmValidate(['nom' => ''])['nom']), 'empty type nom rejected');
check(isset($tmValidate(['nom' => 'Radio'])['nom']), 'duplicate type nom rejected');
check($tmValidate(['nom' => 'Radio'], false, $typeRadioId) === [], 'update excluding self passes uniqueness');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
