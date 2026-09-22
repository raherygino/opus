<?php

/**
 * EvenementSurvenu model + validation tests (no PHPUnit required).
 *
 * Usage: php api/tests/EvenementSurvenuTest.php
 *
 * Creates a scratch database (opus_test_evenement), applies migration
 * database/058_create_evenement_survenu.sql, exercises the EvenementSurvenu
 * model plus the controller validation rules, then drops the scratch
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
$scratch = 'opus_test_evenement';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// Minimal prerequisites for the FKs and the model's joins
$pdo->exec('CREATE TABLE personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, firstname VARCHAR(100) NULL, lastname VARCHAR(100) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

foreach (['058_create_evenement_survenu.sql', '059_create_attach_evenement_survenu.sql', '060_add_evenement_survenu_location.sql', '061_create_evenement_survenu_type.sql'] as $migration) {
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

use App\Models\EvenementSurvenu;
use App\Models\EvenementSurvenuAttachment;

function sampleRow(array $overrides = []): array
{
    return array_merge([
        'date_evenement' => '2026-09-20',
        'heure_evenement' => '14:30',
        'type_evenement' => 'Accident',
        'lieu_exact' => 'Avenue Bourguiba x Rue 10',
        'auteurs_presumes' => 'Conducteur véhicule immatriculé DK-1234-AB',
        'victimes' => 'M. Fall (piéton)',
        'temoins' => 'Mme Ndiaye, M. Sarr',
        'mesures_prises' => 'Victime évacuée, constat dressé',
        'created_by' => null,
    ], $overrides);
}

// --- Migration structure ------------------------------------------------------
echo "Migration\n";
$tables = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
check(in_array('evenement_survenu', $tables, true), 'evenement_survenu table created');
check(in_array('attach_evenement_survenu', $tables, true), 'attach_evenement_survenu table created');
$columns = $pdo->query('SHOW COLUMNS FROM evenement_survenu')->fetchAll(PDO::FETCH_COLUMN);
foreach (['date_evenement', 'heure_evenement', 'type_evenement', 'lieu_exact', 'auteurs_presumes', 'victimes', 'temoins', 'mesures_prises'] as $col) {
    check(in_array($col, $columns, true), "evenement_survenu.$col column exists");
}

// --- Model: create / read ------------------------------------------------------
echo "Model CRUD\n";
$id = EvenementSurvenu::create(sampleRow(['latitude' => 14.7166770, 'longitude' => -17.4676860]));
$row = EvenementSurvenu::find($id);
check($row !== null && $row['lieu_exact'] === 'Avenue Bourguiba x Rue 10', 'create + find');
check($row['type_evenement'] === 'Accident', 'type_evenement persisted');
check($row['auteurs_presumes'] !== '' && $row['victimes'] !== '' && $row['temoins'] !== '', 'parties impliquées persisted');
check($row['mesures_prises'] === 'Victime évacuée, constat dressé', 'mesures_prises persisted');
check(abs((float) $row['latitude'] - 14.716677) < 0.000001, 'latitude persisted');
check(abs((float) $row['longitude'] - (-17.467686)) < 0.000001, 'longitude persisted');

$idDefaults = EvenementSurvenu::create(sampleRow(['auteurs_presumes' => null, 'victimes' => null, 'temoins' => null, 'mesures_prises' => null]));
$rowDefaults = EvenementSurvenu::find($idDefaults);
check($rowDefaults['auteurs_presumes'] === null && $rowDefaults['victimes'] === null, 'nullable party fields default to null');

check(EvenementSurvenu::update($id, sampleRow(['lieu_exact' => 'Place de l\'Indépendance', 'mesures_prises' => 'Chaussée sécurisée'])), 'update');
$row = EvenementSurvenu::find($id);
check($row['lieu_exact'] === 'Place de l\'Indépendance' && $row['mesures_prises'] === 'Chaussée sécurisée', 'fields updated');

check(count(EvenementSurvenu::all(['search' => 'Bourguiba'])) === 1, 'search filter on lieu');
check(count(EvenementSurvenu::all(['search' => 'Ndiaye'])) === 1, 'search filter on témoins');
check(count(EvenementSurvenu::all(['type' => 'Accident'])) === 2, 'type filter');
check(count(EvenementSurvenu::all(['type' => 'infraction'])) === 0, 'type filter no match');

EvenementSurvenu::delete($id);
check(EvenementSurvenu::find($id) === null, 'delete');

// --- Attachments ---------------------------------------------------------------
echo "Attachments\n";
$attachId = EvenementSurvenuAttachment::create([
    'evenement_id'      => $idDefaults,
    'title'             => 'Photo de la scène',
    'filename'          => 'photo_scene_' . uniqid() . '.jpg',
    'original_filename' => 'photo scene.jpg',
    'mime_type'         => 'image/jpeg',
    'file_size'         => 12345,
]);
$att = EvenementSurvenuAttachment::getById($attachId);
check($att !== null && $att['title'] === 'Photo de la scène', 'attachment create + getById');
check((int) $att['file_size'] === 12345, 'attachment file_size persisted');
check(count(EvenementSurvenuAttachment::getByEvenementId($idDefaults)) === 1, 'getByEvenementId');
check(EvenementSurvenuAttachment::belongsToEvenement($attachId, $idDefaults), 'belongsToEvenement true');
check(!EvenementSurvenuAttachment::belongsToEvenement($attachId, $idDefaults + 999), 'belongsToEvenement false for other event');
check(EvenementSurvenuAttachment::update($attachId, ['title' => 'Constat photos']), 'attachment title update');
check(EvenementSurvenuAttachment::getById($attachId)['title'] === 'Constat photos', 'attachment title updated');
check(EvenementSurvenuAttachment::delete($attachId), 'attachment delete');
check(EvenementSurvenuAttachment::getById($attachId) === null, 'attachment gone after delete');

// Cascade: deleting the parent removes attachments (ON DELETE CASCADE).
$attachId2 = EvenementSurvenuAttachment::create([
    'evenement_id'      => $idDefaults,
    'title'             => 'Rapport annexé',
    'filename'          => 'rapport_' . uniqid() . '.pdf',
    'original_filename' => 'rapport.pdf',
]);
EvenementSurvenu::delete($idDefaults);
check(EvenementSurvenuAttachment::getById($attachId2) === null, 'attachments cascade-deleted with parent');

// --- Type catalog (evenement_survenu_type) ------------------------------------
echo "Type catalog\n";
$seedLabels = \App\Models\EvenementSurvenuType::labels();
check(in_array('Infraction', $seedLabels, true) && in_array('Accident', $seedLabels, true)
    && in_array('Incident', $seedLabels, true) && in_array('Autre', $seedLabels, true), 'seeded default types');

$typeId = \App\Models\EvenementSurvenuType::create('Embouteillage');
check(\App\Models\EvenementSurvenuType::exists('Embouteillage'), 'type create + exists');
check(\App\Models\EvenementSurvenuType::getById($typeId)['label'] === 'Embouteillage', 'type getById');
check(\App\Models\EvenementSurvenuType::update($typeId, 'Embouteillage majeur'), 'type update');
check(\App\Models\EvenementSurvenuType::getById($typeId)['label'] === 'Embouteillage majeur', 'type label updated');

// An event can be created with a custom catalog type (label stored verbatim).
$idCustom = EvenementSurvenu::create(sampleRow(['type_evenement' => 'Embouteillage majeur']));
check(EvenementSurvenu::find($idCustom)['type_evenement'] === 'Embouteillage majeur', 'event stores custom type label');

// Renaming/deleting a type must not break existing events (no FK).
check(\App\Models\EvenementSurvenuType::update($typeId, 'Embouteillage'), 'type rename back');
check(EvenementSurvenu::find($idCustom)['type_evenement'] === 'Embouteillage majeur', 'existing event keeps label after rename');
check(\App\Models\EvenementSurvenuType::delete($typeId), 'type delete');
check(!\App\Models\EvenementSurvenuType::exists('Embouteillage'), 'type gone after delete');
check(EvenementSurvenu::find($idCustom)['type_evenement'] === 'Embouteillage majeur', 'existing event keeps label after delete');

// --- Controller validation rules (private static, via reflection) --------------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\EvenementSurvenuController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true): array => $method->invoke(null, $data, $isCreate);

check($validate(sampleRow()) === [], 'valid payload passes');
check(isset($validate(sampleRow(['date_evenement' => '']))['date_evenement']), 'missing date rejected');
check(isset($validate(sampleRow(['heure_evenement' => '']))['heure_evenement']), 'missing heure rejected');
check(isset($validate(sampleRow(['type_evenement' => '']))['type_evenement']), 'missing type rejected');
check(isset($validate(sampleRow(['type_evenement' => 'vol']))['type_evenement']), 'unknown type rejected');
check(isset($validate(sampleRow(['type_evenement' => 'Embouteillage majeur']))['type_evenement']), 'deleted catalog type rejected');
check(isset($validate(sampleRow(['lieu_exact' => ' ']))['lieu_exact']), 'blank lieu rejected');
check($validate(['mesures_prises' => 'Patrouille renforcée'], false) === [], 'partial update payload passes');
check($validate([], false) === [], 'empty update payload passes');
check($validate(sampleRow(['latitude' => '14.716677', 'longitude' => '-17.467686'])) === [], 'numeric lat/lon accepted');
check(isset($validate(sampleRow(['latitude' => 'abc']))['latitude']), 'non-numeric latitude rejected');
check(isset($validate(sampleRow(['longitude' => 'abc']))['longitude']), 'non-numeric longitude rejected');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
