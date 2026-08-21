<?php

/**
 * OPUS Database Reset Script
 *
 * Usage: php reset-db.php
 *
 * Drops and recreates the database, then runs all SQL migration
 * files from /database in order, followed by seed data.
 */

// --- Config (no autoloader needed) ---
$dbConfig = require __DIR__ . '/config/database.php';

$host     = $dbConfig['host'];
$port     = $dbConfig['port'];
$dbname   = $dbConfig['dbname'];
$username = $dbConfig['username'];
$password = $dbConfig['password'];
$charset  = $dbConfig['charset'];

$sqlDir = realpath(__DIR__ . '/../database');
if (!$sqlDir) {
    fwrite(STDERR, "❌ Database directory not found at /database\n");
    exit(1);
}

// --- Collect SQL files ---
$files = glob($sqlDir . DIRECTORY_SEPARATOR . '*.sql');
sort($files);

if (empty($files)) {
    fwrite(STDERR, "❌ No SQL files found in $sqlDir\n");
    exit(1);
}

echo "🔧 OPUS Database Reset Tool\n";
echo "===========================\n\n";

// --- Connect without database to drop/recreate ---
try {
    $dsn = "mysql:host=$host;port=$port;charset=$charset";
    $pdo = new PDO($dsn, $username, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    ]);
    echo "✅ Connected to MySQL ($host:$port)\n";
} catch (PDOException $e) {
    fwrite(STDERR, "❌ MySQL connection failed: " . $e->getMessage() . "\n");
    exit(1);
}

// --- Drop database if exists ---
try {
    $pdo->exec("DROP DATABASE IF EXISTS `$dbname`");
    echo "🗑️  Database '$dbname' dropped\n";
} catch (PDOException $e) {
    fwrite(STDERR, "❌ Failed to drop database: " . $e->getMessage() . "\n");
    exit(1);
}

// --- Create database ---
try {
    $pdo->exec("CREATE DATABASE `$dbname` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    echo "✅ Database '$dbname' created\n";
} catch (PDOException $e) {
    fwrite(STDERR, "❌ Failed to create database: " . $e->getMessage() . "\n");
    exit(1);
}

// --- Reconnect with database selected ---
try {
    $dsn = "mysql:host=$host;port=$port;dbname=$dbname;charset=$charset";
    $pdo = new PDO($dsn, $username, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    ]);
} catch (PDOException $e) {
    fwrite(STDERR, "❌ Failed to connect to database '$dbname': " . $e->getMessage() . "\n");
    exit(1);
}

// --- Run each SQL file ---
$count = 0;
foreach ($files as $file) {
    $basename = basename($file);
    $sql = file_get_contents($file);
    if ($sql === false || trim($sql) === '') {
        echo "⚠️  Skipping empty file: $basename\n";
        continue;
    }

    // Split into individual statements. Supports the DELIMITER directive so
    // that multi-statement constructs (triggers, stored procedures) which
    // contain semicolons inside their BEGIN...END body are kept intact.
    $statements = splitSqlStatements($sql);

    if (empty($statements)) {
        echo "⚠️  Skipping $basename (no statements)\n";
        continue;
    }

    $ok = true;
    foreach ($statements as $i => $stmt) {
        try {
            $pdo->exec($stmt);
        } catch (PDOException $e) {
            fwrite(STDERR, "❌ $basename (statement " . ($i + 1) . ") — " . $e->getMessage() . "\n");
            $ok = false;
            break;
        }
    }

    if ($ok) {
        echo "✅ $basename\n";
        $count++;
    } else {
        exit(1);
    }
}

/**
 * Split a SQL script into individual executable statements.
 *
 * Honors the `DELIMITER` directive (case-insensitive) so that triggers and
 * stored procedures — whose bodies contain semicolons — are returned as a
 * single statement. Lines starting with `--` are treated as comments and
 * stripped.
 *
 * @return string[]
 */
function splitSqlStatements(string $sql): array
{
    // Normalise line endings
    $sql = str_replace(["\r\n", "\r"], "\n", $sql);

    $delimiter = ';';
    $statements = [];
    $buffer = '';

    $lines = explode("\n", $sql);
    foreach ($lines as $line) {
        $trimmed = ltrim($line);

        // Skip full-line comments (but keep DELIMITER directives, which are
        // not prefixed with --).
        if ($trimmed !== '' && str_starts_with($trimmed, '--')) {
            continue;
        }

        // Detect a DELIMITER directive: "DELIMITER $$" changes the delimiter.
        if (preg_match('/^DELIMITER\s+(\S+)\s*$/i', $trimmed, $m)) {
            // Flush any buffered statement using the OLD delimiter first.
            if (trim($buffer) !== '') {
                $statements[] = trim($buffer);
                $buffer = '';
            }
            $delimiter = $m[1];
            continue;
        }

        $buffer .= ($buffer === '' ? '' : "\n") . $line;

        // If the line ends with the current delimiter, cut it off and flush.
        $pos = strripos($buffer, $delimiter);
        if ($pos !== false && $pos === strlen($buffer) - strlen($delimiter)) {
            $stmt = substr($buffer, 0, $pos);
            if (trim($stmt) !== '') {
                $statements[] = trim($stmt);
            }
            $buffer = '';
        }
    }

    if (trim($buffer) !== '') {
        $statements[] = trim($buffer);
    }

    return $statements;
}

echo "\n===========================\n";
echo "✅ Database reset complete — $count files executed\n";
