<?php

/**
 * DeclarationPerte model + validation + notification tests (no PHPUnit required).
 *
 * Usage: php api/tests/DeclarationPerteTest.php
 *
 * Creates a scratch database (opus_test_declaration_perte), applies migration
 * database/020_create_declaration_perte.sql plus the migrations needed for
 * users/roles/permissions/notifications, exercises the DeclarationPerte
 * model, the controller validation rules, and the notification flow
 * triggered on create/update, then drops the scratch database.
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
$scratch = 'opus_test_declaration_perte';
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
    '020_create_declaration_perte.sql',
    '021_create_attach_declaration_perte.sql',
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
// This must be loaded BEFORE any output so the bootstrap's exception handler
// (which calls header()) doesn't trigger "headers already sent" warnings.
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

// Suppress the bootstrap exception handler's header() output during CLI tests
set_exception_handler(function (Throwable $e) {
    fwrite(STDERR, 'ERROR: ' . $e->getMessage() . "\n");
    exit(1);
});

use App\Models\DeclarationPerte;
use App\Models\DeclarationPerteAttachment;
use App\Models\Notification;

function sampleRow(array $overrides = []): array
{
    return array_merge([
        'date_declaration' => '2026-08-23',
        'heure_declaration' => '10:15',
        'identite_declarant' => 'Rakoto Jean',
        'nature_objet' => "Carte nationale d'identité",
        'description_objet' => 'CNI au nom de Rakoto Jean, délivrée en 2020',
        'date_perte' => '2026-08-22',
        'lieu_perte' => 'Marché Anosy',
        'numero_attestation' => 'ATT-2026-0001',
        'nom_agent' => 'Agent System Admin',
        'created_by' => null,
    ], $overrides);
}

// --- Model: create / read ----------------------------------------------------
echo "Model CRUD\n";
$id = DeclarationPerte::create(sampleRow());
$row = DeclarationPerte::getById($id);
check($row !== null && $row['numero_attestation'] === 'ATT-2026-0001', 'create + getById');
check($row['identite_declarant'] === 'Rakoto Jean', 'identite_declarant persisted');

$dupRejected = false;
try {
    DeclarationPerte::create(sampleRow()); // duplicate ATT-2026-0001
} catch (PDOException $e) {
    $dupRejected = true;
}
check($dupRejected, 'duplicate numero_attestation rejected by unique key');

// --- Model: update / filters / delete ----------------------------------------
check(DeclarationPerte::update($id, ['lieu_perte' => 'Gare routière']), 'update lieu_perte');
check(DeclarationPerte::getById($id)['lieu_perte'] === 'Gare routière', 'lieu_perte persisted');

$id2 = DeclarationPerte::create(sampleRow([
    'numero_attestation' => 'ATT-2026-0002',
    'identite_declarant' => 'Rabe Marie',
    'date_declaration' => '2026-08-20',
]));

check(count(DeclarationPerte::getAll(['search' => 'Rakoto'])) === 1, 'search filter (declarant)');
check(count(DeclarationPerte::getAll(['search' => 'ATT-2026-0002'])) === 1, 'search filter (attestation)');
check(count(DeclarationPerte::getAll(['search' => 'no-such-thing'])) === 0, 'search no match');
check(count(DeclarationPerte::getAll(['date_from' => '2026-08-21'])) === 1, 'date_from filter');
check(count(DeclarationPerte::getAll(['date_to' => '2026-08-21'])) === 1, 'date_to filter');

check(DeclarationPerte::getByAttestation('ATT-2026-0002')['id'] == $id2, 'getByAttestation');

// --- Attachments ---------------------------------------------------------------
echo "Attachments\n";
$attId = DeclarationPerteAttachment::create([
    'declaration_id' => $id,
    'title' => 'Attestation signée',
    'filename' => 'att_abc123.pdf',
    'original_filename' => 'attestation.pdf',
    'mime_type' => 'application/pdf',
    'file_size' => 2345,
]);
check(DeclarationPerteAttachment::belongsToDeclaration($attId, $id), 'belongsToDeclaration true');
check(!DeclarationPerteAttachment::belongsToDeclaration($attId, $id2), 'belongsToDeclaration false for other record');
check(count(DeclarationPerteAttachment::getByDeclarationId($id)) === 1, 'getByDeclarationId');

DeclarationPerte::delete($id2);
check(DeclarationPerte::getById($id2) === null, 'delete declaration');
check(count(DeclarationPerteAttachment::getByDeclarationId($id2)) === 0, 'attachments cascade-deleted');

// --- Controller validation rules (private static, via reflection) --------------
echo "Validation\n";
$method = new ReflectionMethod(\App\Controllers\DeclarationPerteController::class, 'validate');
$method->setAccessible(true);
$validate = fn(array $data, bool $isCreate = true, ?int $excludeId = null): array =>
    $method->invoke(null, $data, $isCreate, $excludeId);

// Fixture for uniqueness checks: a record with attestation DUP-1
$dupId = DeclarationPerte::create(sampleRow(['numero_attestation' => 'DUP-1']));

check($validate(sampleRow(['numero_attestation' => 'NEW-1'])) === [], 'valid payload passes');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'date_declaration' => '']))['date_declaration']), 'missing date_declaration rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'date_declaration' => '23/08/2026']))['date_declaration']), 'invalid date_declaration format rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'heure_declaration' => '']))['heure_declaration']), 'missing heure_declaration rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'heure_declaration' => '25:00']))['heure_declaration']), 'invalid heure_declaration rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'identite_declarant' => ' ']))['identite_declarant']), 'blank identite_declarant rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'nature_objet' => '']))['nature_objet']), 'blank nature_objet rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'description_objet' => '']))['description_objet']), 'blank description_objet rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'date_perte' => '']))['date_perte']), 'missing date_perte rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'date_perte' => '2026-13-40']))['date_perte']), 'invalid date_perte rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'lieu_perte' => '']))['lieu_perte']), 'blank lieu_perte rejected');
check(isset($validate(sampleRow(['numero_attestation' => '']))['numero_attestation']), 'blank numero_attestation rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'NEW-1', 'nom_agent' => '']))['nom_agent']), 'blank nom_agent rejected');
check(isset($validate(sampleRow(['numero_attestation' => 'DUP-1']))['numero_attestation']), 'duplicate numero_attestation rejected');
check($validate(sampleRow(['numero_attestation' => 'DUP-1']), false, $dupId) === [], 'update excluding self passes uniqueness');

// Partial update: only provided fields are validated
check($validate(['lieu_perte' => 'Nouveau lieu'], false, $dupId) === [], 'partial update with valid field passes');
check(isset($validate(['lieu_perte' => ''], false, $dupId)['lieu_perte']), 'partial update with blank field rejected');

// --- Notification flow (mirrors the controller's notifyChange) -----------------
echo "Notifications\n";

function createRole(string $code, string $name): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO roles (code, name) VALUES (?, ?)');
    $stmt->execute([$code, $name]);
    return (int) $db->lastInsertId();
}

function createPersonnelRow(string $firstname): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO personnel (im, grade, lastname, firstname, affectation) VALUES (?, ?, ?, ?, ?)');
    $stmt->execute(['IM-' . uniqid(), 'Agent', 'Test', $firstname, 'Sédentaire']);
    return (int) $db->lastInsertId();
}

function createUserRow(int $personnelId, string $username, int $roleId): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO users (personnel_id, username, password_hash, role_id, is_active) VALUES (?, ?, ?, ?, 1)');
    $stmt->execute([$personnelId, $username, 'hash', $roleId]);
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
$secretaireRoleId = createRole('SECRETAIRE', 'Secrétaire');
$agentRoleId = createRole('AGENT_PJ', 'Agent Police Judiciaire');

setPermissionRow($secretaireRoleId, 'sedentaire_secretariat_declaration_perte');
setPermissionRow($agentRoleId, 'personnel');

$admin1 = createUserRow(createPersonnelRow('Admin1'), 'admin1', $adminRoleId);
$sec1   = createUserRow(createPersonnelRow('Sec1'),   'sec1',   $secretaireRoleId);
$agent1 = createUserRow(createPersonnelRow('Agent1'), 'agent1', $agentRoleId);

// A declaration created by sec1: admins + other module users are notified,
// the actor is excluded, users without the module permission get nothing.
$declaration = DeclarationPerte::getById($id);
$link = '/sedentaire/secretariat/declaration-perte/' . $declaration['id'];

Notification::notifyFeatureChange('sedentaire_secretariat_declaration_perte', [
    'title'   => 'Nouvelle déclaration de perte',
    'message' => "Attestation: {$declaration['numero_attestation']}",
    'type'    => 'info',
    'service' => 'Sedentaire',
    'link'    => $link,
], [
    'title'   => 'Nouvelle déclaration de perte',
    'message' => "Attestation: {$declaration['numero_attestation']} — Veuillez en prendre connaissance.",
    'type'    => 'info',
    'service' => 'Sedentaire',
    'link'    => $link,
], $sec1);

check(countNotificationsForUser($sec1) === 0, 'actor (sec1) received 0 notifications');
check(countNotificationsForUser($admin1) === 1, 'admin notified on create');
check(countNotificationsForUser($agent1) === 0, 'user without module permission received 0');

// markAsReadByLink auto-dismisses once the detail is opened (show()).
check(Notification::markAsReadByLink($link, $admin1) === 1, 'markAsReadByLink dismissed the admin notification');

// --- Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
