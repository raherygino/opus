<?php

/**
 * OPUS API — Front Controller
 *
 * Pure PHP REST API (no framework)
 */

// Bootstrap
require __DIR__ . '/../config/bootstrap.php';

use App\Middleware\CorsMiddleware;
use App\Router;
use App\Controllers\AuthController;
use App\Controllers\MouvementController;
use App\Controllers\MouvementAttachmentController;
use App\Controllers\PersonnelController;
use App\Controllers\PersonnelAttachmentController;
use App\Controllers\RoleController;
use App\Controllers\UserController;
use App\Controllers\NotificationController;
use App\Controllers\AuditLogController;

// --- CORS ---
CorsMiddleware::handle();

// --- Router ---
$router = new Router();

// ========================
// Auth Routes
// ========================
$router->post('/api/auth/login',    [AuthController::class, 'login']);
$router->post('/api/auth/refresh',  [AuthController::class, 'refresh']);
$router->get('/api/auth/me',        [AuthController::class, 'me']);
$router->put('/api/auth/password',  [AuthController::class, 'password']);
$router->post('/api/auth/photo',    [AuthController::class, 'uploadPhoto']);

// ========================
// Personnel Routes
// ========================
$router->get('/api/personnel',              [PersonnelController::class, 'index']);
$router->get('/api/personnel/available',    [PersonnelController::class, 'available']);
$router->get('/api/personnel/{id}',         [PersonnelController::class, 'show']);
$router->post('/api/personnel',             [PersonnelController::class, 'store']);
$router->put('/api/personnel/{id}',         [PersonnelController::class, 'update']);
$router->delete('/api/personnel/{id}',      [PersonnelController::class, 'destroy']);

// ========================
// Personnel Photo Routes
// ========================
$router->post('/api/personnel/{id}/photo',     [PersonnelController::class, 'uploadPhoto']);
$router->get('/api/personnel/{id}/photo',      [PersonnelController::class, 'servePhoto']);

// ========================
// Personnel Attachment Routes
// ========================
$router->get('/api/personnel/{id}/attachments',                       [PersonnelAttachmentController::class, 'index']);
$router->post('/api/personnel/{id}/attachments',                      [PersonnelAttachmentController::class, 'store']);
$router->put('/api/personnel/{id}/attachments/{attachId}',            [PersonnelAttachmentController::class, 'update']);
$router->delete('/api/personnel/{id}/attachments/{attachId}',         [PersonnelAttachmentController::class, 'destroy']);
$router->get('/api/personnel/{id}/attachments/{attachId}/download',   [PersonnelAttachmentController::class, 'download']);

// ========================
// Mouvement Routes
// ========================
$router->get('/api/mouvements',           [MouvementController::class, 'index']);
$router->get('/api/mouvements/{id}',      [MouvementController::class, 'show']);
$router->post('/api/mouvements',          [MouvementController::class, 'store']);
$router->put('/api/mouvements/{id}',          [MouvementController::class, 'update']);
$router->put('/api/mouvements/{id}/retour',   [MouvementController::class, 'retour']);
$router->delete('/api/mouvements/{id}',       [MouvementController::class, 'destroy']);

// ========================
// Mouvement Attachment Routes
// ========================
$router->get('/api/mouvements/{id}/attachments',                       [MouvementAttachmentController::class, 'index']);
$router->post('/api/mouvements/{id}/attachments',                      [MouvementAttachmentController::class, 'store']);
$router->put('/api/mouvements/{id}/attachments/{attachId}',            [MouvementAttachmentController::class, 'update']);
$router->delete('/api/mouvements/{id}/attachments/{attachId}',         [MouvementAttachmentController::class, 'destroy']);
$router->get('/api/mouvements/{id}/attachments/{attachId}/download',   [MouvementAttachmentController::class, 'download']);

// ========================
// Role Routes (RBAC - SUPER_ADMIN only)
// ========================
$router->get('/api/roles',                   [RoleController::class, 'index']);
$router->get('/api/roles/{id}',              [RoleController::class, 'show']);
$router->post('/api/roles',                  [RoleController::class, 'store']);
$router->put('/api/roles/{id}',              [RoleController::class, 'update']);
$router->delete('/api/roles/{id}',           [RoleController::class, 'destroy']);
$router->get('/api/roles/{id}/permissions',  [RoleController::class, 'permissions']);
$router->put('/api/roles/{id}/permissions',  [RoleController::class, 'updatePermissions']);

// ========================
// User Routes
// ========================
$router->get('/api/users',          [UserController::class, 'index']);
$router->get('/api/users/{id}',     [UserController::class, 'show']);
$router->post('/api/users',         [UserController::class, 'store']);
$router->put('/api/users/{id}',     [UserController::class, 'update']);
$router->delete('/api/users/{id}',  [UserController::class, 'destroy']);

// ========================
// Notification Routes
// ========================
$router->get('/api/notifications',                [NotificationController::class, 'index']);
$router->get('/api/notifications/unread-count',    [NotificationController::class, 'unreadCount']);
$router->get('/api/notifications/{id}',            [NotificationController::class, 'show']);
$router->post('/api/notifications',                [NotificationController::class, 'store']);
$router->put('/api/notifications/{id}/read',       [NotificationController::class, 'markAsRead']);
$router->put('/api/notifications/read-all',        [NotificationController::class, 'markAllAsRead']);
$router->delete('/api/notifications/{id}',         [NotificationController::class, 'destroy']);

// ========================
// Audit Log Routes (SUPER_ADMIN only)
// ========================
$router->get('/api/audit-logs',       [AuditLogController::class, 'index']);
$router->get('/api/audit-logs/{id}',  [AuditLogController::class, 'show']);

// ========================
// Health Check
// ========================
$router->get('/api/health', function () {
    echo json_encode([
        'success' => true,
        'message' => 'OPUS API is running',
        'version' => '1.0.0',
        'time'    => date('c'),
    ]);
});

// --- Dispatch ---
$method = $_SERVER['REQUEST_METHOD'];
$uri    = $_SERVER['REQUEST_URI'];

$router->dispatch($method, $uri);
