<?php

/**
 * Mandat (Police Judiciaire) model + validation tests.
 *
 * Usage: php api/tests/MandatTest.php
 *
 * Creates a scratch database (opus_test_mandat), applies migration
 * database/054_create_mandat.sql plus a minimal plainte_sequence
 * table, exercises the Mandat + attachment models plus the
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
$scratch = 'opus_test_mandat';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, prenoms VARCHAR(100) NULL, nom VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE plainte_sequence (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, type_key VARCHAR(50) NOT NULL, year INT NOT NULL, last_number INT NOT NULL DEFAULT 0, UNIQUE KEY uq_seq (type_key, year)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$sql = file_get_contents($root . '/database/054_create_mandat.sql');
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

use App\Models\Mandat;
use App\Models\MandatAttachment;
use App\Models\PlainteSequence;
use App\Controllers\MandatController;

// --- Helpers ----------------------------------------------------------------
function sampleMandat(array $overrides = []): array
{
    return array_merge([
        'numero' => 'N°001/MSP/SG/DGPN/DGA/DRSP.1/MAN/CSP/TRIMO/26',
        'type' => 'AMENER',
        'autorite' => 'Procureur de la République',
        'personne_nom' => 'RAKOTO Jean',
        'date_lieu_naissance' => '15/03/1990, Antananarivo',
        'motif' => 'Présentation devant le procureur',
        'qualification_infraction' => 'Vol aggravé',
        'opj_execution' => 'OPJ Rabe Michel',
        'date_heure_execution' => '2026-01-15 10:00:00',
        'lieu_execution' => 'Commissariat CSP',
        'observations' => 'Aucune',
        'created_by' => null,
    ], $overrides);
}

// --- Mandat: CRUD ------------------------------------------------------------
echo "Mandat CRUD\n";
$id = Mandat::create(sampleMandat());
$row = Mandat::find($id);
check($row !== null && $row['personne_nom'] === 'RAKOTO Jean', 'create + find');
check($row['numero'] === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/MAN/CSP/TRIMO/26', 'numero persisted');
check($row['type'] === 'AMENER', 'type persisted');
check($row['opj_execution'] === 'OPJ Rabe Michel', 'opj_execution persisted');
check($row['date_heure_execution'] === '2026-01-15 10:00:00', 'date_heure_execution persisted');

Mandat::update($id, sampleMandat(['type' => 'ARRET', 'motif' => 'Interpellation']));
$updated = Mandat::find($id);
check($updated['type'] === 'ARRET', 'update type');
check($updated['motif'] === 'Interpellation', 'update motif');

Mandat::create(sampleMandat(['numero' => 'MAN/002', 'type' => 'COMPARUTION', 'personne_nom' => 'RASOA Marie']));
$all = Mandat::all();
check(count($all) >= 2, 'all returns at least 2 records');

$filtered = Mandat::all(['type' => 'COMPARUTION']);
check(count($filtered) === 1, 'filter by type works');

$search = Mandat::all(['search' => 'RASOA']);
check(count($search) >= 1, 'search by personne_nom works');

Mandat::delete($id);
check(Mandat::find($id) === null, 'delete removes the record');

// --- Numero uniqueness --------------------------------------------------------
echo "Numero uniqueness\n";
$customId = Mandat::all(['search' => 'MAN/002'])[0]['id'] ?? null;
check(Mandat::numeroExists('MAN/002'), 'numeroExists finds existing');
check(!Mandat::numeroExists('NONEXISTENT/999'), 'numeroExists false for new');
check(!Mandat::numeroExists('MAN/002', $customId), 'numeroExists excludes the record itself');

// --- Validation ----------------------------------------------------------------
echo "Mandat Validation\n";
$validate = function (array $data, bool $isCreate = true, ?int $excludeId = null): array {
    $method = new ReflectionMethod(MandatController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate, $excludeId);
};

check($validate(sampleMandat(['numero' => 'NEW/001'])) === [], 'valid payload passes');
check(isset($validate(sampleMandat(['type' => '']))['type']), 'missing type rejected');
check(isset($validate(sampleMandat(['type' => 'INVALID']))['type']), 'invalid type rejected');
check(isset($validate(sampleMandat(['personne_nom' => '']))['personne_nom']), 'missing personne_nom rejected');
check(isset($validate(sampleMandat(['date_heure_execution' => 'not-a-date']))['date_heure_execution']), 'invalid date_heure_execution rejected');
check($validate(sampleMandat(['date_heure_execution' => null])) === [], 'null date_heure_execution passes');
check(isset($validate(sampleMandat(['numero' => 'MAN/002']))['numero']), 'duplicate numero rejected');
check($validate(sampleMandat(['numero' => 'MAN/002']), false, $customId) === [], 'numero allowed when editing own record');
check($validate(sampleMandat(['type' => 'DEPOT', 'numero' => 'NEW/002'])) === [], 'DEPOT type valid');

// --- Sequence: MAN format -------------------------------------------------------
echo "MAN sequence format\n";
$peek = PlainteSequence::peekNumber(PlainteSequence::MANDAT_KEY, 26);
check($peek === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/MAN/CSP/TRIMO/26', "peek returns expected format (got $peek)");
$next = PlainteSequence::nextNumber(PlainteSequence::MANDAT_KEY, 26);
check($next === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/MAN/CSP/TRIMO/26', "nextNumber consumes and returns expected (got $next)");
$next2 = PlainteSequence::nextNumber(PlainteSequence::MANDAT_KEY, 26);
check($next2 === 'N°002/MSP/SG/DGPN/DGA/DRSP.1/MAN/CSP/TRIMO/26', "second call increments (got $next2)");

// --- Mandat attachments ---------------------------------------------------------
echo "Mandat Attachments\n";
$mandatId = Mandat::create(sampleMandat(['numero' => 'MAN/ATT/001']));
$attId = MandatAttachment::create([
    'mandat_id' => $mandatId,
    'title' => 'Mandat original',
    'filename' => 'mandat.pdf',
    'original_filename' => 'mandat.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 2048,
]);
check(MandatAttachment::find($attId) !== null, 'attachment create + find');
check(MandatAttachment::belongsToMandat($attId, $mandatId), 'belongsToMandat true');
check(!MandatAttachment::belongsToMandat($attId, 999999), 'belongsToMandat false for other');
check(count(MandatAttachment::listForMandat($mandatId)) === 1, 'listForMandat returns 1');
MandatAttachment::updateTitle($attId, 'Mandat updated');
check(MandatAttachment::find($attId)['title'] === 'Mandat updated', 'updateTitle');
MandatAttachment::delete($attId);
check(MandatAttachment::find($attId) === null, 'delete attachment');

// --- Teardown ----------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
