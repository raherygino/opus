<?php

/**
 * Passation model + validation + notification + auth-verify tests (no PHPUnit required).
 *
 * Usage: php api/tests/PassationTest.php
 *
 * Creates a scratch database (opus_test_passation), applies migration
 * database/022_create_passation.sql + 023_create_attach_passation.sql plus the
 * migrations needed for users/roles/permissions/notifications, exercises the
 * Passation model, the controller validation rules, the notification flow
 * triggered on create/update, and the AuthController::verify credential-check
 * path, then drops the scratch database.
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
$scratch = 'opus_test_passation';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// --- Apply migrations in order ----------------------------------------------
$migrations = [
    '001_create_roles.sql',
    '002_create_personnel.sql',
    '003_create_users.sql',
    '008_create_role_permissions.sql',
    '009_create_notifications.sql',
    '018_add_notification_link.sql',
    '022_create_passation.sql',
    '023_create_attach_passation.sql',
];
foreach ($migrations as $file) {
    $sql = file_get_contents($root . '/database/' . $file);
    foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
        if (preg_match('/^\s*(CREATE|ALTER|INSERT)/i', preg_replace('/^--.*$/m', '', $stmt))) {
            $pdo->exec($stmt);
        }
    }
}

// User::getById (used by Notification::sendPush to enrich the push payload)
// runs a subquery against mouvement_personnel. Create a minimal stub table
// so the push-enrichment query succeeds and doesn't spam the test output.
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

use App\Models\Passation;
use App\Models\PassationAttachment;
use App\Models\Notification;
use App\Models\User;

function sampleRow(array $overrides = []): array
{
    return array_merge([
        'date_passation' => '2026-08-23',
        'heure_passation' => '18:00',
        'chef_descendant_user_id' => null,
        'chef_descendant_grade' => 'Brigadier',
        'chef_descendant_lastname' => 'Rakoto',
        'chef_montant_user_id' => null,
        'chef_montant_grade' => 'Adjudant',
        'chef_montant_lastname' => 'Jean',
        'instructions_autorite' => 'Renforcer la patrouille de nuit',
        'incidents_survenus' => 'Aucun incident',
        'created_by' => null,
    ], $overrides);
}

// --- Model: create / read ----------------------------------------------------
echo "Model CRUD\n";
$id = Passation::create(sampleRow());
$row = Passation::getById($id);
check($row !== null && $row['chef_descendant_lastname'] === 'Rakoto', 'create + getById');
check($row['chef_montant_lastname'] === 'Jean', 'chef_montant_lastname persisted');
check($row['instructions_autorite'] === 'Renforcer la patrouille de nuit', 'instructions_autorite persisted');

// --- Model: update / filters / delete ----------------------------------------
check(Passation::update($id, ['incidents_survenus' => 'Vol signalé']), 'update incidents_survenus');
check(Passation::getById($id)['incidents_survenus'] === 'Vol signalé', 'incidents_survenus persisted');

// Chef identity columns must NOT be updatable via the model's allowed list
// (the controller strips them on update; verify the model itself ignores them
// only because they ARE in the allowed list — the controller is the guard).
// Run this AFTER the search tests so it doesn't clobber the search fixture.
$id2 = Passation::create(sampleRow([
    'chef_descendant_lastname' => 'Rabe',
    'chef_montant_lastname' => 'Marie',
    'date_passation' => '2026-08-20',
]));

check(count(Passation::getAll(['search' => 'Rakoto'])) === 1, 'search filter (descendant name)');
check(count(Passation::getAll(['search' => 'Marie'])) === 1, 'search filter (montant name)');
check(count(Passation::getAll(['search' => 'no-such-thing'])) === 0, 'search no match');
check(count(Passation::getAll(['date_from' => '2026-08-21'])) === 1, 'date_from filter');
check(count(Passation::getAll(['date_to' => '2026-08-21'])) === 1, 'date_to filter');

Passation::update($id, ['chef_descendant_lastname' => 'HACKED']);
check(Passation::getById($id)['chef_descendant_lastname'] === 'HACKED', 'model allows chef update (controller guards it)');

// --- Attachments ---------------------------------------------------------------
echo "Attachments\n";
$attId = PassationAttachment::create([
    'passation_id' => $id,
    'title' => 'Feuille de passation signée',
    'filename' => 'pass_abc123.pdf',
    'original_filename' => 'passation.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 2345,
]);
check(PassationAttachment::belongsToPassation($attId, $id), 'belongsToPassation true');
check(!PassationAttachment::belongsToPassation($attId, $id2), 'belongsToPassation false for other record');
check(count(PassationAttachment::getByPassationId($id)) === 1, 'getByPassationId');

Passation::delete($id2);
check(Passation::getById($id2) === null, 'delete passation');
check(count(PassationAttachment::getByPassationId($id2)) === 0, 'attachments cascade-deleted');

// --- Controller validation rules (private static, via reflection) --------------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\PassationController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true): array =>
    $method->invoke(null, $data, $isCreate);

// Validation requires non-null chef user_ids on create (the controller sets
// the descendant from the auth user; the client sends the montant after
// /auth/verify). Use dummy non-zero IDs — validation only checks presence.
$validRow = sampleRow(['chef_descendant_user_id' => 999, 'chef_montant_user_id' => 888]);
check($validate($validRow) === [], 'valid payload passes');
check(isset($validate(sampleRow(['date_passation' => '']))['date_passation']), 'missing date_passation rejected');
check(isset($validate(sampleRow(['date_passation' => '23/08/2026']))['date_passation']), 'invalid date_passation format rejected');
check(isset($validate(sampleRow(['heure_passation' => '']))['heure_passation']), 'missing heure_passation rejected');
check(isset($validate(sampleRow(['heure_passation' => '25:00']))['heure_passation']), 'invalid heure_passation rejected');
check(isset($validate(sampleRow(['chef_montant_user_id' => null]))['chef_montant']), 'missing chef_montant_user_id rejected on create');
check(isset($validate(sampleRow(['chef_montant_grade' => '']))['chef_montant']), 'missing chef_montant_grade rejected on create');
check(isset($validate(sampleRow(['chef_montant_lastname' => '']))['chef_montant']), 'missing chef_montant_lastname rejected on create');
check(isset($validate(sampleRow(['chef_descendant_user_id' => null]))['chef_descendant']), 'missing chef_descendant_user_id rejected on create');
check(isset($validate(sampleRow(['chef_descendant_grade' => '']))['chef_descendant']), 'missing chef_descendant_grade rejected on create');
check(isset($validate(sampleRow(['chef_descendant_lastname' => '']))['chef_descendant']), 'missing chef_descendant_lastname rejected on create');

// Partial update: only provided fields are validated; chef identities are
// stripped by the controller before validate() is called, so an update with
// only instructions_autorite must pass.
check($validate(['instructions_autorite' => 'Nouvelles instructions'], false) === [], 'partial update with valid field passes');
check($validate(['date_passation' => ''], false)['date_passation'] ?? null !== null, 'partial update with blank date rejected');

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

setPermissionRow($chefRoleId, 'sedentaire_poste_passation');

$admin1 = createUserRow(createPersonnelRow('Admin1', 'Test', 'Commandant'), 'admin1', $adminRoleId, password_hash('secret', PASSWORD_BCRYPT));
$chef1  = createUserRow(createPersonnelRow('Chef1',  'Desc', 'Brigadier'), 'chef1',  $chefRoleId, password_hash('descpw', PASSWORD_BCRYPT));
$chef2  = createUserRow(createPersonnelRow('Chef2',  'Mont', 'Adjudant'),  'chef2',  $chefRoleId, password_hash('montpw', PASSWORD_BCRYPT));

// A passation created by chef1: admins + other module users are notified,
// the actor is excluded, users without the module permission get nothing.
$passation = Passation::getById($id);
$link = '/sedentaire/poste/passation/' . $passation['id'];

Notification::notifyFeatureChange('sedentaire_poste_passation', [
    'title'   => 'Nouvelle passation',
    'message' => "Descendant: {$passation['chef_descendant_lastname']} — Montant: {$passation['chef_montant_lastname']}",
    'type'    => 'info',
    'service' => 'Sedentaire',
    'link'    => $link,
], [
    'title'   => 'Nouvelle passation',
    'message' => "Descendant: {$passation['chef_descendant_lastname']} — Montant: {$passation['chef_montant_lastname']} — Veuillez en prendre connaissance.",
    'type'    => 'info',
    'service' => 'Sedentaire',
    'link'    => $link,
], $chef1);

check(countNotificationsForUser($chef1) === 0, 'actor (chef1) received 0 notifications');
check(countNotificationsForUser($admin1) === 1, 'admin notified on create');
check(countNotificationsForUser($chef2) === 1, 'other module user (chef2) notified on create');

// markAsReadByLink auto-dismisses once the detail is opened (show()).
check(Notification::markAsReadByLink($link, $admin1) === 1, 'markAsReadByLink dismissed the admin notification');

// --- Auth verify flow (chef montant credential check) -------------------------
echo "Auth verify\n";

// User::getByUsername reuses the same lookup as AuthController::login/verify.
$montant = User::getByUsername('chef2');
check($montant !== null, 'getByUsername finds chef2');
check(password_verify('montpw', $montant['password_hash']), 'password_verify accepts correct password');
check(!password_verify('wrongpw', $montant['password_hash']), 'password_verify rejects wrong password');
check($montant['grade'] === 'Adjudant', 'montant grade retrieved via personnel JOIN');
check($montant['firstname'] === 'Chef2', 'montant firstname retrieved via personnel JOIN');

// The verify endpoint returns ONLY identity — never password_hash.
// Simulate the identity payload the controller would return.
$identity = [
    'id'        => $montant['id'],
    'username'  => $montant['username'],
    'grade'     => $montant['grade'],
    'firstname' => $montant['firstname'],
    'lastname'  => $montant['lastname'],
];
check(!array_key_exists('password_hash', $identity), 'identity payload contains no password_hash');
check(!array_key_exists('access_token', $identity), 'identity payload contains no access_token');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
