<?php

/**
 * Registre d'enquête (Police Judiciaire) model + validation tests.
 *
 * Usage: php api/tests/RegistreEnqueteTest.php
 *
 * Creates a scratch database (opus_test_registre_enquete), applies migration
 * database/065_create_registre_enquete.sql plus a minimal plainte_sequence
 * table, exercises the RegistreEnquete + attachment models plus the
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
$scratch = 'opus_test_registre_enquete';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, im VARCHAR(50) NULL, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL, grade VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
// mouvement_personnel is referenced by Personnel::getById() for status; minimal stub.
$pdo->exec('CREATE TABLE mouvement_personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, personnel_id INT UNSIGNED NULL, type_mouvement VARCHAR(100) NULL, retour VARCHAR(10) DEFAULT "Non", created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE plainte_sequence (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, type_key VARCHAR(50) NOT NULL, year INT NOT NULL, last_number INT NOT NULL DEFAULT 0, UNIQUE KEY uq_seq (type_key, year)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

$pdo->exec("INSERT INTO personnel (im, firstname, lastname, grade) VALUES ('IM001', 'Jean', 'Rabe', 'OPJ')");

$sql = file_get_contents($root . '/database/065_create_registre_enquete.sql');
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

use App\Models\RegistreEnquete;
use App\Models\RegistreEnqueteAttachment;
use App\Models\PlainteSequence;
use App\Controllers\RegistreEnqueteController;

// --- Helpers ----------------------------------------------------------------
function sampleEnquete(array $overrides = []): array
{
    return array_merge([
        'numero' => 'N°001/MSP/SG/DGPN/DGA/DRSP.1/ENQ/CSP/TRIMO/26',
        'date_ouverture' => '2026-09-20',
        'numero_dossier' => 'N°001/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/26',
        'nature_infraction' => 'Vol aggravé',
        'date_lieu_faits' => 'Le 18/09/2026 à Antananarivo',
        'plaignant' => 'Rakoto Jean',
        'mise_en_cause' => 'Inconnu',
        'enqueteur_personnel_id' => 1,
        'opj_personnel_id' => 1,
        'statut' => 'EN_COURS',
        'observations' => 'Enquête ouverte sur ST du parquet',
        'created_by' => null,
    ], $overrides);
}

// --- RegistreEnquete: CRUD ---------------------------------------------------
echo "RegistreEnquete CRUD\n";
$id = RegistreEnquete::create(sampleEnquete());
$row = RegistreEnquete::find($id);
check($row !== null && $row['nature_infraction'] === 'Vol aggravé', 'create + find');
check($row['numero'] === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/ENQ/CSP/TRIMO/26', 'numero persisted');
check($row['statut'] === 'EN_COURS', 'statut persisted');
check($row['enqueteur_nom'] === 'Rabe' && $row['enqueteur_prenoms'] === 'Jean', 'enqueteur personnel join');
check($row['opj_nom'] === 'Rabe', 'opj personnel join');

RegistreEnquete::update($id, sampleEnquete(['statut' => 'TRANSMISE', 'nature_infraction' => 'Vol aggravé modifié']));
$updated = RegistreEnquete::find($id);
check($updated['nature_infraction'] === 'Vol aggravé modifié', 'update nature_infraction');
check($updated['statut'] === 'TRANSMISE', 'update statut');

RegistreEnquete::create(sampleEnquete(['numero' => 'CUSTOM/ENQ/002', 'plaignant' => 'Rabe Paul']));
$customId = RegistreEnquete::all(['search' => 'CUSTOM/ENQ/002'])[0]['id'] ?? null;
$all = RegistreEnquete::all();
check(count($all) >= 2, 'all returns at least 2 records');

$search = RegistreEnquete::all(['search' => 'Rabe Paul']);
check(count($search) >= 1, 'search by plaignant works');

$statutFilter = RegistreEnquete::all(['statut' => 'EN_COURS']);
check(count($statutFilter) >= 1 && $statutFilter[0]['statut'] === 'EN_COURS', 'statut filter works');

RegistreEnquete::delete($id);
check(RegistreEnquete::find($id) === null, 'delete removes the record');

// --- Numero uniqueness ------------------------------------------------------
echo "Numero uniqueness\n";
check(RegistreEnquete::numeroExists('CUSTOM/ENQ/002'), 'numeroExists finds existing');
check(!RegistreEnquete::numeroExists('NONEXISTENT/999'), 'numeroExists false for new');
check(!RegistreEnquete::numeroExists('CUSTOM/ENQ/002', $customId), 'numeroExists excludes the record itself');

// --- Validation --------------------------------------------------------------
echo "RegistreEnquete Validation\n";
$validate = function (array $data, bool $isCreate = true, ?int $excludeId = null): array {
    $method = new ReflectionMethod(RegistreEnqueteController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate, $excludeId);
};

check($validate(sampleEnquete(['numero' => 'NEW/001'])) === [], 'valid payload passes');
check(isset($validate(sampleEnquete(['nature_infraction' => '']))['nature_infraction']), 'missing nature_infraction rejected');
check(isset($validate(sampleEnquete(['date_ouverture' => '']))['date_ouverture']), 'missing date_ouverture rejected');
check(isset($validate(sampleEnquete(['date_ouverture' => 'not-a-date']))['date_ouverture']), 'invalid date_ouverture rejected');
check(isset($validate(sampleEnquete(['enqueteur_personnel_id' => null]))['enqueteur_personnel_id']), 'missing enqueteur rejected');
check(isset($validate(sampleEnquete(['enqueteur_personnel_id' => 999]))['enqueteur_personnel_id']), 'unknown enqueteur rejected');
check(isset($validate(sampleEnquete(['opj_personnel_id' => 999]))['opj_personnel_id']), 'unknown opj rejected');
check(isset($validate(sampleEnquete(['statut' => 'INVALID']))['statut']), 'invalid statut rejected');
check($validate(sampleEnquete(['numero' => 'CUSTOM/ENQ/002']))['numero'] ?? null, 'duplicate numero rejected');
check($validate(sampleEnquete(['numero' => 'CUSTOM/ENQ/002']), true, $customId) === [], 'numero allowed when editing own record');

// --- Sequence: ENQ format ----------------------------------------------------
echo "ENQ sequence format\n";
$peek = PlainteSequence::peekNumber(PlainteSequence::ENQUETE_KEY, 26);
check($peek === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/ENQ/CSP/TRIMO/26', "peek returns expected format (got $peek)");
$next = PlainteSequence::nextNumber(PlainteSequence::ENQUETE_KEY, 26);
check($next === 'N°001/MSP/SG/DGPN/DGA/DRSP.1/ENQ/CSP/TRIMO/26', "nextNumber consumes and returns expected (got $next)");
$next2 = PlainteSequence::nextNumber(PlainteSequence::ENQUETE_KEY, 26);
check($next2 === 'N°002/MSP/SG/DGPN/DGA/DRSP.1/ENQ/CSP/TRIMO/26', "second call increments (got $next2)");

// --- Attachments -------------------------------------------------------------
echo "RegistreEnquete Attachments\n";
$enqId = RegistreEnquete::create(sampleEnquete(['numero' => 'ENQ/ATT/001']));
$attId = RegistreEnqueteAttachment::create([
    'registre_enquete_id' => $enqId,
    'title' => 'PV d\'ouverture',
    'filename' => 'pv.pdf',
    'original_filename' => 'pv.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 1024,
]);
check(RegistreEnqueteAttachment::find($attId) !== null, 'attachment create + find');
check(RegistreEnqueteAttachment::belongsToEnquete($attId, $enqId), 'belongsToEnquete true');
check(count(RegistreEnqueteAttachment::listForEnquete($enqId)) === 1, 'listForEnquete returns 1');
RegistreEnqueteAttachment::updateTitle($attId, 'PV updated');
check(RegistreEnqueteAttachment::find($attId)['title'] === 'PV updated', 'updateTitle');
RegistreEnqueteAttachment::delete($attId);
check(RegistreEnqueteAttachment::find($attId) === null, 'delete attachment');

// --- Teardown ----------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
