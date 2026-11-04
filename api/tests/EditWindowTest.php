<?php

/**
 * EditWindow (24h edit lock) tests (no PHPUnit required).
 *
 * Usage: php api/tests/EditWindowTest.php
 *
 * Creates a scratch database (opus_test_edit_window) with a minimal
 * correspondance table plus users/roles, exercises
 * App\Helpers\EditWindow::isWithinWindow and User::isAdminUser, then drives
 * assertEditable end-to-end in child PHP processes to verify the 403 path.
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
$scratch = 'opus_test_edit_window';
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");
$pdo->exec("CREATE DATABASE `$scratch` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `$scratch`");

$pdo->exec('CREATE TABLE roles (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, code VARCHAR(50) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE users (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NULL, role_id INT UNSIGNED NULL, personnel_id INT UNSIGNED NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
$pdo->exec('CREATE TABLE correspondance (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reference VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');

// Seed roles + users
$pdo->exec("INSERT INTO roles (code) VALUES ('SUPER_ADMIN'), ('STATION_ADMIN'), ('CLERK')");
$pdo->exec("INSERT INTO users (username, role_id) VALUES ('boss', 1), ('station', 2), ('clerk', 3)");
$adminId = 1;
$stationAdminId = 2;
$clerkId = 3;

// Seed records: one fresh, one created 25 hours ago
$pdo->exec("INSERT INTO correspondance (reference) VALUES ('FRESH-1')");
$freshId = (int) $pdo->lastInsertId();
$pdo->exec("INSERT INTO correspondance (reference, created_at) VALUES ('OLD-1', NOW() - INTERVAL 25 HOUR)");
$oldId = (int) $pdo->lastInsertId();
$pdo->exec("INSERT INTO correspondance (reference, created_at) VALUES ('EDGE-1', NOW() - INTERVAL 23 HOUR)");
$edgeId = (int) $pdo->lastInsertId();

putenv("DB_NAME=$scratch");
require $root . '/api/config/bootstrap.php';

use App\Helpers\EditWindow;
use App\Models\User;

echo "isWithinWindow\n";
check(EditWindow::isWithinWindow('correspondance', $freshId) === true, 'fresh record editable');
check(EditWindow::isWithinWindow('correspondance', $edgeId) === true, '23h-old record still editable');
check(EditWindow::isWithinWindow('correspondance', $oldId) === false, '25h-old record locked');
check(EditWindow::isWithinWindow('correspondance', 999999) === true, 'missing record passes through');

echo "Admin detection\n";
check(User::isAdminUser($adminId) === true, 'SUPER_ADMIN is admin');
check(User::isAdminUser($stationAdminId) === true, 'STATION_ADMIN is admin');
check(User::isAdminUser($clerkId) === false, 'CLERK is not admin');

// --- assertEditable end-to-end (child processes, since it exits) -------------
function runAssert(string $root, string $scratch, int $recordId, int $userId): array
{
    $code = <<<PHP
<?php
putenv('DB_NAME=$scratch');
require '$root/api/config/bootstrap.php';
App\\Helpers\\EditWindow::assertEditable('correspondance', $recordId, ['sub' => $userId]);
echo json_encode(['passed' => true]);
PHP;
    $tmp = tempnam(sys_get_temp_dir(), 'ew_') . '.php';
    file_put_contents($tmp, $code);
    $out = [];
    $rc = 0;
    exec('php ' . escapeshellarg($tmp) . ' 2>&1', $out, $rc);
    unlink($tmp);
    return [implode("\n", $out), $rc];
}

echo "assertEditable\n";
[$out, $rc] = runAssert($root, $scratch, $freshId, $clerkId);
check(str_contains($out, '"passed":true'), 'non-admin can edit fresh record');

[$out, $rc] = runAssert($root, $scratch, $oldId, $clerkId);
check(str_contains($out, '24 heures') || str_contains($out, 'lecture seule'), 'non-admin blocked on expired record (403)');

[$out, $rc] = runAssert($root, $scratch, $oldId, $adminId);
check(str_contains($out, '"passed":true'), 'SUPER_ADMIN bypasses expired record');

[$out, $rc] = runAssert($root, $scratch, $oldId, $stationAdminId);
check(str_contains($out, '"passed":true'), 'STATION_ADMIN bypasses expired record');

// --- Teardown ----------------------------------------------------------------
$pdo->exec("DROP DATABASE IF EXISTS `$scratch`");

if ($failures > 0) {
    echo "\n$failures test(s) failed\n";
    exit(1);
}
echo "\nAll tests passed\n";
