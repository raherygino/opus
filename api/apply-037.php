<?php
/**
 * Apply migration 037_create_materiel.sql to the dev DB.
 *
 * Usage: php api/apply-037.php
 */

$root = __DIR__ . '/..';
$config = require $root . '/api/config/database.php';

$pdo = new PDO(
    "mysql:host={$config['host']};port={$config['port']};dbname={$config['dbname']};charset={$config['charset']}",
    $config['username'],
    $config['password'],
    [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
);

$sql = file_get_contents($root . '/database/037_create_materiel.sql');
foreach (array_filter(array_map('trim', explode(';', $sql))) as $stmt) {
    if (preg_match('/^\s*(CREATE|ALTER)/i', preg_replace('/^--.*$/m', '', $stmt))) {
        $pdo->exec($stmt);
        echo "Executed: " . substr(trim(preg_replace('/\s+/', ' ', $stmt)), 0, 80) . "...\n";
    }
}

echo "Migration 037 applied successfully.\n";
