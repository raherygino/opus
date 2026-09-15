<?php

/**
 * PersonneRecherchee (Police Judiciaire) model + validation tests.
 *
 * Usage: php api/tests/PersonneRechercheeTest.php
 *
 * Creates a scratch database (opus_test_personne_recherchee), applies
 * migration database/050_create_personne_recherchee.sql, exercises the
 * PersonneRecherchee and PersonneRechercheePhoto models plus the
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
$scratch = 'opus_test_personne_recherchee';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, prenoms VARCHAR(100) NULL, nom VARCHAR(100) NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

foreach (['050_create_personne_recherchee.sql'] as $file) {
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

putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Models\PersonneRecherchee;
use App\Models\PersonneRechercheePhoto;
use App\Controllers\PersonneRechercheeController;

// --- Helpers ----------------------------------------------------------------
function samplePersonne(array $overrides = []): array
{
    return array_merge([
        'nom' => 'Rakoto Jean',
        'adresse' => 'Lot 123 Antananarivo',
        'motif' => 'Recherché pour vol',
        'created_by' => null,
    ], $overrides);
}

// --- PersonneRecherchee: create / read -------------------------------------
echo "PersonneRecherchee CRUD\n";
$data = samplePersonne();
$id = PersonneRecherchee::create($data);
$row = PersonneRecherchee::find($id);
check($row !== null && $row['nom'] === 'Rakoto Jean', 'create + find');
check($row['adresse'] === 'Lot 123 Antananarivo', 'adresse persisted');
check($row['motif'] === 'Recherché pour vol', 'motif persisted');

// --- Update -----------------------------------------------------------------
PersonneRecherchee::update($id, ['nom' => 'Rakoto Jean Updated', 'adresse' => 'Lot 456', 'motif' => 'Updated motif']);
$updated = PersonneRecherchee::find($id);
check($updated['nom'] === 'Rakoto Jean Updated', 'update nom');
check($updated['adresse'] === 'Lot 456', 'update adresse');
check($updated['motif'] === 'Updated motif', 'update motif');

// --- all with filters --------------------------------------------------------
$secondId = PersonneRecherchee::create(samplePersonne(['nom' => 'Rabe Pierre', 'motif' => 'Escroquerie']));

$all = PersonneRecherchee::all();
check(count($all) >= 2, 'all returns at least 2 records');

$searchResults = PersonneRecherchee::all(['search' => 'Rabe']);
check(count($searchResults) >= 1, 'search by nom works');

// --- photo_count in list ----------------------------------------------------
$listWithCount = PersonneRecherchee::all();
check(isset($listWithCount[0]['photo_count']), 'all() includes photo_count');
check((int) $listWithCount[0]['photo_count'] === 0, 'photo_count is 0 for new entry');

// --- Delete -----------------------------------------------------------------
PersonneRecherchee::delete($secondId);
check(PersonneRecherchee::find($secondId) === null, 'delete removes the record');

// --- Validation -------------------------------------------------------------
echo "Validation\n";
$validate = function (array $data, bool $isCreate = true): array {
    $method = new ReflectionMethod(PersonneRechercheeController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate);
};

check($validate(samplePersonne()) === [], 'valid payload passes');
check(isset($validate(samplePersonne(['nom' => '']))['nom']), 'missing nom rejected');
check(isset($validate(samplePersonne(['motif' => '']))['motif']), 'missing motif rejected');

// --- Photos ----------------------------------------------------------------
echo "Photos\n";
$photoId = PersonneRechercheePhoto::create([
    'personne_recherchee_id' => $id,
    'caption' => 'Photo 1',
    'filename' => 'photo_1.jpg',
    'original_filename' => 'img1.jpg',
    'mime_type' => 'image/jpeg',
    'file_size' => 1024,
    'width' => 800,
    'height' => 600,
    'capture_source' => 'CAMERA',
    'sort_order' => 0,
]);
$photo = PersonneRechercheePhoto::find($photoId);
check($photo !== null && $photo['caption'] === 'Photo 1', 'photo create + find');
check($photo['capture_source'] === 'CAMERA', 'capture_source persisted');
check((int) $photo['width'] === 800, 'width persisted');

check(PersonneRechercheePhoto::belongsToPersonne($photoId, $id), 'belongsToPersonne true for own');
check(!PersonneRechercheePhoto::belongsToPersonne($photoId, 999999), 'belongsToPersonne false for other');

$photos = PersonneRechercheePhoto::listForPersonne($id);
check(count($photos) === 1, 'listForPersonne returns the photo');

// Sort order increments
$nextSort = PersonneRechercheePhoto::nextSortOrder($id);
check($nextSort === 1, 'nextSortOrder returns 1 after one photo');

$photoId2 = PersonneRechercheePhoto::create([
    'personne_recherchee_id' => $id,
    'caption' => 'Photo 2',
    'filename' => 'photo_2.png',
    'original_filename' => 'img2.png',
    'mime_type' => 'image/png',
    'file_size' => 2048,
    'capture_source' => 'GALLERY',
    'sort_order' => $nextSort,
]);
$photos = PersonneRechercheePhoto::listForPersonne($id);
check(count($photos) === 2, 'listForPersonne returns 2 photos after second create');
check((int) $photos[0]['sort_order'] === 0 && (int) $photos[1]['sort_order'] === 1, 'photos sorted by sort_order');

// Update caption
PersonneRechercheePhoto::updateCaption($photoId, 'Updated caption');
check(PersonneRechercheePhoto::find($photoId)['caption'] === 'Updated caption', 'updateCaption');

// Update sort order
PersonneRechercheePhoto::updateSortOrder($photoId, 5);
check((int) PersonneRechercheePhoto::find($photoId)['sort_order'] === 5, 'updateSortOrder');

// Delete photo
PersonneRechercheePhoto::delete($photoId);
check(PersonneRechercheePhoto::find($photoId) === null, 'delete photo');
check(count(PersonneRechercheePhoto::listForPersonne($id)) === 1, 'photo list has 1 after delete');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
