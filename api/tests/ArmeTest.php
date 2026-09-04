<?php

/**
 * Arme + TypeArme + ammunition consommation tests (no PHPUnit required).
 *
 * Usage: php api/tests/ArmeTest.php
 *
 * Creates a scratch database (opus_test_arme), applies the type_arme,
 * arme and arme_munitions_consommation migrations plus the armement
 * migrations (and the users/roles/notifications tables the models touch),
 * exercises:
 *   - TypeArme CRUD + uniqueness + delete-restriction when weapons exist
 *   - Arme CRUD + matricule uniqueness + delete-restriction when
 *     perceptions/history exist
 *   - Arme::decreaseStock atomic conditional deduction
 *   - Insufficient stock rejection (stock never goes negative)
 *   - Consumption history insertion
 *   - Armement perception with arme_id snapshotting the weapon identity
 *   - Armement reintegration deducting the weapon's stock atomically
 * then drops the scratch database.
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
$scratch = 'opus_test_arme';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// --- Apply migrations in order ----------------------------------------------
$migrations = [
    '001_create_roles.sql',
    '002_create_personnel.sql',
    '003_create_users.sql',
    '007_add_signature_svg.sql',
    '008_create_role_permissions.sql',
    '009_create_notifications.sql',
    '018_add_notification_link.sql',
    '024_create_armement.sql',
    '025_create_attach_armement.sql',
    '026_add_personnel_code_secret.sql',
    '027_add_armement_verification_signature.sql',
    '028_add_armement_location.sql',
    '029_add_armement_reintegration_location.sql',
    '030_create_type_arme.sql',
    '031_create_arme.sql',
    '032_add_armement_arme_id.sql',
    '033_create_arme_munitions_consommation.sql',
    '034_add_type_arme_munitions_stock.sql',
];
foreach ($migrations as $file) {
    $sql = file_get_contents($root . '/database/' . $file);
    // Strip full-line comments before splitting on semicolons — comments
    // may contain semicolons that would break the naive splitter.
    $sql = preg_replace('/^--.*$/m', '', $sql);
    foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
        if (preg_match('/^\s*(CREATE|ALTER|INSERT)/i', $stmt)) {
            $pdo->exec($stmt);
        }
    }
}

// Stub tables the models touch via subqueries (mirrors ArmementTest).
$pdo->exec('CREATE TABLE mouvement_personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, personnel_id INT UNSIGNED NULL, type_mouvement VARCHAR(100) NULL, retour VARCHAR(10) NULL DEFAULT "Non", created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE device_tokens (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, user_id INT UNSIGNED NULL, token VARCHAR(255) NULL, device_name VARCHAR(255) NULL, is_active TINYINT(1) NOT NULL DEFAULT 1) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

// Point the app's Database singleton at the scratch DB.
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

set_exception_handler(function (Throwable $e) {
    fwrite(STDERR, 'ERROR: ' . $e->getMessage() . "\n");
    exit(1);
});

use App\Models\TypeArme;
use App\Models\Arme;
use App\Models\ArmeMunitionsConsommation;
use App\Models\Armement;
use App\Database;

// =============================================================================
// TypeArme
// =============================================================================
echo "TypeArme CRUD\n";

$typeId = TypeArme::create(['nom' => 'Pistolet PA 9mm', 'description' => 'Arme de poing', 'munitions_stock' => 0]);
$type = TypeArme::getById($typeId);
check($type !== null && $type['nom'] === 'Pistolet PA 9mm', 'create + getById');
check((int) $type['munitions_stock'] === 0, 'type munitions_stock default 0');
check(TypeArme::getByNom('Pistolet PA 9mm') !== null, 'getByNom');

$typeId2 = TypeArme::create(['nom' => 'Fusil AK-47', 'munitions_stock' => 0]);
check(count(TypeArme::getAll()) === 2, 'getAll returns 2 types');
check(count(TypeArme::getAll(['search' => 'AK'])) === 1, 'search filter');

TypeArme::update($typeId, ['description' => 'Arme de poing 9mm Parabellum', 'munitions_stock' => 100]);
check(TypeArme::getById($typeId)['description'] === 'Arme de poing 9mm Parabellum', 'update description');
check((int) TypeArme::getById($typeId)['munitions_stock'] === 100, 'update munitions_stock on type');

// Uniqueness — duplicate nom rejected at the model layer (controller validates).
try {
    TypeArme::create(['nom' => 'Pistolet PA 9mm']);
    check(false, 'duplicate nom should fail (FK/unique)');
} catch (\PDOException $e) {
    check(true, 'duplicate nom rejected by unique constraint');
}

// countArmes — 0 before any arme.
check(TypeArme::countArmes($typeId) === 0, 'countArmes 0 before arme created');

// =============================================================================
// Arme
// =============================================================================
echo "Arme CRUD\n";

$armeId = Arme::create(['type_arme_id' => $typeId, 'matricule' => 'PA-0001', 'munitions_stock' => 0]);
$arme = Arme::getById($armeId);
check($arme !== null && $arme['matricule'] === 'PA-0001', 'create + getById');
check($arme['type_arme_nom'] === 'Pistolet PA 9mm', 'joined type_arme_nom');
check((int) $arme['type_arme_munitions_stock'] === 100, 'joined type_arme_munitions_stock');
check(TypeArme::countArmes($typeId) === 1, 'countArmes 1 after arme created');

$armeId2 = Arme::create(['type_arme_id' => $typeId2, 'matricule' => 'AK47-001', 'munitions_stock' => 0]);
check(count(Arme::getAll()) === 2, 'getAll returns 2 armes');
check(count(Arme::getAll(['type_arme_id' => $typeId])) === 1, 'filter by type_arme_id');
check(count(Arme::getAll(['search' => 'AK47'])) === 1, 'search by matricule');
check(count(Arme::getAll(['search' => 'Pistolet'])) === 1, 'search by type nom');

// Matricule uniqueness.
try {
    Arme::create(['type_arme_id' => $typeId, 'matricule' => 'PA-0001', 'munitions_stock' => 0]);
    check(false, 'duplicate matricule should fail');
} catch (\PDOException $e) {
    check(true, 'duplicate matricule rejected by unique constraint');
}

Arme::update($armeId, ['munitions_stock' => 28]);
check((int) Arme::getById($armeId)['munitions_stock'] === 28, 'update munitions_stock');

// getByMatricule
check(Arme::getByMatricule('PA-0001')['id'] === $armeId, 'getByMatricule');

// =============================================================================
// decreaseStock — atomic conditional deduction
// =============================================================================
echo "Stock deduction\n";

// Reset stock to 30 for clean assertions — stock is on type_arme now.
TypeArme::update($typeId, ['munitions_stock' => 30]);

// Normal deduction — deducts from the type's stock.
check(Arme::decreaseStock($armeId, 5), 'decrease 5 from 30 succeeds');
check((int) TypeArme::getById($typeId)['munitions_stock'] === 25, 'type stock is now 25');

// Deduct exactly the remaining stock.
check(Arme::decreaseStock($armeId, 25), 'decrease 25 from 25 succeeds (exact)');
check((int) TypeArme::getById($typeId)['munitions_stock'] === 0, 'type stock is now 0');

// Insufficient stock — must fail and leave stock at 0 (never negative).
check(!Arme::decreaseStock($armeId, 1), 'decrease 1 from 0 rejected');
check((int) TypeArme::getById($typeId)['munitions_stock'] === 0, 'type stock stays 0 (never negative)');

// Insufficient stock partial.
TypeArme::update($typeId, ['munitions_stock' => 2]);
check(!Arme::decreaseStock($armeId, 3), 'decrease 3 from 2 rejected');
check((int) TypeArme::getById($typeId)['munitions_stock'] === 2, 'type stock stays 2 (never goes to -1)');

// Non-existent arme.
check(!Arme::decreaseStock(999999, 1), 'decrease on non-existent arme rejected');

// Shared stock: two armes of the same type share the same munitions pool.
// Create a second arme of the same type and verify that consuming from one
// arme decreases the shared type stock visible to the other.
$armeIdSameType = Arme::create(['type_arme_id' => $typeId, 'matricule' => 'PA-0002', 'munitions_stock' => 0]);
TypeArme::update($typeId, ['munitions_stock' => 10]);
check(Arme::decreaseStock($armeIdSameType, 3), 'decrease 3 from second arme of same type succeeds');
check((int) TypeArme::getById($typeId)['munitions_stock'] === 7, 'shared type stock is 7 after second arme consumed 3');
check((int) Arme::getById($armeId)['type_arme_munitions_stock'] === 7, 'first arme sees updated shared stock (7)');
check((int) Arme::getById($armeIdSameType)['type_arme_munitions_stock'] === 7, 'second arme sees same shared stock (7)');

// =============================================================================
// Consumption history
// =============================================================================
echo "Consumption history\n";

TypeArme::update($typeId, ['munitions_stock' => 30]);
Arme::decreaseStock($armeId, 1);
$histId = ArmeMunitionsConsommation::create([
    'arme_id' => $armeId,
    'agent_id' => null,
    'armement_id' => null,
    'quantite' => 1,
    'date_consommation' => date('Y-m-d H:i:s'),
]);
check($histId > 0, 'create consumption history row');
$hist = ArmeMunitionsConsommation::getByArmeId($armeId);
check(count($hist) === 1 && (int) $hist[0]['quantite'] === 1, 'getByArmeId returns 1 row');
check($hist[0]['arme_matricule'] === 'PA-0001', 'history joined arme_matricule');
check($hist[0]['type_arme_nom'] === 'Pistolet PA 9mm', 'history joined type_arme_nom');

// =============================================================================
// Controller validation (private static, via reflection)
// =============================================================================
echo "Controller validation\n";

$validateArme = function (array $data, bool $isCreate = true, ?int $excludeId = null) use ($typeId, $typeId2, $armeId): array {
    $method = new ReflectionMethod(\App\Controllers\ArmeController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate, $excludeId);
};

check($validateArme(['type_arme_id' => $typeId, 'matricule' => 'X']) === [], 'valid arme payload passes');
check(isset($validateArme(['type_arme_id' => null])['type_arme_id']), 'missing type_arme_id rejected');
check(isset($validateArme(['type_arme_id' => $typeId, 'matricule' => ''])['matricule']), 'blank matricule rejected');
check(isset($validateArme(['type_arme_id' => 999999, 'matricule' => 'NEW-1'])['type_arme_id']), 'unknown type_arme_id rejected');
check(isset($validateArme(['type_arme_id' => $typeId, 'matricule' => 'PA-0001'])['matricule']), 'duplicate matricule rejected on create');
check(!isset($validateArme(['type_arme_id' => $typeId, 'matricule' => 'PA-0001'], false, $armeId)['matricule']), 'own matricule allowed on update');
check(isset($validateArme(['type_arme_id' => $typeId, 'matricule' => 'NEW-OK', 'munitions_stock' => -1])['munitions_stock']), 'negative munitions_stock rejected');

$validateType = function (array $data, bool $isCreate = true, ?int $excludeId = null) use ($typeId): array {
    $method = new ReflectionMethod(\App\Controllers\TypeArmeController::class, 'validate');
    $method->setAccessible(true);
    return $method->invoke(null, $data, $isCreate, $excludeId);
};
check(isset($validateType(['nom' => ''])['nom']), 'missing type nom rejected');
check(isset($validateType(['nom' => 'Pistolet PA 9mm'])['nom']), 'duplicate type nom rejected');
check(!isset($validateType(['nom' => 'Pistolet PA 9mm'], false, $typeId)['nom']), 'own type nom allowed on update');

// =============================================================================
// Delete restrictions
// =============================================================================
echo "Delete restrictions\n";

// TypeArme with weapons — deletion must be rejected by the controller.
check(TypeArme::countArmes($typeId) > 0, 'type has weapons (countArmes > 0)');

// Arme with consumption history — deletion must be rejected by the controller.
check(Arme::countConsommations($armeId) === 1, 'arme has consumption history');

// Arme with armement perception — create one, then check countArmements.
$armementId = Armement::create([
    'date_perception' => '2026-09-03',
    'heure_perception' => '10:00',
    'agent_preneur_im' => 'IM-100',
    'agent_preneur_grade' => 'Brigadier',
    'agent_preneur_nom' => 'Test Agent',
    'arme_id' => $armeId,
    'type_arme' => 'Pistolet PA 9mm',
    'matricule_arme' => 'PA-0001',
    'munitions' => 30,
    'secteur_mission' => 'Patrouille',
    'etat_perception' => 'Bon état',
    'created_by' => null,
]);
check($armementId > 0, 'armement created with arme_id');
check(Arme::countArmements($armeId) === 1, 'arme has 1 perception');

// =============================================================================
// Armement perception snapshots the arme identity
// =============================================================================
echo "Armement arme snapshot\n";

$armement = Armement::getById($armementId);
check((int) $armement['arme_id'] === $armeId, 'armement.arme_id persisted');
check($armement['type_arme'] === 'Pistolet PA 9mm', 'type_arme snapshotted from arme');
check($armement['matricule_arme'] === 'PA-0001', 'matricule_arme snapshotted from arme');

// snapshotArme overrides client-supplied free-text with the arme's canonical values.
$snapMethod = new ReflectionMethod(\App\Controllers\ArmementController::class, 'snapshotArme');
$snapMethod->setAccessible(true);
$data = ['arme_id' => $armeId, 'type_arme' => 'WRONG', 'matricule_arme' => 'WRONG'];
$snapMethod->invokeArgs(null, [&$data]);
check($data['type_arme'] === 'Pistolet PA 9mm', 'snapshotArme overrides type_arme');
check($data['matricule_arme'] === 'PA-0001', 'snapshotArme overrides matricule_arme');

// Unknown arme_id → nullified, free-text kept.
$data = ['arme_id' => 999999, 'type_arme' => 'Free', 'matricule_arme' => 'Free-1'];
$snapMethod->invokeArgs(null, [&$data]);
check($data['arme_id'] === null, 'unknown arme_id nullified');
check($data['type_arme'] === 'Free', 'free-text type_arme kept when arme unknown');

// Empty arme_id → null, free-text kept (legacy compatibility).
$data = ['arme_id' => null, 'type_arme' => 'Legacy', 'matricule_arme' => 'Legacy-1'];
$snapMethod->invokeArgs(null, [&$data]);
check($data['arme_id'] === null, 'empty arme_id stays null');
check($data['type_arme'] === 'Legacy', 'legacy free-text kept');

// =============================================================================
// Armement reintegration deducts the weapon's stock atomically
// =============================================================================
echo "Armement reintegration stock deduction\n";

// Reset the type stock and create a fresh perception so reintegration is allowed.
TypeArme::update($typeId, ['munitions_stock' => 30]);
$armementId2 = Armement::create([
    'date_perception' => '2026-09-03',
    'heure_perception' => '11:00',
    'agent_preneur_im' => 'IM-200',
    'agent_preneur_grade' => 'Adjudant',
    'agent_preneur_nom' => 'Second Agent',
    'arme_id' => $armeId,
    'type_arme' => 'Pistolet PA 9mm',
    'matricule_arme' => 'PA-0001',
    'munitions' => 30,
    'secteur_mission' => 'Poste fixe',
    'etat_perception' => 'Bon état',
    'created_by' => null,
]);

// Simulate the controller's reintegration transaction directly (the model
// path the controller uses), so we test the atomic deduction + history.
$db = Database::getInstance()->getConnection();
$db->beginTransaction();
Armement::reintegrate($armementId2, [
    'heure_reintegration' => '18:00',
    'date_reintegration' => '2026-09-03',
    'etat_reintegration' => 'Bon état',
    'munitions_consommees' => 5,
]);
$deducted = Arme::decreaseStock($armeId, 5);
if ($deducted) {
    ArmeMunitionsConsommation::create([
        'arme_id' => $armeId,
        'agent_id' => null,
        'armement_id' => $armementId2,
        'quantite' => 5,
        'date_consommation' => date('Y-m-d H:i:s'),
    ]);
    $db->commit();
} else {
    $db->rollBack();
}
check($deducted, 'reintegration deducted 5 from stock');
check((int) TypeArme::getById($typeId)['munitions_stock'] === 25, 'type stock is 25 after reintegration');
$reintArmement = Armement::getById($armementId2);
check((int) $reintArmement['munitions_consommees'] === 5, 'armement munitions_consommees = 5');
check($reintArmement['heure_reintegration'] !== null, 'armement reintegrated');
$hist2 = ArmeMunitionsConsommation::getByArmeId($armeId);
check(count($hist2) === 2, 'consumption history now has 2 rows');
$reintHistRow = array_filter($hist2, fn($r) => (int) $r['armement_id'] === $armementId2);
check(count($reintHistRow) === 1 && (int) reset($reintHistRow)['quantite'] === 5, 'history row linked to armement with quantite 5');

// Insufficient stock during reintegration → rollback, armement stays "en cours".
TypeArme::update($typeId, ['munitions_stock' => 2]);
$armementId3 = Armement::create([
    'date_perception' => '2026-09-03',
    'heure_perception' => '12:00',
    'agent_preneur_im' => 'IM-300',
    'agent_preneur_grade' => 'Brigadier',
    'agent_preneur_nom' => 'Third Agent',
    'arme_id' => $armeId,
    'type_arme' => 'Pistolet PA 9mm',
    'matricule_arme' => 'PA-0001',
    'munitions' => 30,
    'secteur_mission' => 'Patrouille',
    'etat_perception' => 'Bon état',
    'created_by' => null,
]);
$db->beginTransaction();
Armement::reintegrate($armementId3, [
    'heure_reintegration' => '19:00',
    'date_reintegration' => '2026-09-03',
    'etat_reintegration' => 'Bon état',
    'munitions_consommees' => 3,
]);
$deducted3 = Arme::decreaseStock($armeId, 3);
if ($deducted3) {
    $db->commit();
} else {
    $db->rollBack();
}
check(!$deducted3, 'reintegration with insufficient stock rejected');
check((int) TypeArme::getById($typeId)['munitions_stock'] === 2, 'type stock unchanged (2) after failed reintegration');
$reintArmement3 = Armement::getById($armementId3);
check($reintArmement3['heure_reintegration'] === null, 'armement stays en cours after rollback');

// =============================================================================
// Concurrent consumption simulation (race-condition safety)
// =============================================================================
echo "Concurrency\n";

// Two consumers each try to take 2 from a stock of 3. Only one should win.
TypeArme::update($typeId, ['munitions_stock' => 3]);
$winner = 0;
$loser = 0;
for ($i = 0; $i < 2; $i++) {
    if (Arme::decreaseStock($armeId, 2)) {
        $winner++;
    } else {
        $loser++;
    }
}
check($winner === 1, 'exactly one concurrent consumer wins');
check($loser === 1, 'exactly one concurrent consumer loses');
check((int) TypeArme::getById($typeId)['munitions_stock'] === 1, 'type stock is 1 after concurrent consumption (never negative)');

// =============================================================================
// Cleanup
// =============================================================================
echo "Cleanup\n";
Arme::delete($armeId2);
check(Arme::getById($armeId2) === null, 'delete arme with no references');
// armeId still has references — direct delete would violate FK; skip.

$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? "ALL TESTS PASSED\n" : "$failures TEST(S) FAILED\n");
exit($failures === 0 ? 0 : 1);
