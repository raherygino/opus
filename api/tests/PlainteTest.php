<?php

/**
 * Plainte (Police Judiciaire) model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/PlainteTest.php
 *
 * Creates a scratch database (opus_test_plainte), applies migrations
 * database/044_create_plainte.sql, 045_create_plainte_sortie.sql and
 * 046_create_plainte_sequence.sql, exercises the PlainteEntree,
 * PlainteSortie, PlainteSequence and attachment models plus the
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
$scratch = 'opus_test_plainte';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FKs to users and personnel and the model joins.
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(50) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL, grade VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
// mouvement_personnel is referenced by Personnel::getById() for status; minimal stub.
$pdo->exec('CREATE TABLE mouvement_personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, personnel_id INT UNSIGNED NULL, type_mouvement VARCHAR(100) NULL, retour VARCHAR(10) DEFAULT "Non", created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
// Seed two personnel rows so OPJ/Enquêteur FKs resolve.
$pdo->exec("INSERT INTO personnel (im, firstname, lastname, grade) VALUES ('111', 'Jean', 'Dupont', 'OPJ'), ('222', 'Marie', 'Curie', 'Enquêteur')");

foreach (['044_create_plainte.sql', '045_create_plainte_sortie.sql', '046_create_plainte_sequence.sql'] as $file) {
    $sql = file_get_contents($root . '/database/' . $file);
    foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
        if (preg_match('/^\s*(CREATE|ALTER)/i', preg_replace('/^--.*$/m', '', $stmt))) {
            $pdo->exec($stmt);
        }
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\PlainteEntree;
use App\Models\PlainteSortie;
use App\Models\PlainteSequence;
use App\Models\PlainteEntreeAttachment;
use App\Models\PlainteSortieAttachment;

function sampleEntree(array $overrides = []): array
{
    return array_merge([
        'type' => 'ST_PARQUET',
        'date_plainte' => '2026-09-13',
        'numero_st' => 'ST-001',
        'opj_personnel_id' => 1,
        'enqueteur_personnel_id' => 2,
        'partie_civile' => 'Rakoto Jean',
        'mise_en_cause' => 'Rabe Paul',
        'adresse_pc' => 'Lot 12 Antananarivo',
        'infraction' => 'Vol',
        'prejudice' => '500 000 Ar',
        'lieu_infraction' => 'Marché Analakely',
        'heure_infraction' => '14:30',
        'observation' => 'Plainte déposée au ST',
        'created_by' => null,
    ], $overrides);
}

function sampleSortie(array $overrides = []): array
{
    return array_merge([
        'plainte_entree_id' => 1,
        'nature' => 'DAT',
        'date_sortie' => '2026-09-20',
        'numero_ttr' => 'TTR-001',
        'nom_substitut' => 'Substitut Andry',
        'observation' => 'Sortie DAT',
        'created_by' => null,
    ], $overrides);
}

// --- PlainteSequence: number generation --------------------------------------
echo "PlainteSequence\n";
$yy = (int) date('y');
$n1 = PlainteSequence::nextNumber('ST');
$n2 = PlainteSequence::nextNumber('ST');
check($n1 === "N°001/TRIMO/ST/$yy", "first ST number is N°001/TRIMO/ST/$yy (got $n1)");
check($n2 === "N°002/TRIMO/ST/$yy", "second ST number increments to N°002 (got $n2)");

// Per-type independence: PD starts at 001 even though ST is at 002.
$pd1 = PlainteSequence::nextNumber('PD');
check($pd1 === "N°001/TRIMO/PD/$yy", "PD sequence independent from ST (got $pd1)");

// SORTIE format.
$s1 = PlainteSequence::nextNumber('SORTIE');
check($s1 === "N°001/MSP/DGPN/DGA/DRSP-1/CSP/A-TRIMO/$yy", "SORTIE format correct (got $s1)");

// Peek without consuming.
$peek = PlainteSequence::peekNumber('RP');
check($peek === "N°001/TRIMO/RP/$yy", "peek RP shows next without consuming (got $peek)");
$rp1 = PlainteSequence::nextNumber('RP');
check($rp1 === "N°001/TRIMO/RP/$yy", "RP still starts at 001 after peek (got $rp1)");

// --- PlainteEntree: create / read --------------------------------------------
echo "PlainteEntree CRUD\n";
$data = sampleEntree();
$data['numero_dossier'] = PlainteSequence::nextNumber('ST');
$id = PlainteEntree::create($data);
$row = PlainteEntree::getById($id);
check($row !== null && $row['type'] === 'ST_PARQUET', 'create + getById ST_PARQUET');
check($row['numero_dossier'] === $data['numero_dossier'], 'numero_dossier persisted');
check($row['opj_im'] === '111', 'OPJ join resolves personnel');
check($row['enqueteur_grade'] === 'Enquêteur', 'Enquêteur join resolves grade');

// Plainte Directe (no numero_st, has PC + adresse).
$pdData = sampleEntree([
    'type' => 'PLAINTE_DIRECTE',
    'numero_st' => null,
    'numero_dossier' => PlainteSequence::nextNumber('PD'),
]);
$idPd = PlainteEntree::create($pdData);
check(PlainteEntree::getById($idPd)['type'] === 'PLAINTE_DIRECTE', 'create PLAINTE_DIRECTE');

// Rapport de Police (no PC, no adresse, no numero_st).
$rpData = sampleEntree([
    'type' => 'RAPPORT_POLICE',
    'numero_st' => null,
    'partie_civile' => null,
    'adresse_pc' => null,
    'numero_dossier' => PlainteSequence::nextNumber('RP'),
]);
$idRp = PlainteEntree::create($rpData);
check(PlainteEntree::getById($idRp)['type'] === 'RAPPORT_POLICE', 'create RAPPORT_POLICE');

// --- PlainteEntree: update / filters / delete --------------------------------
check(PlainteEntree::update($id, ['observation' => 'Observation modifiée']), 'update observation');
check(PlainteEntree::getById($id)['observation'] === 'Observation modifiée', 'observation persisted');
check(count(PlainteEntree::getAll(['type' => 'ST_PARQUET'])) === 1, 'filter by type ST_PARQUET');
check(count(PlainteEntree::getAll(['type' => 'PLAINTE_DIRECTE'])) === 1, 'filter by type PLAINTE_DIRECTE');
check(count(PlainteEntree::getAll(['search' => 'Rakoto'])) === 2, 'search by partie_civile matches ST + PD');
check(count(PlainteEntree::getAll(['date_from' => '2026-09-13'])) === 3, 'date_from filter');
check(count(PlainteEntree::getAll(['date_from' => '2026-09-14'])) === 0, 'date_from no match');

// Uniqueness.
$duplicate = PlainteEntree::getByNumeroDossier($data['numero_dossier']);
check($duplicate !== null && (int) $duplicate['id'] === $id, 'getByNumeroDossier finds row');

// --- PlainteEntree attachments ----------------------------------------------
echo "PlainteEntree Attachments\n";
$attId = PlainteEntreeAttachment::create([
    'plainte_entree_id' => $id,
    'title' => 'Procès-verbal',
    'filename' => 'pv_abc.pdf',
    'original_filename' => 'pv.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 1234,
]);
check(PlainteEntreeAttachment::belongsToPlainteEntree($attId, $id), 'belongsToPlainteEntree true');
check(!PlainteEntreeAttachment::belongsToPlainteEntree($attId, $idPd), 'belongsToPlainteEntree false for other record');
check(count(PlainteEntreeAttachment::getByPlainteEntreeId($id)) === 1, 'getByPlainteEntreeId');

// --- getWithoutSortie --------------------------------------------------------
echo "PlainteEntree withoutSortie\n";
$without = PlainteEntree::getWithoutSortie();
check(count($without) === 3, 'all 3 entrees have no sortie initially');

// --- PlainteSortie: create / read -------------------------------------------
echo "PlainteSortie CRUD\n";
$sortieData = sampleSortie(['plainte_entree_id' => $id]);
$sortieData['numero'] = PlainteSequence::nextNumber('SORTIE');
$sid = PlainteSortie::create($sortieData);
$srow = PlainteSortie::getById($sid);
check($srow !== null && $srow['nature'] === 'DAT', 'create + getById DAT');
check($srow['entree_numero_dossier'] === $data['numero_dossier'], 'joined ENTRÉE numero_dossier');

// Déferrement (requires date_deferrement).
$defData = sampleSortie([
    'plainte_entree_id' => $idPd,
    'nature' => 'DEFERREMENT',
    'date_deferrement' => '2026-09-25',
    'numero' => PlainteSequence::nextNumber('SORTIE'),
]);
$sidDef = PlainteSortie::create($defData);
check(PlainteSortie::getById($sidDef)['nature'] === 'DEFERREMENT', 'create DEFERREMENT');
check(PlainteSortie::getById($sidDef)['date_deferrement'] === '2026-09-25', 'date_deferrement persisted');

// --- ENTRÉE ↔ SORTIE relationship -------------------------------------------
echo "ENTRÉE ↔ SORTIE relationship\n";
check(PlainteSortie::getByEntreeId($id)['id'] === $sid, 'getByEntreeId finds linked sortie');
$without2 = PlainteEntree::getWithoutSortie();
check(count($without2) === 1, 'only 1 entree without sortie after linking 2');
check(PlainteEntree::getById($idRp)['id'] === $without2[0]['id'], 'remaining entree is the RP one');

// --- PlainteSortie filters --------------------------------------------------
check(count(PlainteSortie::getAll(['nature' => 'DAT'])) === 1, 'filter sortie by nature DAT');
check(count(PlainteSortie::getAll(['nature' => 'DEFERREMENT'])) === 1, 'filter sortie by nature DEFERREMENT');
check(count(PlainteSortie::getAll(['entree_id' => $id])) === 1, 'filter sortie by entree_id');

// --- PlainteSortie attachments ----------------------------------------------
echo "PlainteSortie Attachments\n";
$sAttId = PlainteSortieAttachment::create([
    'plainte_sortie_id' => $sid,
    'title' => 'Lettre TTR',
    'filename' => 'ttr_xyz.pdf',
    'original_filename' => 'ttr.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 2345,
]);
check(PlainteSortieAttachment::belongsToPlainteSortie($sAttId, $sid), 'belongsToPlainteSortie true');
check(count(PlainteSortieAttachment::getByPlainteSortieId($sid)) === 1, 'getByPlainteSortieId');

// --- Cascade delete: ENTRÉE deletion removes its SORTIE --------------------
echo "Cascade delete\n";
PlainteEntree::delete($id);
check(PlainteEntree::getById($id) === null, 'delete entree');
check(PlainteSortie::getById($sid) === null, 'linked sortie cascade-deleted with entree');
check(count(PlainteEntreeAttachment::getByPlainteEntreeId($id)) === 0, 'entree attachments cascade-deleted');
check(count(PlainteSortieAttachment::getByPlainteSortieId($sid)) === 0, 'sortie attachments cascade-deleted with entree');

// --- Controller validation rules (private static, via reflection) -----------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\PlainteEntreeController::class, 'validate');
$method->setAccessible(true);
$validateEntree = fn(array $data, bool $isCreate = true): array =>
    $method->invoke(null, $data, $isCreate);

check($validateEntree(sampleEntree()) === [], 'valid ST_PARQUET payload passes');
check($validateEntree(sampleEntree(['type' => 'PLAINTE_DIRECTE', 'numero_st' => null])) === [], 'valid PLAINTE_DIRECTE payload passes');
check($validateEntree(sampleEntree(['type' => 'RAPPORT_POLICE', 'numero_st' => null, 'partie_civile' => null, 'adresse_pc' => null])) === [], 'valid RAPPORT_POLICE payload passes');

// Type-conditional: ST requires numero_st.
check(isset($validateEntree(sampleEntree(['numero_st' => '']))['numero_st']), 'ST_PARQUET requires numero_st');
// Type-conditional: PD does NOT require numero_st.
check(!isset($validateEntree(sampleEntree(['type' => 'PLAINTE_DIRECTE', 'numero_st' => null]))['numero_st']), 'PLAINTE_DIRECTE does not require numero_st');
// Type-conditional: PD requires partie_civile.
check(isset($validateEntree(sampleEntree(['type' => 'PLAINTE_DIRECTE', 'numero_st' => null, 'partie_civile' => '']))['partie_civile']), 'PLAINTE_DIRECTE requires partie_civile');
// Type-conditional: RP does NOT require partie_civile.
check(!isset($validateEntree(sampleEntree(['type' => 'RAPPORT_POLICE', 'numero_st' => null, 'partie_civile' => null, 'adresse_pc' => null]))['partie_civile']), 'RAPPORT_POLICE does not require partie_civile');
// All types require mise_en_cause.
check(isset($validateEntree(sampleEntree(['mise_en_cause' => '']))['mise_en_cause']), 'mise_en_cause required for all types');
// Invalid type.
check(isset($validateEntree(sampleEntree(['type' => 'NOPE']))['type']), 'invalid type rejected');
// Missing date.
check(isset($validateEntree(sampleEntree(['date_plainte' => '']))['date_plainte']), 'missing date rejected');
// Invalid date format.
check(isset($validateEntree(sampleEntree(['date_plainte' => '13/09/2026']))['date_plainte']), 'invalid date format rejected');
// Invalid heure format.
check(isset($validateEntree(sampleEntree(['heure_infraction' => '25:00']))['heure_infraction']), 'invalid heure rejected');
// Missing OPJ.
check(isset($validateEntree(sampleEntree(['opj_personnel_id' => null]))['opj_personnel_id']), 'missing OPJ rejected');
// Non-existent OPJ.
check(isset($validateEntree(sampleEntree(['opj_personnel_id' => 99999]))['opj_personnel_id']), 'non-existent OPJ rejected');

// Sortie validation. Use idPd (still exists after the cascade-delete test
// removed entree id=1) as the linked ENTRÉE.
$methodS = new ReflectionMethod(\App\Controllers\PlainteSortieController::class, 'validate');
$methodS->setAccessible(true);
$validateSortie = fn(array $data, bool $isCreate = true): array =>
    $methodS->invoke(null, $data, $isCreate);

$validSortie = sampleSortie(['plainte_entree_id' => $idPd]);
$validDef = sampleSortie(['plainte_entree_id' => $idPd, 'nature' => 'DEFERREMENT', 'date_deferrement' => '2026-09-25']);
check($validateSortie($validSortie) === [], 'valid DAT sortie payload passes');
check($validateSortie($validDef) === [], 'valid DEFERREMENT sortie payload passes');
// DEFERREMENT requires date_deferrement.
check(isset($validateSortie(sampleSortie(['plainte_entree_id' => $idPd, 'nature' => 'DEFERREMENT', 'date_deferrement' => null]))['date_deferrement']), 'DEFERREMENT requires date_deferrement');
// DAT does NOT require date_deferrement.
check(!isset($validateSortie(sampleSortie(['plainte_entree_id' => $idPd, 'nature' => 'DAT', 'date_deferrement' => null]))['date_deferrement']), 'DAT does not require date_deferrement');
check(isset($validateSortie(sampleSortie(['plainte_entree_id' => $idPd, 'nature' => '']))['nature']), 'missing nature rejected');
check(isset($validateSortie(sampleSortie(['plainte_entree_id' => $idPd, 'nature' => 'NOPE']))['nature']), 'invalid nature rejected');
check(isset($validateSortie(sampleSortie(['plainte_entree_id' => $idPd, 'numero_ttr' => '']))['numero_ttr']), 'missing numero_ttr rejected');
check(isset($validateSortie(sampleSortie(['plainte_entree_id' => $idPd, 'nom_substitut' => '']))['nom_substitut']), 'missing nom_substitut rejected');
check(isset($validateSortie(sampleSortie(['plainte_entree_id' => null]))['plainte_entree_id']), 'missing plainte_entree_id rejected');
check(isset($validateSortie(sampleSortie(['plainte_entree_id' => 99999]))['plainte_entree_id']), 'non-existent plainte_entree_id rejected');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
