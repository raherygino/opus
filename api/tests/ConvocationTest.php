<?php

/**
 * Convocation (Police Judiciaire) model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/ConvocationTest.php
 *
 * Creates a scratch database (opus_test_convocation), applies migration
 * database/047_create_convocation.sql, exercises the Convocation,
 * PlainteSequence (COV_ST / COV_PD) and attachment models plus the
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
$scratch = 'opus_test_convocation';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FKs to users and personnel (for model joins).
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(50) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL, grade VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

foreach (['046_create_plainte_sequence.sql', '047_create_convocation.sql'] as $file) {
    $sql = file_get_contents($root . '/database/' . $file);
    // Remove comment lines first, then split on semicolons (same as apply scripts).
    $lines = explode("\n", $sql);
    $codeLines = array_filter($lines, fn($l) => !preg_match('/^\s*--/', $l));
    $cleanSql = implode("\n", $codeLines);
    foreach (array_filter(array_map('trim', explode(';', $cleanSql))) as $stmt) {
        if ($stmt !== '') {
            $pdo->exec($stmt);
        }
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\Convocation;
use App\Models\ConvocationAttachment;
use App\Models\PlainteSequence;
use App\Controllers\ConvocationController;

// --- Helpers ----------------------------------------------------------------
function sampleConvocation(array $overrides = []): array
{
    return array_merge([
        'type' => 'ST_PARQUET',
        'date_convocation' => '2026-09-20',
        'nom' => 'Rakoto Jean',
        'adresse' => 'Lot 123 Antananarivo',
        'infraction' => 'Vol',
        'personne_accuse_recu' => 'Rabe Pierre',
        'numero_dossier' => 'N°001/TRIMO/ST/26',
        'observation' => 'Test observation',
        'created_by' => null,
    ], $overrides);
}

// --- PlainteSequence: COV number generation -----------------------------------
echo "PlainteSequence (COV)\n";
$yy = (int) date('y');
$n1 = PlainteSequence::nextNumber('COV_ST');
$n2 = PlainteSequence::nextNumber('COV_ST');
check($n1 === "N°001/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/ST/MC/COV/$yy", "first COV_ST number correct (got $n1)");
check($n2 === "N°002/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/ST/MC/COV/$yy", "second COV_ST increments (got $n2)");

// Per-type independence: COV_PD starts at 001 even though COV_ST is at 002.
$pd1 = PlainteSequence::nextNumber('COV_PD');
check($pd1 === "N°001/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/MC/COV/$yy", "COV_PD sequence independent from COV_ST (got $pd1)");

// Peek without consuming.
$peek = PlainteSequence::peekNumber('COV_PD');
check($peek === "N°002/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/MC/COV/$yy", "peek COV_PD shows next without consuming (got $peek)");

// --- Convocation: create / read --------------------------------------------
echo "Convocation CRUD\n";
$data = sampleConvocation();
$data['numero'] = PlainteSequence::nextNumber('COV_ST');
$id = Convocation::create($data);
$row = Convocation::getById($id);
check($row !== null && $row['type'] === 'ST_PARQUET', 'create + getById ST_PARQUET');
check($row['numero'] === $data['numero'], 'numero persisted');
check($row['nom'] === 'Rakoto Jean', 'nom persisted');

// --- Update -----------------------------------------------------------------
Convocation::update($id, ['nom' => 'Rakoto Jean Updated', 'adresse' => 'Lot 456']);
$updated = Convocation::getById($id);
check($updated['nom'] === 'Rakoto Jean Updated', 'update nom');
check($updated['adresse'] === 'Lot 456', 'update adresse');
check($updated['numero'] === $data['numero'], 'numero unchanged on update');

// --- getAll with filters ----------------------------------------------------
$pdData = sampleConvocation(['type' => 'PLAINTE_DIRECTE']);
$pdData['numero'] = PlainteSequence::nextNumber('COV_PD');
$pdId = Convocation::create($pdData);

$all = Convocation::getAll();
check(count($all) >= 2, 'getAll returns at least 2 records');

$stOnly = Convocation::getAll(['type' => 'ST_PARQUET']);
check(count($stOnly) >= 1 && $stOnly[0]['type'] === 'ST_PARQUET', 'filter by type ST_PARQUET');

$pdOnly = Convocation::getAll(['type' => 'PLAINTE_DIRECTE']);
check(count($pdOnly) >= 1 && $pdOnly[0]['type'] === 'PLAINTE_DIRECTE', 'filter by type PLAINTE_DIRECTE');

$searchResults = Convocation::getAll(['search' => 'Rakoto']);
check(count($searchResults) >= 1, 'search by nom works');

// --- getByNumero ------------------------------------------------------------
$found = Convocation::getByNumero($data['numero']);
check($found !== null && (int) $found['id'] === $id, 'getByNumero finds the record');

// --- Delete -----------------------------------------------------------------
Convocation::delete($id);
check(Convocation::getById($id) === null, 'delete removes the record');

// --- Validation -------------------------------------------------------------
echo "Validation\n";
$validate = function (array $data, bool $isCreate = true, ?int $excludeId = null): array {
    $method = new ReflectionMethod(ConvocationController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate, $excludeId);
};

check($validate(sampleConvocation()) === [], 'valid ST_PARQUET payload passes');
check($validate(sampleConvocation(['type' => 'PLAINTE_DIRECTE'])) === [], 'valid PLAINTE_DIRECTE payload passes');

check(isset($validate(sampleConvocation(['type' => '']))['type']), 'missing type rejected');
check(isset($validate(sampleConvocation(['type' => 'NOPE']))['type']), 'invalid type rejected');
check(isset($validate(sampleConvocation(['date_convocation' => '']))['date_convocation']), 'missing date rejected');
check(isset($validate(sampleConvocation(['date_convocation' => '13/09/2026']))['date_convocation']), 'invalid date format rejected');
check(isset($validate(sampleConvocation(['nom' => '']))['nom']), 'missing nom rejected');
check(isset($validate(sampleConvocation(['infraction' => '']))['infraction']), 'missing infraction rejected');

// --- Editable numero (user-provided) ------------------------------------------
echo "Editable numero\n";
// User can provide a custom numero on create.
$custom = sampleConvocation(['type' => 'PLAINTE_DIRECTE']);
$custom['numero'] = 'N°099/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/MC/COV/' . $yy;
$customId = Convocation::create($custom);
$customRow = Convocation::getById($customId);
check($customRow['numero'] === 'N°099/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/MC/COV/' . $yy, 'user-provided numero persisted on create');

// Duplicate user-provided numero is rejected by validation.
$dupErrors = $validate(sampleConvocation([
    'type' => 'PLAINTE_DIRECTE',
    'numero' => 'N°099/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/MC/COV/' . $yy,
]));
check(isset($dupErrors['numero']), 'duplicate user-provided numero rejected');

// Empty numero on create is allowed (server will auto-generate).
$autoErrors = $validate(sampleConvocation(['numero' => '']));
check(!isset($autoErrors['numero']), 'empty numero on create is valid (auto-generated)');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
