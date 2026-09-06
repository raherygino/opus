<?php

/**
 * OPUS — Seed personnel (demo agents)
 *
 * Applies ONLY database/036_seed_personnel.sql to the dev DB.
 * Non-destructive: does NOT drop/recreate the database. The seed file
 * itself uses INSERT ... ON DUPLICATE KEY UPDATE on the unique `im`
 * column so it is re-runnable.
 *
 * Usage (from the api/ folder):
 *   php seed-personnel.php
 *
 * Or with custom DB credentials via env:
 *   DB_HOST=127.0.0.1 DB_NAME=opus DB_USER=root DB_PASS= php seed-personnel.php
 */

// --- Config (no autoloader needed, mirrors reset-db.php) ---
$dbConfig = require __DIR__ . '/config/database.php';

$host     = getenv('DB_HOST') ?: $dbConfig['host'];
$port     = getenv('DB_PORT') ?: $dbConfig['port'];
$dbname   = getenv('DB_NAME') ?: $dbConfig['dbname'];
$username = getenv('DB_USER') ?: $dbConfig['username'];
$password = getenv('DB_PASS') ?: $dbConfig['password'];
$charset  = $dbConfig['charset'];

$seedFile = realpath(__DIR__ . '/../database/036_seed_personnel.sql');
if (!$seedFile || !is_readable($seedFile)) {
    fwrite(STDERR, "❌ Seed file not found: database/036_seed_personnel.sql\n");
    exit(1);
}

echo "🔧 OPUS — Seed personnel\n";
echo "=========================\n\n";
echo "📁 Seed file: " . basename($seedFile) . "\n";
echo "🗄️  Target DB:  $dbname @ $host:$port\n\n";

// --- Connect (database must already exist) ---
try {
    $dsn = "mysql:host=$host;port=$port;dbname=$dbname;charset=$charset";
    $pdo = new PDO($dsn, $username, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    ]);
    echo "✅ Connected to MySQL\n\n";
} catch (PDOException $e) {
    fwrite(STDERR, "❌ MySQL connection failed: " . $e->getMessage() . "\n");
    fwrite(STDERR, "   (Make sure the '$dbname' database exists — run api/reset-db.php first if needed.)\n");
    exit(1);
}

// --- Pre-flight: confirm the target table exists ---
try {
    $pdo->query("SELECT 1 FROM `personnel` LIMIT 1");
} catch (PDOException $e) {
    fwrite(STDERR, "❌ Table `personnel` is missing — run migration 002 (or api/reset-db.php) first.\n");
    exit(1);
}
echo "✅ Table `personnel` present\n\n";

// --- Read & split the seed file into statements ---
$sql = file_get_contents($seedFile);
if ($sql === false || trim($sql) === '') {
    fwrite(STDERR, "❌ Seed file is empty.\n");
    exit(1);
}

$statements = splitSqlStatements($sql);
if (empty($statements)) {
    fwrite(STDERR, "❌ No executable statements found in seed file.\n");
    exit(1);
}

// --- Execute each statement in a single transaction ---
$pdo->beginTransaction();
$i = 0;
try {
    foreach ($statements as $i => $stmt) {
        $pdo->exec($stmt);
    }
    $pdo->commit();
} catch (PDOException $e) {
    $pdo->rollBack();
    fwrite(STDERR, "❌ Seed failed (statement " . ($i + 1) . "): " . $e->getMessage() . "\n");
    exit(1);
}

// --- Report row counts ---
$total   = (int) $pdo->query("SELECT COUNT(*) FROM `personnel`")->fetchColumn();
$seeded  = (int) $pdo->query("SELECT COUNT(*) FROM `personnel` WHERE `im` LIKE '1000%'")->fetchColumn();
$withPin = (int) $pdo->query("SELECT COUNT(*) FROM `personnel` WHERE `code_secret_hash` IS NOT NULL")->fetchColumn();

echo "✅ Seed applied successfully\n";
echo "   • total personnel rows:   $total\n";
echo "   • seeded demo rows (1000*): $seeded\n";
echo "   • with code_secret_hash:  $withPin\n";
echo "\n=========================\n";
echo "✅ Done\n";

/**
 * Split a SQL script into individual executable statements.
 *
 * Honors the `DELIMITER` directive (case-insensitive) so that triggers and
 * stored procedures — whose bodies contain semicolons — are returned as a
 * single statement. Lines starting with `--` are treated as comments and
 * stripped. (Same logic as reset-db.php.)
 *
 * @return string[]
 */
function splitSqlStatements(string $sql): array
{
    $sql = str_replace(["\r\n", "\r"], "\n", $sql);

    $delimiter = ';';
    $statements = [];
    $buffer = '';

    $lines = explode("\n", $sql);
    foreach ($lines as $line) {
        $trimmed = ltrim($line);

        if ($trimmed !== '' && str_starts_with($trimmed, '--')) {
            continue;
        }

        if (preg_match('/^DELIMITER\s+(\S+)\s*$/i', $trimmed, $m)) {
            if (trim($buffer) !== '') {
                $statements[] = trim($buffer);
                $buffer = '';
            }
            $delimiter = $m[1];
            continue;
        }

        $buffer .= ($buffer === '' ? '' : "\n") . $line;

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
