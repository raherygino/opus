<?php

/**
 * Database migration runner — GET or POST /migrate.php?key=YOUR_KEY
 *
 * Runs every database/*.sql file in filename order against the configured
 * database (same credentials/env as the API). NON-destructive: it does not
 * drop or create the database — it only executes the migration files.
 *
 * ⚠️  Set MIGRATE_KEY below before uploading, and DELETE this file from the
 *     server after running it — anyone who can reach it can alter the schema.
 *     The /database folder must also be uploaded next to /api.
 */

// Simple shared-secret gate — change this value before deploying.
const MIGRATE_KEY = 'change-me';

set_time_limit(0);
ini_set('display_errors', '0');

// Plain text output — readable in browser, curl or CLI.
header('Content-Type: text/plain; charset=utf-8');

if (PHP_SAPI !== 'cli') {
    $key = $_GET['key'] ?? ($_POST['key'] ?? '');
    if (!hash_equals(MIGRATE_KEY, $key)) {
        http_response_code(403);
        exit("❌ Forbidden — pass ?key=... (set MIGRATE_KEY in this file first)\n");
    }
}

// Load api/.env so production credentials apply.
require __DIR__ . '/../config/bootstrap.php';

$dbConfig = require __DIR__ . '/../config/database.php';

echo "🔧 OPUS Migration Runner\n";
echo "========================\n\n";
flush();

$sqlDir = realpath(__DIR__ . '/../../database');
if (!$sqlDir) {
    http_response_code(500);
    exit("❌ /database directory not found (expected next to /api)\n");
}

$files = glob($sqlDir . DIRECTORY_SEPARATOR . '*.sql');
sort($files);

if (empty($files)) {
    http_response_code(500);
    exit("❌ No .sql files found in $sqlDir\n");
}

// Connect with the database already selected (no CREATE/DROP — the host
// provides the database and users typically can't create schemas).
try {
    $dsn = sprintf(
        'mysql:host=%s;port=%s;dbname=%s;charset=%s',
        $dbConfig['host'],
        $dbConfig['port'],
        $dbConfig['dbname'],
        $dbConfig['charset']
    );
    $pdo = new PDO($dsn, $dbConfig['username'], $dbConfig['password'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    ]);
    echo "✅ Connected to {$dbConfig['dbname']} @ {$dbConfig['host']}:{$dbConfig['port']}\n\n";
} catch (PDOException $e) {
    http_response_code(500);
    exit("❌ Connection failed: " . $e->getMessage() . "\n");
}

flush();

$applied = 0;
$skipped = 0;
foreach ($files as $file) {
    $basename = basename($file);
    $sql = file_get_contents($file);
    if ($sql === false || trim($sql) === '') {
        echo "⚠️  Skipping empty file: $basename\n";
        $skipped++;
        continue;
    }

    $statements = splitSqlStatements($sql);
    if (empty($statements)) {
        echo "⚠️  Skipping $basename (no statements)\n";
        $skipped++;
        continue;
    }

    try {
        foreach ($statements as $stmt) {
            $pdo->exec($stmt);
        }
        echo "✅ $basename (" . count($statements) . " statements)\n";
        $applied++;
    } catch (PDOException $e) {
        http_response_code(500);
        echo "❌ $basename FAILED — " . $e->getMessage() . "\n";
        echo "\n⚠️  Stopped. Fix the migration or DB state, then re-run — files that already succeeded may fail with 'already exists' errors on re-run.\n";
        exit(1);
    }
    flush();
}

echo "\n========================\n";
echo "✅ Done — $applied file(s) applied, $skipped skipped\n";

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
