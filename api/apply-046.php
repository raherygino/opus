<?php
/**
 * Apply migration 046_create_plainte_sequence.sql to the dev database.
 *
 * Usage: php api/apply-046.php
 */

require_once __DIR__ . '/src/Database.php';

use App\Database;

$sql = file_get_contents(__DIR__ . '/../database/046_create_plainte_sequence.sql');
if ($sql === false) {
    fwrite(STDERR, "Cannot read database/046_create_plainte_sequence.sql\n");
    exit(1);
}

$db = Database::getInstance()->getConnection();

// Remove comment lines, then split on semicolons.
$lines = explode("\n", $sql);
$codeLines = array_filter($lines, fn($l) => !preg_match('/^\s*--/', $l));
$cleanSql = implode("\n", $codeLines);
$statements = array_filter(array_map('trim', explode(';', $cleanSql)));
$executed = 0;
foreach ($statements as $stmt) {
    if ($stmt === '') {
        continue;
    }
    try {
        $db->exec($stmt);
        $executed++;
        echo "OK: " . substr(preg_replace('/\s+/', ' ', $stmt), 0, 80) . "...\n";
    } catch (\Throwable $e) {
        fwrite(STDERR, "ERROR: " . $e->getMessage() . "\n");
        fwrite(STDERR, "Statement: " . $stmt . "\n");
        exit(1);
    }
}

echo "\nMigration 046 applied successfully ($executed statement(s)).\n";
