<?php

/**
 * Registre de déferrement (Police Judiciaire) model + validation tests.
 *
 * Usage: php api/tests/RegistreDeferrementTest.php
 *
 * Creates a scratch database (opus_test_registre_deferrement), applies
 * migration database/066_create_registre_deferrement.sql plus a minimal
 * plainte_sequence table, exercises the RegistreDeferrement + attachment
 * models plus the controller validation rules, then drops the scratch
 * database. Never touches the main `opus` database.
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
$scratch = 'opus_test_registre_deferrement';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(50) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL, grade VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE plainte_sequence (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, type_key VARCHAR(50) NOT NULL, year INT NOT NULL, last_number INT NOT NULL DEFAULT 0, UNIQUE KEY uq_seq (type_key, year)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/066_create_registre_deferrement.sql');
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

use App\Models\RegistreDeferrement;
use App\Models\RegistreDeferrementAttachment;
use App\Models\PlainteSequence;
use App\Controllers\RegistreDeferrementController;

// --- Helpers ----------------------------------------------------------------
function sampleDeferrement(array $overrides = []): array
{
    return array_merge([
        'numero' => 'N°001/MSP/SG/DGPN/DGA/DRSP.1/DEF/CSP/TRIMO/26',
        'date_heure_deferrement' => '2026-09-20 10:30:00',
        'personne_nom' => 'Rakoto Jean',
        'date_lieu_naissance' => 'Né le 12/05/1990 à Antananarivo',
        'infraction' => 'Vol aggravé',
        'numero_dossier' => 'N°001/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/26',
        'autorite' => 'Procureur de la République',
        'destination' => 'Parquet du Tribunal de première instance',
        'escorte' => "ADJ Rabe Paul\nCPL Ralai Eric",
        'suite_donnee' => 'Placement sous mandat de dépôt',
        'observations' => 'Personne déférée à l\'issue de la GAV',
        'created_by' => null,
    ], $overrides);
}

// --- RegistreDeferrement: CRUD ----------------------------------------------
echo "RegistreDeferrement CRUD\n";
$id = RegistreDeferrement::create(sampleDeferrement());
$row = RegistreDeferrement::find($id);
check($row !== null && $row['personne_nom'] === 'Rakoto Jean', 'create + find');
check($row['numero'] === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/DEF/CSP/TRIMO/26', 'numero persisted');
check($row['autorite'] === 'Procureur de la République', 'autorite persisted');
check($row['suite_donnee'] === 'Placement sous mandat de dépôt', 'suite_donnee persisted');

RegistreDeferrement::update($id, sampleDeferrement(['suite_donnee' => 'Convocation à comparaître']));
$updated = RegistreDeferrement::find($id);
check($updated['suite_donnee'] === 'Convocation à comparaître', 'update suite_donnee');

RegistreDeferrement::create(sampleDeferrement(['numero' => 'CUSTOM/DEF/002', 'personne_nom' => 'Rabe Paul']));
$customId = RegistreDeferrement::all(['search' => 'CUSTOM/DEF/002'])[0]['id'] ?? null;
$all = RegistreDeferrement::all();
check(count($all) >= 2, 'all returns at least 2 records');

$search = RegistreDeferrement::all(['search' => 'Rabe Paul']);
check(count($search) >= 1, 'search by personne_nom works');

RegistreDeferrement::delete($id);
check(RegistreDeferrement::find($id) === null, 'delete removes the record');

// --- Numero uniqueness ------------------------------------------------------
echo "Numero uniqueness\n";
check(RegistreDeferrement::numeroExists('CUSTOM/DEF/002'), 'numeroExists finds existing');
check(!RegistreDeferrement::numeroExists('NONEXISTENT/999'), 'numeroExists false for new');
check(!RegistreDeferrement::numeroExists('CUSTOM/DEF/002', $customId), 'numeroExists excludes the record itself');

// --- Validation --------------------------------------------------------------
echo "RegistreDeferrement Validation\n";
$validate = function (array $data, bool $isCreate = true, ?int $excludeId = null): array {
    $method = new ReflectionMethod(RegistreDeferrementController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate, $excludeId);
};

check($validate(sampleDeferrement(['numero' => 'NEW/001'])) === [], 'valid payload passes');
check(isset($validate(sampleDeferrement(['personne_nom' => '']))['personne_nom']), 'missing personne_nom rejected');
check(isset($validate(sampleDeferrement(['date_heure_deferrement' => '']))['date_heure_deferrement']), 'missing date_heure_deferrement rejected');
check(isset($validate(sampleDeferrement(['date_heure_deferrement' => 'not-a-date']))['date_heure_deferrement']), 'invalid date_heure_deferrement rejected');
check(isset($validate(sampleDeferrement(['numero' => 'CUSTOM/DEF/002']))['numero']), 'duplicate numero rejected');
check($validate(sampleDeferrement(['numero' => 'CUSTOM/DEF/002']), true, $customId) === [], 'numero allowed when editing own record');

// --- Sequence: DEF format ----------------------------------------------------
echo "DEF sequence format\n";
$peek = PlainteSequence::peekNumber(PlainteSequence::DEFERREMENT_KEY, 26);
check($peek === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/DEF/CSP/TRIMO/26', "peek returns expected format (got $peek)");
$next = PlainteSequence::nextNumber(PlainteSequence::DEFERREMENT_KEY, 26);
check($next === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/DEF/CSP/TRIMO/26', "nextNumber consumes and returns expected (got $next)");
$next2 = PlainteSequence::nextNumber(PlainteSequence::DEFERREMENT_KEY, 26);
check($next2 === 'N°002/MSP/SG/DGPN/DGA/DRSP.1/DEF/CSP/TRIMO/26', "second call increments (got $next2)");

// --- Attachments -------------------------------------------------------------
echo "RegistreDeferrement Attachments\n";
$defId = RegistreDeferrement::create(sampleDeferrement(['numero' => 'DEF/ATT/001']));
$attId = RegistreDeferrementAttachment::create([
    'registre_deferrement_id' => $defId,
    'title' => 'Ordre de déferrement',
    'filename' => 'ordre.pdf',
    'original_filename' => 'ordre.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 1024,
]);
check(RegistreDeferrementAttachment::find($attId) !== null, 'attachment create + find');
check(RegistreDeferrementAttachment::belongsToDeferrement($attId, $defId), 'belongsToDeferrement true');
check(count(RegistreDeferrementAttachment::listForDeferrement($defId)) === 1, 'listForDeferrement returns 1');
RegistreDeferrementAttachment::updateTitle($attId, 'Ordre updated');
check(RegistreDeferrementAttachment::find($attId)['title'] === 'Ordre updated', 'updateTitle');
RegistreDeferrementAttachment::delete($attId);
check(RegistreDeferrementAttachment::find($attId) === null, 'delete attachment');

// --- Teardown ----------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
