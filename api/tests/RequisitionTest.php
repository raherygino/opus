<?php

/**
 * Requisition (Police Judiciaire) model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/RequisitionTest.php
 *
 * Creates a scratch database (opus_test_requisition), applies migration
 * database/049_create_requisition.sql plus the plainte_sequence table,
 * exercises the Requisition, PlainteSequence (REQ) and attachment models
 * plus the controller validation rules, then drops the scratch database.
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
$scratch = 'opus_test_requisition';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FKs to users.
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(50) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL, grade VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

foreach (['046_create_plainte_sequence.sql', '049_create_requisition.sql'] as $file) {
    $sql = file_get_contents($root . '/database/' . $file);
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

use App\Models\Requisition;
use App\Models\RequisitionAttachment;
use App\Models\PlainteSequence;
use App\Controllers\RequisitionController;

// --- Helpers ----------------------------------------------------------------
function sampleRequisition(array $overrides = []): array
{
    return array_merge([
        'type' => 'TPH',
        'date_requisition' => '2026-09-20',
        'numero_ttr' => 'N°001/TRIMO/26',
        'nom_substitut' => 'Substitut Rabe',
        'affaire' => 'Affaire vol rue 123',
        'numero_dossier' => 'N°DOS/001/26',
        'opj' => 'OPJ Rakoto',
        'created_by' => null,
    ], $overrides);
}

// --- PlainteSequence: REQ number generation -----------------------------------
echo "PlainteSequence (REQ)\n";
$yy = (int) date('y');
$n1 = PlainteSequence::nextNumber('REQ');
$n2 = PlainteSequence::nextNumber('REQ');
check($n1 === "N°001/MSP/SG/DGPN/DRSP.1/REQ/CSP/A-TRIMO/$yy", "first REQ number correct (got $n1)");
check($n2 === "N°002/MSP/SG/DGPN/DRSP.1/REQ/CSP/A-TRIMO/$yy", "second REQ increments (got $n2)");

// Peek without consuming.
$peek = PlainteSequence::peekNumber('REQ');
check($peek === "N°003/MSP/SG/DGPN/DRSP.1/REQ/CSP/A-TRIMO/$yy", "peek REQ shows next without consuming (got $peek)");

// --- Requisition: create / read --------------------------------------------
echo "Requisition CRUD\n";
$data = sampleRequisition();
$data['numero'] = PlainteSequence::nextNumber('REQ');
$id = Requisition::create($data);
$row = Requisition::find($id);
check($row !== null && $row['type'] === 'TPH', 'create + find TPH');
check($row['numero'] === $data['numero'], 'numero persisted');
check($row['affaire'] === 'Affaire vol rue 123', 'affaire persisted');
check($row['nom_substitut'] === 'Substitut Rabe', 'nom_substitut persisted');

// --- Update -----------------------------------------------------------------
Requisition::update($id, ['type' => 'CIM', 'date_requisition' => '2026-09-21', 'numero' => $data['numero'], 'affaire' => 'Updated affaire']);
$updated = Requisition::find($id);
check($updated['type'] === 'CIM', 'update type');
check($updated['affaire'] === 'Updated affaire', 'update affaire');
check($updated['numero'] === $data['numero'], 'numero unchanged on update');

// --- all with filters --------------------------------------------------------
$autreData = sampleRequisition(['type' => 'AUTRE', 'affaire' => 'Other case']);
$autreData['numero'] = PlainteSequence::nextNumber('REQ');
$autreId = Requisition::create($autreData);

$all = Requisition::all();
check(count($all) >= 2, 'all returns at least 2 records');

$tphOnly = Requisition::all(['type' => 'CIM']);
check(count($tphOnly) >= 1 && $tphOnly[0]['type'] === 'CIM', 'filter by type CIM (updated from TPH)');

$searchResults = Requisition::all(['search' => 'Updated affaire']);
check(count($searchResults) >= 1, 'search by affaire works');

// --- numeroExists ------------------------------------------------------------
check(Requisition::numeroExists($data['numero']), 'numeroExists finds existing numero');
check(!Requisition::numeroExists('NONEXISTENT-NUMERO-999'), 'numeroExists returns false for unknown');
check(!Requisition::numeroExists($data['numero'], $id), 'numeroExists excludes self when excludeId provided');

// --- Delete -----------------------------------------------------------------
Requisition::delete($id);
check(Requisition::find($id) === null, 'delete removes the record');

// --- Validation -------------------------------------------------------------
echo "Validation\n";
$validate = function (array $data, bool $isCreate = true, ?int $excludeId = null): array {
    $method = new ReflectionMethod(RequisitionController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate, $excludeId);
};

check($validate(sampleRequisition()) === [], 'valid TPH payload passes');
check($validate(sampleRequisition(['type' => 'MEDECIN_LEGISTE'])) === [], 'valid MEDECIN_LEGISTE payload passes');
check($validate(sampleRequisition(['type' => 'CIM'])) === [], 'valid CIM payload passes');
check($validate(sampleRequisition(['type' => 'AUTRE'])) === [], 'valid AUTRE payload passes');

check(isset($validate(sampleRequisition(['type' => '']))['type']), 'missing type rejected');
check(isset($validate(sampleRequisition(['type' => 'NOPE']))['type']), 'invalid type rejected');
check(isset($validate(sampleRequisition(['date_requisition' => '']))['date_requisition']), 'missing date rejected');
check(isset($validate(sampleRequisition(['date_requisition' => '13/09/2026']))['date_requisition']), 'invalid date format rejected');
check(isset($validate(sampleRequisition(['affaire' => '']))['affaire']), 'missing affaire rejected');

// --- Editable numero (user-provided) ------------------------------------------
echo "Editable numero\n";
$custom = sampleRequisition(['type' => 'CIM']);
$custom['numero'] = 'N°099/MSP/SG/DGPN/DRSP.1/REQ/CSP/A-TRIMO/' . $yy;
$customId = Requisition::create($custom);
$customRow = Requisition::find($customId);
check($customRow['numero'] === 'N°099/MSP/SG/DGPN/DRSP.1/REQ/CSP/A-TRIMO/' . $yy, 'user-provided numero persisted on create');

// Duplicate user-provided numero is rejected by validation.
$dupErrors = $validate(sampleRequisition([
    'type' => 'CIM',
    'numero' => 'N°099/MSP/SG/DGPN/DRSP.1/REQ/CSP/A-TRIMO/' . $yy,
]));
check(isset($dupErrors['numero']), 'duplicate user-provided numero rejected');

// Empty numero on create is allowed (server will auto-generate).
$autoErrors = $validate(sampleRequisition(['numero' => '']));
check(!isset($autoErrors['numero']), 'empty numero on create is valid (auto-generated)');

// --- Attachments ------------------------------------------------------------
echo "Attachments\n";
$attId = RequisitionAttachment::create([
    'requisition_id'    => $customId,
    'title'             => 'Test doc',
    'filename'          => 'test_doc.txt',
    'original_filename' => 'doc.txt',
    'mime_type'         => 'text/plain',
    'file_size'         => 100,
]);
$att = RequisitionAttachment::find($attId);
check($att !== null && $att['title'] === 'Test doc', 'attachment create + find');
check(RequisitionAttachment::belongsToRequisition($attId, $customId), 'belongsToRequisition true for own');
check(!RequisitionAttachment::belongsToRequisition($attId, 999999), 'belongsToRequisition false for other requisition');

$atts = RequisitionAttachment::listForRequisition($customId);
check(count($atts) === 1 && $atts[0]['id'] === $attId, 'listForRequisition returns the attachment');

RequisitionAttachment::updateTitle($attId, 'Updated title');
check(RequisitionAttachment::find($attId)['title'] === 'Updated title', 'updateTitle');

RequisitionAttachment::delete($attId);
check(RequisitionAttachment::find($attId) === null, 'delete attachment');
check(count(RequisitionAttachment::listForRequisition($customId)) === 0, 'attachment list empty after delete');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
