<?php

/**
 * Database connectivity check — GET or POST /api/db-check.php
 *
 * Loads the same .env + config as the real API, opens a PDO connection and
 * runs a query. Returns JSON. Diagnostic endpoint — delete after testing
 * (or protect it) since it reveals server/database names.
 */

require __DIR__ . '/../config/bootstrap.php';

use App\Middleware\CorsMiddleware;

if (PHP_SAPI !== 'cli') {
    CorsMiddleware::handle();
}
header('Content-Type: application/json; charset=utf-8');

$config = require __DIR__ . '/../config/database.php';

try {
    $dsn = sprintf(
        'mysql:host=%s;port=%s;dbname=%s;charset=%s',
        $config['host'],
        $config['port'],
        $config['dbname'],
        $config['charset']
    );

    $start = microtime(true);
    $pdo = new PDO($dsn, $config['username'], $config['password'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    ]);
    $connectMs = round((microtime(true) - $start) * 1000);

    $version = $pdo->query('SELECT VERSION()')->fetchColumn();
    $tables  = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);

    $users = null;
    if (in_array('users', $tables, true)) {
        $users = (int) $pdo->query('SELECT COUNT(*) FROM users')->fetchColumn();
    }

    echo json_encode([
        'success' => true,
        'message' => 'Database connection OK',
        'data'    => [
            'method'        => $_SERVER['REQUEST_METHOD'] ?? 'CLI',
            'host'          => $config['host'] . ':' . $config['port'],
            'database'      => $config['dbname'],
            'mysql_version' => $version,
            'tables_count'  => count($tables),
            'users_count'   => $users,
            'connect_ms'    => $connectMs,
            'php_version'   => PHP_VERSION,
        ],
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed',
        'error'   => $e->getMessage(),
        'config'  => [
            'host'     => $config['host'] . ':' . $config['port'],
            'database' => $config['dbname'],
            'username' => $config['username'],
        ],
    ]);
}
