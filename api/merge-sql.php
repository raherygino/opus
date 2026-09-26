<?php

/**
 * Merge all database/*.sql migrations into a single importable SQL file.
 *
 * Usage:
 *   php api/merge-sql.php [output-file]
 *
 * Default output: <repo-root>/opus-db-full.sql
 *
 * The output intentionally lives OUTSIDE /database so migration runners
 * (reset-db.php, public/migrate.php) never pick it up on their next glob.
 * Import it on the server via phpMyAdmin or:
 *   mysql -u USER -p DBNAME < opus-db-full.sql
 */

$sqlDir = realpath(__DIR__ . '/../database');
if (!$sqlDir) {
    fwrite(STDERR, "❌ /database directory not found\n");
    exit(1);
}

$output = $argv[1] ?? dirname(__DIR__) . DIRECTORY_SEPARATOR . 'opus-db-full.sql';

$files = glob($sqlDir . DIRECTORY_SEPARATOR . '*.sql');
sort($files);

if (empty($files)) {
    fwrite(STDERR, "❌ No .sql files found in $sqlDir\n");
    exit(1);
}

$out = fopen($output, 'wb');
if (!$out) {
    fwrite(STDERR, "❌ Cannot write to $output\n");
    exit(1);
}

fwrite($out, "-- ============================================\n");
fwrite($out, "-- OPUS — merged database schema & seeds\n");
fwrite($out, "-- Generated " . date('c') . " from " . count($files) . " files in /database\n");
fwrite($out, "-- Import into an EMPTY database (files are not re-runnable).\n");
fwrite($out, "-- ============================================\n\n");
fwrite($out, "SET NAMES utf8mb4;\n");
fwrite($out, "SET FOREIGN_KEY_CHECKS = 0;\n\n");

$count = 0;
foreach ($files as $file) {
    $basename = basename($file);
    $sql = file_get_contents($file);
    if ($sql === false || trim($sql) === '') {
        echo "⚠️  Skipping empty file: $basename\n";
        continue;
    }

    // Normalise line endings so sections stay readable in the merged file.
    $sql = str_replace(["\r\n", "\r"], "\n", trim($sql));

    fwrite($out, "\n-- ============================================\n");
    fwrite($out, "-- $basename\n");
    fwrite($out, "-- ============================================\n\n");
    fwrite($out, $sql . "\n");
    $count++;
    echo "✅ $basename\n";
}

fwrite($out, "\nSET FOREIGN_KEY_CHECKS = 1;\n");
fclose($out);

$size = round(filesize($output) / 1024, 1);
echo "\n✅ Merged $count file(s) → $output ($size KB)\n";
