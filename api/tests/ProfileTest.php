<?php

/**
 * Self-service profile tests — AuthValidator::validateProfileUpdate and the
 * personnel fields backing PUT /api/auth/profile (no PHPUnit required).
 *
 * Usage: php api/tests/ProfileTest.php
 *
 * Creates a scratch database (opus_test_profile), applies the migrations
 * needed for roles/personnel/users (+ the email column), exercises the
 * validator and Personnel/User models, then drops the scratch database.
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
$scratch = 'opus_test_profile';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

// --- Apply migrations in order ----------------------------------------------
$migrations = [
    '001_create_roles.sql',
    '002_create_personnel.sql',
    '003_create_users.sql',
    '007_add_signature_svg.sql',
    '012_add_thumbnail_personnel.sql',
    '026_add_personnel_code_secret.sql',
    '067_add_email_personnel.sql',
];
foreach ($migrations as $file) {
    $sql = file_get_contents($root . '/database/' . $file);
    foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
        if (preg_match('/^\s*(CREATE|ALTER|INSERT)/i', preg_replace('/^--.*$/m', '', $stmt))) {
            $pdo->exec($stmt);
        }
    }
}

// User::getById runs a subquery against mouvement_personnel — create a minimal
// stub so the join succeeds.
$pdo->exec('CREATE TABLE mouvement_personnel (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, personnel_id INT UNSIGNED NULL, type_mouvement VARCHAR(100) NULL, retour VARCHAR(10) NULL DEFAULT "Non", created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

// Point the app's Database singleton at the scratch DB.
putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

set_exception_handler(function (Throwable $e) {
    fwrite(STDERR, 'ERROR: ' . $e->getMessage() . "\n");
    exit(1);
});

use App\Models\Personnel;
use App\Models\User;
use App\Validators\AuthValidator;

// ─── Fixture ────────────────────────────────────────────────────────────────
$db = \App\Database::getInstance()->getConnection();
$db->prepare('INSERT INTO roles (code, name) VALUES (?, ?)')->execute(['AGENT', 'Agent']);
$roleId = (int) $db->lastInsertId();

$personnelId = Personnel::create([
    'im' => 'IM-001',
    'grade' => 'Agent',
    'lastname' => 'Rabe',
    'firstname' => 'Armelin',
    'affectation' => 'Sédentaire',
    'phone' => '034 00 000 00',
]);

$db->prepare('INSERT INTO users (personnel_id, username, password_hash, role_id, is_active) VALUES (?, ?, ?, ?, 1)')
    ->execute([$personnelId, 'armelin', password_hash('secret', PASSWORD_BCRYPT), $roleId]);
$userId = (int) $db->lastInsertId();

// ═══════════════════════════════════════════════════════════════════════════
echo "\nValidator — validateProfileUpdate\n";

check(
    AuthValidator::validateProfileUpdate(['lastname' => 'Rabe', 'firstname' => 'Armelin']) === [],
    'valid names pass with no errors'
);
check(
    isset(AuthValidator::validateProfileUpdate(['firstname' => 'Armelin'])['lastname']),
    'missing lastname is rejected'
);
check(
    isset(AuthValidator::validateProfileUpdate(['lastname' => 'Rabe'])['firstname']),
    'missing firstname is rejected'
);
check(
    isset(AuthValidator::validateProfileUpdate(['lastname' => '  ', 'firstname' => 'Armelin'])['lastname']),
    'blank lastname is rejected'
);
check(
    isset(AuthValidator::validateProfileUpdate(['lastname' => 'Rabe', 'firstname' => 'A', 'email' => 'not-an-email'])['email']),
    'invalid email is rejected'
);
check(
    AuthValidator::validateProfileUpdate(['lastname' => 'Rabe', 'firstname' => 'A', 'email' => 'agent@police.mg']) === [],
    'valid email passes'
);
check(
    AuthValidator::validateProfileUpdate(['lastname' => 'Rabe', 'firstname' => 'A', 'email' => '']) === [],
    'empty email (clearing the field) passes'
);

// ═══════════════════════════════════════════════════════════════════════════
echo "\nModel — profile fields persist through Personnel::update\n";

Personnel::update($personnelId, [
    'lastname'  => 'Rakoto',
    'firstname' => 'Armelin Jean',
    'phone'     => '033 11 222 33',
    'email'     => 'armelin.rakoto@police.mg',
    'address'   => 'Antananarivo',
]);

$person = Personnel::getById($personnelId);
check($person['lastname'] === 'Rakoto', 'lastname updated');
check($person['firstname'] === 'Armelin Jean', 'firstname updated');
check($person['phone'] === '033 11 222 33', 'phone updated');
check($person['email'] === 'armelin.rakoto@police.mg', 'email updated');
check($person['address'] === 'Antananarivo', 'address updated');
check($person['im'] === 'IM-001' && $person['grade'] === 'Agent', 'administrative fields untouched');

// Clearing an optional field stores NULL
Personnel::update($personnelId, ['email' => null]);
check(Personnel::getById($personnelId)['email'] === null, 'cleared email is stored as NULL');

// ═══════════════════════════════════════════════════════════════════════════
echo "\nModel — User::getById exposes the profile fields used by /auth/me\n";

$user = User::getById($userId);
check($user !== null, 'user found');
check($user['email'] === null, 'email is surfaced on the user payload (null when unset)');
check($user['phone'] === '033 11 222 33', 'phone is surfaced on the user payload');
check(array_key_exists('password_hash', $user), 'raw row contains hash (controller unsets it before responding)');

// ═══════════════════════════════════════════════════════════════════════════
echo "\nDone. $failures failure(s).\n";
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
exit($failures > 0 ? 1 : 0);
