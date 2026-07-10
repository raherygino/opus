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

    // Split by semicolons to execute multi-statement files safely
    $statements = array_values(array_filter(
        array_map('trim', explode(';', $sql)),
        fn($s) => $s !== ''
    ));

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

echo "\n===========================\n";
echo "✅ Database reset complete — $count files executed\n";
