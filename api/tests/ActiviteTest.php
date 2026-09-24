<?php

/**
 * Activite model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/ActiviteTest.php
 *
 * Creates a scratch database (opus_test_activite), applies migrations
 * database/062_create_activite.sql and 063_create_attach_activite.sql,
 * exercises the Activite model plus the controller validation rules,
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
$scratch = 'opus_test_activite';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FKs and the model's joins
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

foreach (['062_create_activite.sql', '063_create_attach_activite.sql'] as $migration) {
    $sql = file_get_contents($root . '/database/' . $migration);
    foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
        if (preg_match('/^\s*(CREATE|ALTER|UPDATE|INSERT|DROP)/i', preg_replace('/^--.*$/m', '', $stmt))) {
            $pdo->exec($stmt);
        }
    }
}

// Point the app's Database singleton at the scratch DB
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\Activite;
use App\Models\ActiviteAttachment;

function sampleRow(array $overrides = []): array
{
    return array_merge([
        'date_activite' => '2026-09-22',
        'heure_activite' => '08:30',
        'patrouille_diurne_motorisee_itineraire' => 'PK3 → Marché central → Gare',
        'patrouille_diurne_pedestre_itineraire' => null,
        'patrouille_diurne_portee_itineraire' => null,
        'patrouille_nocturne_motorisee_itineraire' => null,
        'patrouille_nocturne_pedestre_itineraire' => null,
        'patrouille_nocturne_portee_itineraire' => 'Quartier administratif',
        'operation_ciblee' => 'Contrôle CIN',
        'faits_constates' => 'RAS dans le secteur nord',
        'compte_rendu_hierarchie' => 'Patrouille en cours, RAS à 08h45',
        'conduite_a_tenir' => 'Poursuivre la patrouille jusqu\'à 10h',
        'nature_intervention' => 'Tapage nocturne',
        'suites_donnees' => 'Conduite au Poste pour examen de situation',
        'created_by' => null,
    ], $overrides);
}

// --- Migration structure ------------------------------------------------------
echo "Migration\n";
$tables = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
check(in_array('activite', $tables, true), 'activite table created');
check(in_array('attach_activite', $tables, true), 'attach_activite table created');
$columns = $pdo->query('SHOW COLUMNS FROM activite')->fetchAll(PDO::FETCH_COLUMN);
foreach ([
    'date_activite', 'heure_activite',
    'patrouille_diurne_motorisee_itineraire', 'patrouille_nocturne_portee_itineraire',
    'operation_ciblee', 'faits_constates', 'compte_rendu_hierarchie',
    'conduite_a_tenir', 'nature_intervention', 'suites_donnees',
    'latitude', 'longitude', 'created_by',
] as $col) {
    check(in_array($col, $columns, true), "activite.$col column exists");
}

// --- Model: create / read ------------------------------------------------------
echo "Model CRUD\n";
$id = Activite::create(sampleRow(['latitude' => 14.7166770, 'longitude' => -17.4676860]));
$row = Activite::find($id);
check($row !== null && $row['operation_ciblee'] === 'Contrôle CIN', 'create + find');
check($row['patrouille_diurne_motorisee_itineraire'] === 'PK3 → Marché central → Gare', 'diurne motorisée itinerary persisted');
check($row['patrouille_nocturne_portee_itineraire'] === 'Quartier administratif', 'nocturne portée itinerary persisted');
check($row['patrouille_diurne_pedestre_itineraire'] === null, 'unselected mode stays NULL');
check($row['faits_constates'] === 'RAS dans le secteur nord', 'faits_constates persisted');
check($row['compte_rendu_hierarchie'] !== '' && $row['conduite_a_tenir'] !== '', 'hiérarchie fields persisted');
check($row['nature_intervention'] === 'Tapage nocturne', 'nature_intervention persisted');
check($row['suites_donnees'] === 'Conduite au Poste pour examen de situation', 'suites_donnees persisted');
check(abs((float) $row['latitude'] - 14.716677) < 0.000001, 'latitude persisted');
check(abs((float) $row['longitude'] - (-17.467686)) < 0.000001, 'longitude persisted');

// Selected-but-empty itinerary is stored as '' (distinct from NULL).
$idEmpty = Activite::create(sampleRow([
    'patrouille_diurne_motorisee_itineraire' => '',
    'patrouille_nocturne_portee_itineraire' => null,
]));
$rowEmpty = Activite::find($idEmpty);
check($rowEmpty['patrouille_diurne_motorisee_itineraire'] === '', 'selected mode without itinerary stored as empty string');
check($rowEmpty['patrouille_nocturne_portee_itineraire'] === null, 'unselected mode stays NULL on second row');

check(Activite::update($id, sampleRow(['operation_ciblee' => 'Contrôle débit de boissons', 'suites_donnees' => 'RAS'])), 'update');
$row = Activite::find($id);
check($row['operation_ciblee'] === 'Contrôle débit de boissons' && $row['suites_donnees'] === 'RAS', 'fields updated');

check(count(Activite::all(['search' => 'boissons'])) === 1, 'search filter on operation_ciblee');
check(count(Activite::all(['search' => 'tapage'])) === 2, 'search filter on nature_intervention');
check(count(Activite::all(['search' => 'inexistant'])) === 0, 'search filter no match');

Activite::delete($id);
check(Activite::find($id) === null, 'delete');

// --- Attachments ---------------------------------------------------------------
echo "Attachments\n";
$attachId = ActiviteAttachment::create([
    'activite_id'       => $idEmpty,
    'title'             => 'Photo de la patrouille',
    'filename'          => 'photo_patrouille_' . uniqid() . '.jpg',
    'original_filename' => 'photo patrouille.jpg',
    'mime_type'         => 'image/jpeg',
    'file_size'         => 12345,
]);
$att = ActiviteAttachment::getById($attachId);
check($att !== null && $att['title'] === 'Photo de la patrouille', 'attachment create + getById');
check((int) $att['file_size'] === 12345, 'attachment file_size persisted');
check(count(ActiviteAttachment::getByActiviteId($idEmpty)) === 1, 'getByActiviteId');
check(ActiviteAttachment::belongsToActivite($attachId, $idEmpty), 'belongsToActivite true');
check(!ActiviteAttachment::belongsToActivite($attachId, $idEmpty + 999), 'belongsToActivite false for other activité');
check(ActiviteAttachment::update($attachId, ['title' => 'Constat photos']), 'attachment title update');
check(ActiviteAttachment::getById($attachId)['title'] === 'Constat photos', 'attachment title updated');
check(ActiviteAttachment::delete($attachId), 'attachment delete');
check(ActiviteAttachment::getById($attachId) === null, 'attachment gone after delete');

// Cascade: deleting the parent removes attachments (ON DELETE CASCADE).
$attachId2 = ActiviteAttachment::create([
    'activite_id'       => $idEmpty,
    'title'             => 'Rapport annexé',
    'filename'          => 'rapport_' . uniqid() . '.pdf',
    'original_filename' => 'rapport.pdf',
]);
Activite::delete($idEmpty);
check(ActiviteAttachment::getById($attachId2) === null, 'attachments cascade-deleted with parent');

// --- Controller validation rules (private static, via reflection) --------------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\ActiviteController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true): array => $method->invoke(null, $data, $isCreate);

check($validate(sampleRow()) === [], 'valid payload passes');
check(isset($validate(sampleRow(['date_activite' => '']))['date_activite']), 'missing date rejected');
check(isset($validate(sampleRow(['heure_activite' => '']))['heure_activite']), 'missing heure rejected');
check($validate(sampleRow(['patrouille_diurne_motorisee_itineraire' => null])) === [], 'null itinerary accepted');
check($validate(sampleRow(['patrouille_diurne_motorisee_itineraire' => ''])) === [], 'empty itinerary accepted');
check(isset($validate(sampleRow(['patrouille_diurne_motorisee_itineraire' => 42]))['patrouille_diurne_motorisee_itineraire']), 'non-string itinerary rejected');
check($validate(['suites_donnees' => 'Interpellation'], false) === [], 'partial update payload passes');
check($validate([], false) === [], 'empty update payload passes');
check($validate(sampleRow(['latitude' => '14.716677', 'longitude' => '-17.467686'])) === [], 'numeric lat/lon accepted');
check(isset($validate(sampleRow(['latitude' => 'abc']))['latitude']), 'non-numeric latitude rejected');
check(isset($validate(sampleRow(['longitude' => 'abc']))['longitude']), 'non-numeric longitude rejected');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
