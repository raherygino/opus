<?php

/**
 * Armement model + validation + reintegration workflow tests (no PHPUnit required).
 *
 * Usage: php api/tests/ArmementTest.php
 *
 * Creates a scratch database (opus_test_armement), applies migration
 * database/024_create_armement.sql + 025_create_attach_armement.sql plus the
 * migrations needed for users/roles/permissions/notifications, exercises the
 * Armement model, the perception → réintégration workflow (one-way
 * transition), the controller validation rules, attachments, and the
 * notification flow, then drops the scratch database.
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
$scratch = 'opus_test_armement';
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
];
foreach ($migrations as $file) {
    $sql = file_get_contents($root . '/database/' . $file);
    foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
        if (preg_match('/^\s*(CREATE|ALTER|INSERT)/i', preg_replace('/^--.*$/m', '', $stmt))) {
            $pdo->exec($stmt);
        }
    }
}

// User::getById / Personnel::getById (used by Notification::sendPush and the
// agent preneur snapshot) run subqueries against mouvement_personnel. Create
// a minimal stub table so those queries succeed.
$pdo->exec('CREATE TABLE mouvement_personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, personnel_id INT UNSIGNED NULL, type_mouvement VARCHAR(100) NULL, retour VARCHAR(10) NULL DEFAULT "Non", created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

// DeviceToken stub — FcmSender queries device_tokens; create empty table so
// the push path doesn't error (no tokens registered → no pushes sent).
$pdo->exec('CREATE TABLE device_tokens (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, user_id INT UNSIGNED NULL, token VARCHAR(255) NULL, device_name VARCHAR(255) NULL, is_active TINYINT(1) NOT NULL DEFAULT 1) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

// Point the app's Database singleton at the scratch DB.
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

// Suppress the bootstrap exception handler's header() output during CLI tests
set_exception_handler(function (Throwable $e) {
    fwrite(STDERR, 'ERROR: ' . $e->getMessage() . "\n");
    exit(1);
});

use App\Models\Armement;
use App\Models\ArmementAttachment;
use App\Models\Notification;
use App\Models\Personnel;

function sampleRow(array $overrides = []): array
{
    return array_merge([
        'date_perception' => '2026-08-23',
        'heure_perception' => '18:00',
        'agent_preneur_personnel_id' => null,
        'agent_preneur_im' => 'IM-100',
        'agent_preneur_grade' => 'Brigadier',
        'agent_preneur_nom' => 'Jean Rakoto',
        'type_arme' => 'Pistolet PA',
        'matricule_arme' => 'PA-0001',
        'munitions' => 30,
        'secteur_mission' => 'Patrouille Centre-ville',
        'etat_perception' => 'Bon état',
        'created_by' => null,
    ], $overrides);
}

// --- Model: create / read ----------------------------------------------------
echo "Model CRUD\n";
$id = Armement::create(sampleRow());
$row = Armement::getById($id);
check($row !== null && $row['agent_preneur_nom'] === 'Jean Rakoto', 'create + getById');
check($row['matricule_arme'] === 'PA-0001', 'matricule_arme persisted');
check((int) $row['munitions'] === 30, 'munitions persisted');
check($row['heure_reintegration'] === null, 'new perception is en cours (heure_reintegration NULL)');

// --- Model: update (perception fields) / filters ------------------------------
check(Armement::update($id, ['secteur_mission' => 'Poste fixe Marché']), 'update secteur_mission');
check(Armement::getById($id)['secteur_mission'] === 'Poste fixe Marché', 'secteur_mission persisted');

$id2 = Armement::create(sampleRow([
    'agent_preneur_nom' => 'Marie Rabe',
    'type_arme' => 'Mousqueton',
    'matricule_arme' => 'MQ-0042',
    'date_perception' => '2026-08-20',
]));

check(count(Armement::getAll(['search' => 'Rakoto'])) === 1, 'search filter (agent name)');
check(count(Armement::getAll(['search' => 'MQ-0042'])) === 1, 'search filter (matricule arme)');
check(count(Armement::getAll(['search' => 'Mousqueton'])) === 1, 'search filter (type arme)');
check(count(Armement::getAll(['search' => 'no-such-thing'])) === 0, 'search no match');
check(count(Armement::getAll(['date_from' => '2026-08-21'])) === 1, 'date_from filter');
check(count(Armement::getAll(['date_to' => '2026-08-21'])) === 1, 'date_to filter');
check(count(Armement::getAll(['statut' => 'en_cours'])) === 2, 'statut en_cours filter (both non-reintegrated)');
check(count(Armement::getAll(['statut' => 'reintegree'])) === 0, 'statut reintegree filter (none yet)');

// Reintegration columns must NOT be updatable via the generic update path.
Armement::update($id, ['heure_reintegration' => '22:30']);
check(Armement::getById($id)['heure_reintegration'] === null, 'update() cannot touch reintegration columns');

// --- Reintegration workflow ---------------------------------------------------
echo "Reintegration\n";
check(Armement::reintegrate($id, [
    'heure_reintegration' => '22:30',
    'etat_reintegration' => 'Bon état',
    'munitions_consommees' => 5,
]), 'reintegrate succeeds while en cours');
$row = Armement::getById($id);
check(substr((string) $row['heure_reintegration'], 0, 5) === '22:30', 'heure_reintegration persisted');
check($row['etat_reintegration'] === 'Bon état', 'etat_reintegration persisted');
check((int) $row['munitions_consommees'] === 5, 'munitions_consommees persisted');
// Perception data is preserved after reintegration.
check($row['agent_preneur_nom'] === 'Jean Rakoto' && $row['matricule_arme'] === 'PA-0001', 'perception data preserved after reintegration');
// One-way transition: a second reintegration must fail.
check(!Armement::reintegrate($id, [
    'heure_reintegration' => '23:00',
    'etat_reintegration' => 'Rayé',
    'munitions_consommees' => 0,
]), 'second reintegration rejected');
check(substr((string) Armement::getById($id)['heure_reintegration'], 0, 5) === '22:30', 'original reintegration untouched');

check(count(Armement::getAll(['statut' => 'en_cours'])) === 1, 'statut en_cours filter after reintegration');
check(count(Armement::getAll(['statut' => 'reintegree'])) === 1, 'statut reintegree filter after reintegration');

// --- Attachments ---------------------------------------------------------------
echo "Attachments\n";
$attId = ArmementAttachment::create([
    'armement_id' => $id,
    'title' => 'Fiche de perception signée',
    'filename' => 'arm_abc123.pdf',
    'original_filename' => 'perception.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 2345,
]);
check(ArmementAttachment::belongsToArmement($attId, $id), 'belongsToArmement true');
check(!ArmementAttachment::belongsToArmement($attId, $id2), 'belongsToArmement false for other record');
check(count(ArmementAttachment::getByArmementId($id)) === 1, 'getByArmementId');

Armement::delete($id2);
check(Armement::getById($id2) === null, 'delete armement');

// --- Controller validation rules (private static, via reflection) --------------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\ArmementController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true): array =>
    $method->invoke(null, $data, $isCreate);

$validRow = sampleRow(['agent_preneur_personnel_id' => 999]);
check($validate($validRow) === [], 'valid payload passes');
check(isset($validate(sampleRow(['date_perception' => '']))['date_perception']), 'missing date_perception rejected');
check(isset($validate(sampleRow(['date_perception' => '23/08/2026']))['date_perception']), 'invalid date_perception format rejected');
check(isset($validate(sampleRow(['heure_perception' => '']))['heure_perception']), 'missing heure_perception rejected');
check(isset($validate(sampleRow(['heure_perception' => '25:00']))['heure_perception']), 'invalid heure_perception rejected');
check(isset($validate(sampleRow(['type_arme' => '']))['type_arme']), 'missing type_arme rejected');
check(isset($validate(sampleRow(['matricule_arme' => '  ']))['matricule_arme']), 'blank matricule_arme rejected');
check(isset($validate(sampleRow(['munitions' => -5]))['munitions']), 'negative munitions rejected');
check(isset($validate(sampleRow(['munitions' => 'abc']))['munitions']), 'non-numeric munitions rejected');
check($validate(sampleRow(['munitions' => null, 'agent_preneur_personnel_id' => 999])) === [], 'null munitions allowed');
check(isset($validate(sampleRow(['agent_preneur_personnel_id' => null]))['agent_preneur']), 'missing agent_preneur_personnel_id rejected on create');
check(isset($validate(sampleRow(['agent_preneur_grade' => '']))['agent_preneur']), 'missing agent_preneur_grade rejected on create');
check(isset($validate(sampleRow(['agent_preneur_nom' => '']))['agent_preneur']), 'missing agent_preneur_nom rejected on create');

// Partial update: only provided fields are validated.
check($validate(['secteur_mission' => 'Nouveau secteur'], false) === [], 'partial update with valid field passes');
check(isset($validate(['date_perception' => ''], false)['date_perception']), 'partial update with blank date rejected');

// --- Reintegration validation (private static, via reflection) -----------------
echo "Reintegration validation\n";
$reintMethod = new ReflectionMethod(\App\Controllers\ArmementController::class, 'validateReintegration');
$reintMethod->setAccessible(true);
$current = Armement::getById($id); // munitions = 30
$validateReint = fn(array $data): array => $reintMethod->invoke(null, $data, $current);

check($validateReint([
    'heure_reintegration' => '22:30',
    'etat_reintegration' => 'Bon état',
    'munitions_consommees' => 5,
]) === [], 'valid reintegration payload passes');
check(isset($validateReint(['heure_reintegration' => '', 'etat_reintegration' => 'Bon état', 'munitions_consommees' => 5])['heure_reintegration']), 'missing heure_reintegration rejected');
check(isset($validateReint(['heure_reintegration' => '25:00', 'etat_reintegration' => 'Bon état', 'munitions_consommees' => 5])['heure_reintegration']), 'invalid heure_reintegration rejected');
check(isset($validateReint(['heure_reintegration' => '22:30', 'etat_reintegration' => '', 'munitions_consommees' => 5])['etat_reintegration']), 'missing etat_reintegration rejected');
check(isset($validateReint(['heure_reintegration' => '22:30', 'etat_reintegration' => 'Bon état', 'munitions_consommees' => null])['munitions_consommees']), 'missing munitions_consommees rejected');
check(isset($validateReint(['heure_reintegration' => '22:30', 'etat_reintegration' => 'Bon état', 'munitions_consommees' => -1])['munitions_consommees']), 'negative munitions_consommees rejected');
check(isset($validateReint(['heure_reintegration' => '22:30', 'etat_reintegration' => 'Bon état', 'munitions_consommees' => 31])['munitions_consommees']), 'munitions_consommees > munitions rejected');

// --- Agent preneur snapshot (private static, via reflection) -------------------
echo "Agent preneur snapshot\n";
$agentPersonnelId = (function (): int {
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO personnel (im, grade, lastname, firstname, affectation) VALUES (?, ?, ?, ?, ?)');
    $stmt->execute(['IM-' . uniqid(), 'Adjudant', 'Ndiaye', 'Fatou', 'Sédentaire']);
    return (int) $db->lastInsertId();
})();

$snapMethod = new ReflectionMethod(\App\Controllers\ArmementController::class, 'snapshotAgentPreneur');
$snapMethod->setAccessible(true);

// The first param is by-ref; call via invokeArgs with a reference.
$data = ['agent_preneur_personnel_id' => $agentPersonnelId];
$snapMethod->invokeArgs(null, [&$data]);
check($data['agent_preneur_grade'] === 'Adjudant', 'snapshot grade from personnel');
check($data['agent_preneur_nom'] === 'Fatou Ndiaye', 'snapshot full name (firstname + lastname)');
check(!empty($data['agent_preneur_im']), 'snapshot IM from personnel');

$data = ['agent_preneur_personnel_id' => 999999];
$snapMethod->invokeArgs(null, [&$data]);
check($data['agent_preneur_personnel_id'] === null, 'unknown personnel_id nullified');

// --- Notification flow (mirrors the controller's notifyChange) -----------------
echo "Notifications\n";

function createRole(string $code, string $name): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO roles (code, name) VALUES (?, ?)');
    $stmt->execute([$code, $name]);
    return (int) $db->lastInsertId();
}

function createPersonnelRow(string $firstname, string $lastname, string $grade): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO personnel (im, grade, lastname, firstname, affectation) VALUES (?, ?, ?, ?, ?)');
    $stmt->execute(['IM-' . uniqid(), $grade, $lastname, $firstname, 'Sédentaire']);
    return (int) $db->lastInsertId();
}

function createUserRow(int $personnelId, string $username, int $roleId, string $passwordHash): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO users (personnel_id, username, password_hash, role_id, is_active) VALUES (?, ?, ?, ?, 1)');
    $stmt->execute([$personnelId, $username, $passwordHash, $roleId]);
    return (int) $db->lastInsertId();
}

function setPermissionRow(int $roleId, string $module): void
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare(
        'INSERT INTO role_permissions (role_id, module, can_view, can_create, can_edit, can_delete, can_export)
         VALUES (?, ?, 1, 0, 0, 0, 0)'
    );
    $stmt->execute([$roleId, $module]);
}

function countNotificationsForUser(int $userId): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('SELECT COUNT(*) FROM notifications WHERE user_id = ?');
    $stmt->execute([$userId]);
    return (int) $stmt->fetchColumn();
}

$adminRoleId = createRole('SUPER_ADMIN', 'Super Administrator');
$chefRoleId = createRole('CHEF_POSTE', 'Chef de poste');

setPermissionRow($chefRoleId, 'sedentaire_poste_armement');

$admin1 = createUserRow(createPersonnelRow('Admin1', 'Test', 'Commandant'), 'admin1', $adminRoleId, password_hash('secret', PASSWORD_BCRYPT));
$chef1  = createUserRow(createPersonnelRow('Chef1',  'Desc', 'Brigadier'), 'chef1',  $chefRoleId, password_hash('descpw', PASSWORD_BCRYPT));
$chef2  = createUserRow(createPersonnelRow('Chef2',  'Mont', 'Adjudant'),  'chef2',  $chefRoleId, password_hash('montpw', PASSWORD_BCRYPT));

// An armement created by chef1: admins + other module users are notified,
// the actor is excluded, users without the module permission get nothing.
$armement = Armement::getById($id);
$link = '/sedentaire/poste/armement/' . $armement['id'];

Notification::notifyFeatureChange('sedentaire_poste_armement', [
    'title'   => "Nouvelle perception d'arme",
    'message' => "Agent preneur: {$armement['agent_preneur_nom']} — {$armement['type_arme']} {$armement['matricule_arme']}",
    'type'    => 'info',
    'service' => 'Sedentaire',
    'link'    => $link,
], [
    'title'   => "Nouvelle perception d'arme",
    'message' => "Agent preneur: {$armement['agent_preneur_nom']} — {$armement['type_arme']} {$armement['matricule_arme']} — Veuillez en prendre connaissance.",
    'type'    => 'info',
    'service' => 'Sedentaire',
    'link'    => $link,
], $chef1);

check(countNotificationsForUser($chef1) === 0, 'actor (chef1) received 0 notifications');
check(countNotificationsForUser($admin1) === 1, 'admin notified on create');
check(countNotificationsForUser($chef2) === 1, 'other module user (chef2) notified on create');

// markAsReadByLink auto-dismisses once the detail is opened (show()).
check(Notification::markAsReadByLink($link, $admin1) === 1, 'markAsReadByLink dismissed the admin notification');

// --- Personnel code secret (Armement identity verification) -------------------
echo "Personnel code secret\n";

// Create a dedicated personnel for the agent preneur.
$agentPersonnel = (function (): int {
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO personnel (im, grade, lastname, firstname, affectation) VALUES (?, ?, ?, ?, ?)');
    $stmt->execute(['IM-AGENT-1', 'Brigadier', 'Rakoto', 'Jean', 'Sédentaire']);
    return (int) $db->lastInsertId();
})();

// No code secret set yet.
check(!Personnel::hasCodeSecret($agentPersonnel), 'hasCodeSecret false before setting');
check(!Personnel::verifyCodeSecret($agentPersonnel, '1234'), 'verifyCodeSecret false when no code set');

// Set a code secret.
check(Personnel::setCodeSecret($agentPersonnel, '4321'), 'setCodeSecret succeeds');
check(Personnel::hasCodeSecret($agentPersonnel), 'hasCodeSecret true after setting');
check(Personnel::verifyCodeSecret($agentPersonnel, '4321'), 'verifyCodeSecret true with correct code');
check(!Personnel::verifyCodeSecret($agentPersonnel, '1234'), 'verifyCodeSecret false with wrong code');

// The hash must never appear in any Personnel response.
$person = Personnel::getById($agentPersonnel);
check(!array_key_exists('code_secret_hash', $person), 'code_secret_hash stripped from getById');
$all = Personnel::getAll(['search' => 'Rakoto']);
check(!array_key_exists('code_secret_hash', $all[0]), 'code_secret_hash stripped from getAll');
$avail = Personnel::getAvailableForUser();
$found = false;
foreach ($avail as $p) {
    if (array_key_exists('code_secret_hash', $p)) { $found = true; break; }
}
check(!$found, 'code_secret_hash stripped from getAvailableForUser');

// Clear the code secret.
check(Personnel::setCodeSecret($agentPersonnel, null), 'setCodeSecret(null) clears the code');
check(!Personnel::hasCodeSecret($agentPersonnel), 'hasCodeSecret false after clearing');
check(!Personnel::verifyCodeSecret($agentPersonnel, '4321'), 'verifyCodeSecret false after clearing');

// Set a new code for the armement verification tests.
Personnel::setCodeSecret($agentPersonnel, '9876');

// --- Armement verification + signature ----------------------------------------
echo "Armement verification + signature\n";

// Create an armement with agent_verifie + signature_svg.
$verifiedId = Armement::create(sampleRow([
    'agent_preneur_personnel_id' => $agentPersonnel,
    'agent_verifie' => 1,
    'agent_verifie_at' => '2026-08-23 18:05:00',
    'signature_svg' => '<svg xmlns="http://www.w3.org/2000/svg"><path d="M10 10 L100 100" /></svg>',
]));
$verifiedRow = Armement::getById($verifiedId);
check((int) $verifiedRow['agent_verifie'] === 1, 'agent_verifie persisted as 1');
check(!empty($verifiedRow['agent_verifie_at']), 'agent_verifie_at persisted');
check($verifiedRow['signature_svg'] !== null && strpos($verifiedRow['signature_svg'], '<svg') !== false, 'signature_svg persisted');

// Create an armement without verification (backward-compatible default).
$unverifiedId = Armement::create(sampleRow([
    'agent_preneur_personnel_id' => $agentPersonnel,
]));
$unverifiedRow = Armement::getById($unverifiedId);
check((int) $unverifiedRow['agent_verifie'] === 0, 'agent_verifie defaults to 0 when not provided');
check($unverifiedRow['agent_verifie_at'] === null, 'agent_verifie_at null when not provided');
check($unverifiedRow['signature_svg'] === null, 'signature_svg null when not provided');

// The generic update() path must NOT be able to modify the verification or
// signature fields — they are one-way (set at perception time).
Armement::update($verifiedId, ['agent_verifie' => 0, 'signature_svg' => '<svg>tampered</svg>']);
$afterUpdate = Armement::getById($verifiedId);
check((int) $afterUpdate['agent_verifie'] === 1, 'update() cannot modify agent_verifie');
check($afterUpdate['signature_svg'] === $verifiedRow['signature_svg'], 'update() cannot modify signature_svg');

// Reintegration still works on a verified armement — the verification data
// is preserved alongside the reintegration data.
check(Armement::reintegrate($verifiedId, [
    'heure_reintegration' => '20:00',
    'etat_reintegration' => 'Bon état',
    'munitions_consommees' => 10,
]), 'reintegration succeeds on verified armement');
$reintVerified = Armement::getById($verifiedId);
check((int) $reintVerified['agent_verifie'] === 1, 'agent_verifie preserved after reintegration');
check($reintVerified['signature_svg'] !== null, 'signature_svg preserved after reintegration');
check(substr((string) $reintVerified['heure_reintegration'], 0, 5) === '20:00', 'reintegration time persisted on verified armement');

// --- Location (latitude/longitude) ---------------------------------------------
echo "Location\n";
$idLoc = Armement::create(sampleRow([
    'type_arme' => 'Pistolet',
    'matricule_arme' => 'P-LOC1',
    'latitude' => '-18.912345',
    'longitude' => '47.523456',
]));
$row = Armement::getById($idLoc);
check($row['latitude'] !== null, 'latitude persisted on create');
check($row['longitude'] !== null, 'longitude persisted on create');
check((float) $row['latitude'] === -18.912345, 'latitude value correct');
check((float) $row['longitude'] === 47.523456, 'longitude value correct');

// Null latitude/longitude when not provided (desktop-created armements).
$idNoLoc = Armement::create(sampleRow([
    'type_arme' => 'Pistolet',
    'matricule_arme' => 'P-LOC2',
]));
$rowNoLoc = Armement::getById($idNoLoc);
check($rowNoLoc['latitude'] === null, 'latitude null when not provided');
check($rowNoLoc['longitude'] === null, 'longitude null when not provided');

// Update latitude/longitude.
Armement::update($idLoc, ['latitude' => '-19.000000', 'longitude' => '48.000000']);
$rowUpd = Armement::getById($idLoc);
check((float) $rowUpd['latitude'] === -19.000000, 'latitude updated');
check((float) $rowUpd['longitude'] === 48.000000, 'longitude updated');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
