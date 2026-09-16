<?php

/**
 * Perquisition (Police Judiciaire) model + validation tests.
 *
 * Usage: php api/tests/PerquisitionTest.php
 *
 * Creates a scratch database (opus_test_perquisition), applies migration
 * database/052_create_perquisition.sql plus a minimal plainte_sequence
 * table, exercises the Perquisition + attachment models plus the
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
$scratch = 'opus_test_perquisition';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(50) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL, grade VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE plainte_sequence (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, type_key VARCHAR(50) NOT NULL, year INT NOT NULL, last_number INT NOT NULL DEFAULT 0, UNIQUE KEY uq_seq (type_key, year)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/052_create_perquisition.sql');
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

use App\Models\Perquisition;
use App\Models\PerquisitionAttachment;
use App\Models\PlainteSequence;
use App\Controllers\PerquisitionController;

// --- Helpers ----------------------------------------------------------------
function samplePerquisition(array $overrides = []): array
{
    return array_merge([
        'numero' => 'N°001/MSP/SG/DGPN/DGA/DRSP.1/PEQ/CSP/TRIMO/26',
        'numero_ttr' => 'TTR-001/26',
        'substitut' => 'Rabe Jacques',
        'affaire' => 'Vol aggravé',
        'motif' => 'Perquisition domicile',
        'created_by' => null,
    ], $overrides);
}

// --- Perquisition: CRUD -----------------------------------------------------
echo "Perquisition CRUD\n";
$id = Perquisition::create(samplePerquisition());
$row = Perquisition::find($id);
check($row !== null && $row['affaire'] === 'Vol aggravé', 'create + find');
check($row['numero'] === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/PEQ/CSP/TRIMO/26', 'numero persisted');
check($row['substitut'] === 'Rabe Jacques', 'substitut persisted');

Perquisition::update($id, samplePerquisition(['affaire' => 'Vol aggravé modifié']));
$updated = Perquisition::find($id);
check($updated['affaire'] === 'Vol aggravé modifié', 'update affaire');

Perquisition::create(samplePerquisition(['numero' => 'CUSTOM/002', 'affaire' => 'Recel']));
$customId = Perquisition::all(['search' => 'CUSTOM/002'])[0]['id'] ?? null;
$all = Perquisition::all();
check(count($all) >= 2, 'all returns at least 2 records');

$search = Perquisition::all(['search' => 'Recel']);
check(count($search) >= 1, 'search by affaire works');

Perquisition::delete($id);
check(Perquisition::find($id) === null, 'delete removes the record');

// --- Numero uniqueness ------------------------------------------------------
echo "Numero uniqueness\n";
check(Perquisition::numeroExists('CUSTOM/002'), 'numeroExists finds existing');
check(!Perquisition::numeroExists('NONEXISTENT/999'), 'numeroExists false for new');
check(!Perquisition::numeroExists('CUSTOM/002', $customId), 'numeroExists excludes the record itself');

// --- Perquisition validation -------------------------------------------------
echo "Perquisition Validation\n";
$validate = function (array $data, bool $isCreate = true, ?int $excludeId = null): array {
    $method = new ReflectionMethod(PerquisitionController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate, $excludeId);
};

check($validate(samplePerquisition(['numero' => 'NEW/001'])) === [], 'valid payload passes');
check(isset($validate(samplePerquisition(['affaire' => '']))['affaire']), 'missing affaire rejected');
check(isset($validate(samplePerquisition(['numero' => 'CUSTOM/002']))['numero']), 'duplicate numero rejected');
check($validate(samplePerquisition(['numero' => 'CUSTOM/002']), true, $customId) === [], 'numero allowed when editing own record');

// --- Sequence: PEQ format ----------------------------------------------------
echo "PEQ sequence format\n";
$peek = PlainteSequence::peekNumber(PlainteSequence::PERQUISITION_KEY, 26);
check($peek === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/PEQ/CSP/TRIMO/26', "peek returns expected format (got $peek)");
$next = PlainteSequence::nextNumber(PlainteSequence::PERQUISITION_KEY, 26);
check($next === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/PEQ/CSP/TRIMO/26', "nextNumber consumes and returns expected (got $next)");
$next2 = PlainteSequence::nextNumber(PlainteSequence::PERQUISITION_KEY, 26);
check($next2 === 'N°002/MSP/SG/DGPN/DGA/DRSP.1/PEQ/CSP/TRIMO/26', "second call increments (got $next2)");

// --- Perquisition attachments ------------------------------------------------
echo "Perquisition Attachments\n";
$peqId = Perquisition::create(samplePerquisition(['numero' => 'PEQ/ATT/001']));
$attId = PerquisitionAttachment::create([
    'perquisition_id' => $peqId,
    'title' => 'Ordre de perquisition',
    'filename' => 'ordre.pdf',
    'original_filename' => 'ordre.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 1024,
]);
check(PerquisitionAttachment::find($attId) !== null, 'attachment create + find');
check(PerquisitionAttachment::belongsToPerquisition($attId, $peqId), 'belongsToPerquisition true');
check(count(PerquisitionAttachment::listForPerquisition($peqId)) === 1, 'listForPerquisition returns 1');
PerquisitionAttachment::updateTitle($attId, 'Ordre updated');
check(PerquisitionAttachment::find($attId)['title'] === 'Ordre updated', 'updateTitle');
PerquisitionAttachment::delete($attId);
check(PerquisitionAttachment::find($attId) === null, 'delete attachment');

// --- Teardown ----------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
