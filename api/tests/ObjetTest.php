<?php

/**
 * Objet Saisi / Objet Trouvé (Police Judiciaire) model + validation tests.
 *
 * Usage: php api/tests/ObjetTest.php
 *
 * Creates a scratch database (opus_test_objet), applies migration
 * database/051_create_objet.sql, exercises the ObjetSaisi, ObjetTrouve
 * and attachment models plus the controller validation rules, then
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
$scratch = 'opus_test_objet';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, prenoms VARCHAR(100) NULL, nom VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/051_create_objet.sql');
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

use App\Models\ObjetSaisi;
use App\Models\ObjetSaisiAttachment;
use App\Models\ObjetTrouve;
use App\Models\ObjetTrouveAttachment;
use App\Controllers\ObjetSaisiController;
use App\Controllers\ObjetTrouveController;

// --- Helpers ----------------------------------------------------------------
function sampleSaisi(array $overrides = []): array
{
    return array_merge([
        'numero_dossier' => 'N°DOS/001/26',
        'motif' => 'Objet saisi lors de la perquisition',
        'type_objet' => 'TELEPHONE',
        'proprietaire' => 'Rakoto Jean',
        'created_by' => null,
    ], $overrides);
}

function sampleTrouve(array $overrides = []): array
{
    return array_merge([
        'affaire' => 'Affaire test',
        'motif_decouverte' => 'PERQUISITION',
        'restitution' => false,
        'created_by' => null,
    ], $overrides);
}

// --- ObjetSaisi: CRUD -------------------------------------------------------
echo "ObjetSaisi CRUD\n";
$id = ObjetSaisi::create(sampleSaisi());
$row = ObjetSaisi::find($id);
check($row !== null && $row['type_objet'] === 'TELEPHONE', 'create + find');
check($row['proprietaire'] === 'Rakoto Jean', 'proprietaire persisted');

ObjetSaisi::update($id, sampleSaisi(['type_objet' => 'VEHICULE', 'motif' => 'Updated']));
$updated = ObjetSaisi::find($id);
check($updated['type_objet'] === 'VEHICULE', 'update type_objet');
check($updated['motif'] === 'Updated', 'update motif');

ObjetSaisi::create(sampleSaisi(['type_objet' => 'ARME', 'motif' => 'Arme saisie']));
$all = ObjetSaisi::all();
check(count($all) >= 2, 'all returns at least 2 records');

$armeOnly = ObjetSaisi::all(['type_objet' => 'ARME']);
check(count($armeOnly) >= 1 && $armeOnly[0]['type_objet'] === 'ARME', 'filter by type_objet ARME');

$search = ObjetSaisi::all(['search' => 'Arme saisie']);
check(count($search) >= 1, 'search by motif works');

ObjetSaisi::delete($id);
check(ObjetSaisi::find($id) === null, 'delete removes the record');

// --- ObjetSaisi validation ---------------------------------------------------
echo "ObjetSaisi Validation\n";
$validateSaisi = function (array $data, bool $isCreate = true): array {
    $method = new ReflectionMethod(ObjetSaisiController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate);
};

check($validateSaisi(sampleSaisi()) === [], 'valid payload passes');
check(isset($validateSaisi(sampleSaisi(['motif' => '']))['motif']), 'missing motif rejected');
check(isset($validateSaisi(sampleSaisi(['type_objet' => '']))['type_objet']), 'missing type_objet rejected');
check(isset($validateSaisi(sampleSaisi(['type_objet' => 'NOPE']))['type_objet']), 'invalid type_objet rejected');

// --- ObjetSaisi attachments ---------------------------------------------------
echo "ObjetSaisi Attachments\n";
$saisiId = ObjetSaisi::create(sampleSaisi());
$attId = ObjetSaisiAttachment::create([
    'objet_saisi_id' => $saisiId,
    'title' => 'PV de saisie',
    'filename' => 'pv_saisie.pdf',
    'original_filename' => 'pv.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 1024,
]);
check(ObjetSaisiAttachment::find($attId) !== null, 'attachment create + find');
check(ObjetSaisiAttachment::belongsToObjet($attId, $saisiId), 'belongsToObjet true');
check(!ObjetSaisiAttachment::belongsToObjet($attId, 999999), 'belongsToObjet false for other');
check(count(ObjetSaisiAttachment::listForObjet($saisiId)) === 1, 'listForObjet returns 1');
ObjetSaisiAttachment::updateTitle($attId, 'PV updated');
check(ObjetSaisiAttachment::find($attId)['title'] === 'PV updated', 'updateTitle');
ObjetSaisiAttachment::delete($attId);
check(ObjetSaisiAttachment::find($attId) === null, 'delete attachment');

// --- ObjetTrouve: CRUD --------------------------------------------------------
echo "ObjetTrouve CRUD\n";
$tid = ObjetTrouve::create(sampleTrouve());
$trow = ObjetTrouve::find($tid);
check($trow !== null && $trow['motif_decouverte'] === 'PERQUISITION', 'create + find');
check((int) $trow['restitution'] === 0, 'restitution defaults to 0');

ObjetTrouve::update($tid, sampleTrouve(['restitution' => true, 'affaire' => 'Updated affaire']));
$tupdated = ObjetTrouve::find($tid);
check((int) $tupdated['restitution'] === 1, 'restitution updated to 1');
check($tupdated['affaire'] === 'Updated affaire', 'update affaire');

ObjetTrouve::create(sampleTrouve(['motif_decouverte' => 'SUR_PERSONNE', 'restitution' => true]));
$tall = ObjetTrouve::all();
check(count($tall) >= 2, 'all returns at least 2 records');

$surPersonne = ObjetTrouve::all(['motif_decouverte' => 'SUR_PERSONNE']);
check(count($surPersonne) >= 1 && $surPersonne[0]['motif_decouverte'] === 'SUR_PERSONNE', 'filter by motif_decouverte');

$restitues = ObjetTrouve::all(['restitution' => 1]);
check(count($restitues) >= 1, 'filter by restitution=1');

$tsearch = ObjetTrouve::all(['search' => 'Updated affaire']);
check(count($tsearch) >= 1, 'search by affaire works');

ObjetTrouve::delete($tid);
check(ObjetTrouve::find($tid) === null, 'delete removes the record');

// --- ObjetTrouve validation ----------------------------------------------------
echo "ObjetTrouve Validation\n";
$validateTrouve = function (array $data, bool $isCreate = true): array {
    $method = new ReflectionMethod(ObjetTrouveController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate);
};

check($validateTrouve(sampleTrouve()) === [], 'valid payload passes');
check($validateTrouve(sampleTrouve(['motif_decouverte' => 'REQUISITION'])) === [], 'REQUISITION motif valid');
check($validateTrouve(sampleTrouve(['motif_decouverte' => 'SUR_PERSONNE'])) === [], 'SUR_PERSONNE motif valid');
check(isset($validateTrouve(sampleTrouve(['affaire' => '']))['affaire']), 'missing affaire rejected');
check(isset($validateTrouve(sampleTrouve(['motif_decouverte' => '']))['motif_decouverte']), 'missing motif rejected');
check(isset($validateTrouve(sampleTrouve(['motif_decouverte' => 'NOPE']))['motif_decouverte']), 'invalid motif rejected');

// --- ObjetTrouve attachments -----------------------------------------------------
echo "ObjetTrouve Attachments\n";
$trouveId = ObjetTrouve::create(sampleTrouve());
$tattId = ObjetTrouveAttachment::create([
    'objet_trouve_id' => $trouveId,
    'title' => 'PV de découverte',
    'filename' => 'pv_trouve.pdf',
    'original_filename' => 'pv.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 2048,
]);
check(ObjetTrouveAttachment::find($tattId) !== null, 'attachment create + find');
check(ObjetTrouveAttachment::belongsToObjet($tattId, $trouveId), 'belongsToObjet true');
check(count(ObjetTrouveAttachment::listForObjet($trouveId)) === 1, 'listForObjet returns 1');
ObjetTrouveAttachment::delete($tattId);
check(ObjetTrouveAttachment::find($tattId) === null, 'delete attachment');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
