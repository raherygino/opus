<?php

/**
 * Notification system tests — self-notification prevention, dynamic
 * role-based recipient selection, and deduplication (no PHPUnit required).
 *
 * Usage: php api/tests/NotificationTest.php
 *
 * Creates a scratch database (opus_test_notifications), applies the
 * migrations needed for users/roles/permissions/notifications, exercises
 * the Notification model helpers, then drops the scratch database.
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
$scratch = 'opus_test_notifications';
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

use App\Models\Notification;

// ─── Test fixture helpers ───────────────────────────────────────────────────

function createRole(string $code, string $name): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO roles (code, name) VALUES (?, ?)');
    $stmt->execute([$code, $name]);
    return (int) $db->lastInsertId();
}

function createPersonnel(string $firstname): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO personnel (im, grade, lastname, firstname, affectation) VALUES (?, ?, ?, ?, ?)');
    $stmt->execute(['IM-' . uniqid(), 'Agent', 'Test', $firstname, 'Sédentaire']);
    return (int) $db->lastInsertId();
}

function createUser(int $personnelId, string $username, int $roleId): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('INSERT INTO users (personnel_id, username, password_hash, role_id, is_active) VALUES (?, ?, ?, ?, 1)');
    $stmt->execute([$personnelId, $username, 'hash', $roleId]);
    return (int) $db->lastInsertId();
}

function setPermission(int $roleId, string $module, int $canView = 1, int $canCreate = 0, int $canEdit = 0): void
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare(
        'INSERT INTO role_permissions (role_id, module, can_view, can_create, can_edit, can_delete, can_export)
         VALUES (?, ?, ?, ?, ?, 0, 0)'
    );
    $stmt->execute([$roleId, $module, $canView, $canCreate, $canEdit]);
}

function countNotificationsForUser(int $userId): int
{
    $db = \App\Database::getInstance()->getConnection();
    $stmt = $db->prepare('SELECT COUNT(*) FROM notifications WHERE user_id = ?');
    $stmt->execute([$userId]);
    return (int) $stmt->fetchColumn();
}

function countAllNotifications(): int
{
    $db = \App\Database::getInstance()->getConnection();
    return (int) $db->query('SELECT COUNT(*) FROM notifications')->fetchColumn();
}

function clearNotifications(): void
{
    $db = \App\Database::getInstance()->getConnection();
    $db->exec('DELETE FROM notifications');
}

// ─── Build test fixture ─────────────────────────────────────────────────────
//
// Roles:
//   SUPER_ADMIN  — admin role
//   STATION_ADMIN — admin role
//   SECRETAIRE   — regular role with can_view on "personnel" and
//                  "sedentaire_secretariat_correspondance" (view only)
//   AGENT_PJ     — regular role with can_view on "personnel" only
//   CORRESPONDANT — different regular role with can_view + can_create +
//                   can_edit on "sedentaire_secretariat_correspondance"
//                   (tests "different roles, same feature" peer-to-peer)
//
// Users:
//   admin1   → SUPER_ADMIN
//   admin2   → STATION_ADMIN
//   sec1     → SECRETAIRE      (view-only on correspondance)
//   sec2     → SECRETAIRE      (view-only on correspondance, same role as sec1)
//   agent1   → AGENT_PJ        (personnel only, no correspondance)
//   corr1    → CORRESPONDANT   (can add/edit correspondance)

$adminRoleId    = createRole('SUPER_ADMIN', 'Super Administrator');
$stationAdminId = createRole('STATION_ADMIN', 'Station Administrator');
$secretaireId   = createRole('SECRETAIRE', 'Secrétaire');
$agentPjId      = createRole('AGENT_PJ', 'Agent Police Judiciaire');
$correspondantId = createRole('CORRESPONDANT', 'Correspondant');

setPermission($secretaireId, 'personnel');
setPermission($secretaireId, 'sedentaire_secretariat_correspondance');
setPermission($agentPjId, 'personnel');
setPermission($correspondantId, 'sedentaire_secretariat_correspondance', 1, 1, 1);

$admin1 = createUser(createPersonnel('Admin1'), 'admin1', $adminRoleId);
$admin2 = createUser(createPersonnel('Admin2'), 'admin2', $stationAdminId);
$sec1   = createUser(createPersonnel('Sec1'),   'sec1',   $secretaireId);
$sec2   = createUser(createPersonnel('Sec2'),   'sec2',   $secretaireId);
$agent1 = createUser(createPersonnel('Agent1'), 'agent1', $agentPjId);
$corr1  = createUser(createPersonnel('Corr1'),  'corr1',  $correspondantId);

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 1: Admin changes something → the admin does NOT receive their own
// notification.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 1: Admin does not receive own notification\n";
clearNotifications();

Notification::notifyAdmins([
    'title'   => 'Test S1',
    'message' => 'Admin self-notification test',
    'type'    => 'info',
    'service' => 'System',
], $admin1);

check(countNotificationsForUser($admin1) === 0, 'admin1 (actor) received 0 notifications');
check(countNotificationsForUser($admin2) === 1, 'admin2 (other admin) received 1 notification');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 2: Admin changes something → all affected users receive the
// notification.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 2: All affected users receive notification\n";
clearNotifications();

// Admin adds personnel → notify admins + module users with "personnel" perm
Notification::notifyAdmins([
    'title'   => 'Test S2-admin',
    'message' => 'Admin→admin test',
    'type'    => 'info',
    'service' => 'System',
], $admin1);

Notification::notifyModuleUsers('personnel', [
    'title'   => 'Test S2-user',
    'message' => 'Admin→user test',
    'type'    => 'info',
    'service' => 'System',
], $admin1);

// admin2 is the other admin → should get the admin notification
check(countNotificationsForUser($admin2) === 1, 'admin2 received admin notification');
// sec1, sec2, agent1 all have can_view on "personnel" → should each get 1
check(countNotificationsForUser($sec1) === 1, 'sec1 received module-user notification');
check(countNotificationsForUser($sec2) === 1, 'sec2 received module-user notification');
check(countNotificationsForUser($agent1) === 1, 'agent1 received module-user notification');
// admin1 is the actor → should get 0
check(countNotificationsForUser($admin1) === 0, 'admin1 (actor) received 0');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 3: Two users share the same relevant role → both receive the
// notification.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 3: Two users share role → both notified\n";
clearNotifications();

// Simulate: admin changes Correspondant data → notify users with
// "sedentaire_secretariat_correspondance" permission (sec1 and sec2 both
// have SECRETAIRE role with that permission).
Notification::notifyModuleUsers('sedentaire_secretariat_correspondance', [
    'title'   => 'Test S3',
    'message' => 'Correspondance change test',
    'type'    => 'info',
    'service' => 'Sedentaire',
], $admin1);

check(countNotificationsForUser($sec1) === 1, 'sec1 (SECRETAIRE) received notification');
check(countNotificationsForUser($sec2) === 1, 'sec2 (SECRETAIRE) received notification');
// agent1 does NOT have correspondance permission → should get 0
check(countNotificationsForUser($agent1) === 0, 'agent1 (AGENT_PJ, no correspondance perm) received 0');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 4: A user has multiple relevant roles → they receive only ONE
// notification for the same change.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 4: Deduplication — one notification per user per change\n";
clearNotifications();

// notifyModuleUsers with multiple modules: sec1's role (SECRETAIRE) has
// can_view on BOTH "personnel" and "sedentaire_secretariat_correspondance".
// Even though both modules are passed, sec1 should receive exactly ONE
// notification (deduplicated via getUsersWithAnyModulePermission + DISTINCT).
Notification::notifyModuleUsers(
    ['personnel', 'sedentaire_secretariat_correspondance'],
    [
        'title'   => 'Test S4',
        'message' => 'Dedup test',
        'type'    => 'info',
        'service' => 'System',
    ],
    $admin1
);

check(countNotificationsForUser($sec1) === 1, 'sec1 received exactly 1 (not 2) despite matching both modules');
check(countNotificationsForUser($sec2) === 1, 'sec2 received exactly 1 (not 2) despite matching both modules');
check(countNotificationsForUser($agent1) === 1, 'agent1 received 1 (matches "personnel" only)');

// Also test notifyRecipients with duplicate IDs in the input array
clearNotifications();
Notification::notifyRecipients(
    [$sec1, $sec1, $sec1, $sec2, $sec2],
    [
        'title'   => 'Test S4b',
        'message' => 'Dedup input test',
        'type'    => 'info',
        'service' => 'System',
    ],
    $admin1
);
check(countNotificationsForUser($sec1) === 1, 'sec1 received 1 even with duplicate IDs in input');
check(countNotificationsForUser($sec2) === 1, 'sec2 received 1 even with duplicate IDs in input');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 5: A regular user makes a change → they do NOT receive their own
// notification, while the appropriate other users/admins still do.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 5: Regular user change → no self-notification, others notified\n";
clearNotifications();

// sec1 (regular user) makes a change → notify admins (user→admin flow)
Notification::notifyAdmins([
    'title'   => 'Test S5',
    'message' => 'User→admin test',
    'type'    => 'warning',
    'service' => 'System',
], $sec1);

check(countNotificationsForUser($sec1) === 0, 'sec1 (actor) received 0');
check(countNotificationsForUser($admin1) === 1, 'admin1 received notification from sec1');
check(countNotificationsForUser($admin2) === 1, 'admin2 received notification from sec1');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 6 (bonus): Source-level guard — Notification::create refuses to
// create a self-notification even if called directly.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 6: Source-level self-notification guard in create()\n";
clearNotifications();

$result = Notification::create([
    'title'      => 'Self-notif test',
    'message'    => 'This should be blocked',
    'type'       => 'info',
    'service'    => 'System',
    'user_id'    => $sec1,
    'created_by' => $sec1,  // same user → must be blocked
]);
check($result === 0, 'create() returned 0 for self-notification');
check(countNotificationsForUser($sec1) === 0, 'no notification row created for self-notification');

// Verify a normal (non-self) create still works
$result2 = Notification::create([
    'title'      => 'Normal test',
    'message'    => 'This should work',
    'type'       => 'info',
    'service'    => 'System',
    'user_id'    => $sec2,
    'created_by' => $sec1,  // different users → OK
]);
check($result2 > 0, 'create() returned >0 for valid cross-user notification');
check(countNotificationsForUser($sec2) === 1, 'sec2 received the valid notification');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 7 (bonus): excludeIds prevents duplicates when combining targeted
// + broadcast notifications (comportement confirm/reject pattern).
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 7: excludeIds prevents duplicate for targeted+broadcast\n";
clearNotifications();

// Simulate comportement confirm: creator (sec1) gets targeted notification,
// then admins get broadcast with sec1 excluded to avoid duplicate.
Notification::create([
    'title'      => 'Confirmed',
    'message'    => 'Your record was confirmed',
    'type'       => 'success',
    'service'    => 'System',
    'user_id'    => $sec1,
    'created_by' => $admin1,
]);
// Broadcast to admins, excluding sec1 (the creator)
Notification::notifyAdmins([
    'title'   => 'Confirmed (admin)',
    'message' => 'A record was confirmed',
    'type'    => 'success',
    'service' => 'System',
], $admin1, [$sec1]);

check(countNotificationsForUser($sec1) === 1, 'sec1 received exactly 1 (targeted only, not duplicated by broadcast)');
check(countNotificationsForUser($admin2) === 1, 'admin2 received the admin broadcast');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 8 (bonus): getUsersByRoleId returns dynamic recipients for
// role-change notifications.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 8: getUsersByRoleId — dynamic role-based recipients\n";
clearNotifications();

// Admin changes SECRETAIRE role → notify all users with that role
$usersWithRole = Notification::getUsersByRoleId($secretaireId);
$recipientIds = array_map(fn($u) => (int) $u['id'], $usersWithRole);
check(count($recipientIds) === 2, 'getUsersByRoleId returned 2 users for SECRETAIRE');
check(in_array($sec1, $recipientIds, true), 'sec1 is in SECRETAIRE recipients');
check(in_array($sec2, $recipientIds, true), 'sec2 is in SECRETAIRE recipients');

Notification::notifyRecipients($recipientIds, [
    'title'   => 'Role changed',
    'message' => 'Your role was modified',
    'type'    => 'info',
    'service' => 'System',
], $admin1);

check(countNotificationsForUser($sec1) === 1, 'sec1 notified of role change');
check(countNotificationsForUser($sec2) === 1, 'sec2 notified of role change');
check(countNotificationsForUser($admin1) === 0, 'admin1 (actor) not notified of role change');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 9: Peer-to-peer — a regular user (corr1, CORRESPONDANT role)
// adds a correspondance. User sec1 (SECRETAIRE role, view-only on
// correspondance) should receive a notification because they have view
// permission on the same feature, even though they have a DIFFERENT role
// and only view (not add/edit) permission.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 9: Peer-to-peer — different roles, same feature permission\n";
clearNotifications();

// corr1 (CORRESPONDANT, can add/edit) makes a change in correspondance.
// notifyFeatureChange sends to admins + all users with correspondance view.
Notification::notifyFeatureChange('sedentaire_secretariat_correspondance', [
    'title'   => 'Nouvelle correspondance',
    'message' => 'Correspondance enregistrée.',
    'type'    => 'info',
    'service' => 'Sedentaire',
], [
    'title'   => 'Nouvelle correspondance',
    'message' => 'Correspondance enregistrée. Veuillez en prendre connaissance.',
    'type'    => 'info',
    'service' => 'Sedentaire',
], $corr1);

// corr1 is the actor → 0
check(countNotificationsForUser($corr1) === 0, 'corr1 (actor) received 0');
// sec1 has view on correspondance (different role) → should be notified
check(countNotificationsForUser($sec1) === 1, 'sec1 (different role, view perm) received notification');
// sec2 has view on correspondance (same role as sec1) → should be notified
check(countNotificationsForUser($sec2) === 1, 'sec2 (different role, view perm) received notification');
// agent1 has NO correspondance permission → should NOT be notified
check(countNotificationsForUser($agent1) === 0, 'agent1 (no correspondance perm) received 0');
// admins still receive (existing admin flow preserved)
check(countNotificationsForUser($admin1) === 1, 'admin1 received admin notification');
check(countNotificationsForUser($admin2) === 1, 'admin2 received admin notification');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 10: Peer-to-peer — a regular user (sec1) edits personnel.
// Other peers with personnel view (sec2, agent1) AND admins should be
// notified. sec1 (actor) should NOT.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 10: Peer-to-peer — regular user edits personnel\n";
clearNotifications();

Notification::notifyFeatureChange('personnel', [
    'title'   => 'Personnel modifié',
    'message' => 'Personnel modifié.',
    'type'    => 'info',
    'service' => 'System',
], [
    'title'   => 'Personnel modifié',
    'message' => 'Personnel modifié. Veuillez en prendre connaissance.',
    'type'    => 'info',
    'service' => 'System',
], $sec1);

check(countNotificationsForUser($sec1) === 0, 'sec1 (actor) received 0');
check(countNotificationsForUser($sec2) === 1, 'sec2 (peer, personnel view) received notification');
check(countNotificationsForUser($agent1) === 1, 'agent1 (peer, personnel view) received notification');
check(countNotificationsForUser($admin1) === 1, 'admin1 received admin notification');
check(countNotificationsForUser($admin2) === 1, 'admin2 received admin notification');
// corr1 has NO personnel permission → should NOT be notified
check(countNotificationsForUser($corr1) === 0, 'corr1 (no personnel perm) received 0');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 11: notifyFeatureChange deduplication — a user who is BOTH an
// admin AND has module permission should receive only ONE notification.
// (This can't happen with the current role structure since admin roles are
// excluded from module-user queries, but verify the contract anyway by
// passing overlapping IDs through notifyRecipients directly.)
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 11: notifyFeatureChange — no duplicates across admin+user groups\n";
clearNotifications();

// notifyFeatureChange calls notifyAdmins (admin1, admin2) then
// notifyModuleUsers (sec1, sec2, agent1 for personnel). These groups are
// mutually exclusive by design (admin roles excluded from module query),
// so no user should receive more than 1.
Notification::notifyFeatureChange('personnel', [
    'title'   => 'Test S11',
    'message' => 'Dedup test admin',
    'type'    => 'info',
    'service' => 'System',
], [
    'title'   => 'Test S11',
    'message' => 'Dedup test user',
    'type'    => 'info',
    'service' => 'System',
], $admin1);

check(countNotificationsForUser($admin1) === 0, 'admin1 (actor) received 0');
check(countNotificationsForUser($admin2) === 1, 'admin2 received exactly 1');
check(countNotificationsForUser($sec1) === 1, 'sec1 received exactly 1');
check(countNotificationsForUser($sec2) === 1, 'sec2 received exactly 1');
check(countNotificationsForUser($agent1) === 1, 'agent1 received exactly 1');

// ═══════════════════════════════════════════════════════════════════════════
// Scenario 12: notifyFeatureChange with excludeIds (comportement confirm
// pattern) — creator gets targeted message, broadcast excludes creator.
// ═══════════════════════════════════════════════════════════════════════════
echo "\nScenario 12: notifyFeatureChange with excludeIds\n";
clearNotifications();

// sec1 creates a comportement → admin confirms it.
// Targeted notification to sec1 (creator), then broadcast to everyone else
// with sec1 excluded.
Notification::create([
    'title'      => 'Confirmed',
    'message'    => 'Your record was confirmed',
    'type'       => 'success',
    'service'    => 'System',
    'user_id'    => $sec1,
    'created_by' => $admin1,
]);
Notification::notifyFeatureChange('personnel', [
    'title'   => 'Confirmed (admin)',
    'message' => 'A record was confirmed',
    'type'    => 'success',
    'service' => 'System',
], [
    'title'   => 'Confirmed (user)',
    'message' => 'A record was confirmed. Veuillez en prendre connaissance.',
    'type'    => 'success',
    'service' => 'System',
], $admin1, [$sec1]);

check(countNotificationsForUser($sec1) === 1, 'sec1 (creator) received exactly 1 (targeted only)');
check(countNotificationsForUser($sec2) === 1, 'sec2 (peer) received broadcast');
check(countNotificationsForUser($agent1) === 1, 'agent1 (peer) received broadcast');
check(countNotificationsForUser($admin2) === 1, 'admin2 received admin broadcast');
check(countNotificationsForUser($admin1) === 0, 'admin1 (actor) received 0');

// ─── Teardown -------------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

echo "\n" . ($failures === 0 ? 'All tests passed' : "$failures test(s) FAILED") . "\n";
exit($failures === 0 ? 0 : 1);
