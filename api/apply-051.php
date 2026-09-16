<?php
/**
 * Apply migration 051_create_objet.sql to the dev database.
 *
 * Usage: php api/apply-051.php
 */

require_once __DIR__ . '/src/Database.php';

use App\Database;

$sql = file_get_contents(__DIR__ . '/../database/051_create_objet.sql');
if ($sql === false) {
    fwrite(STDERR, "Cannot read database/051_create_objet.sql\n");
    exit(1);
}

$lines = explode("\n", $sql);
$codeLines = array_filter($lines, fn($l) => !preg_match('/^\s*--/', $l));
$cleanSql = implode("\n", $codeLines);
$statements = array_filter(array_map('trim', explode(';', $cleanSql)));

try {
    $db = Database::getInstance()->getConnection();
    $count = 0;
    foreach ($statements as $stmt) {
        if ($stmt !== '') {
            $db->exec($stmt);
            $count++;
        }
    }
    echo "Migration 051 applied successfully ($count statement(s)).\n";
    exit(0);
} catch (Throwable $e) {
    fwrite(STDERR, "Migration 051 failed: " . $e->getMessage() . "\n");
    exit(1);
}
