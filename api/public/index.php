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
use App\Controllers\ComportementController;
use App\Controllers\CorrespondanceController;
use App\Controllers\CorrespondanceAttachmentController;
use App\Controllers\DeclarationPerteController;
use App\Controllers\DeclarationPerteAttachmentController;
use App\Controllers\PassationController;
use App\Controllers\PassationAttachmentController;
use App\Controllers\ArmementController;
use App\Controllers\ArmementAttachmentController;
use App\Controllers\MouvementAttachmentController;
use App\Controllers\PersonnelController;
use App\Controllers\PersonnelAttachmentController;
use App\Controllers\RoleController;
use App\Controllers\UserController;
use App\Controllers\NotificationController;
use App\Controllers\DeviceTokenController;
use App\Controllers\AuditLogController;
use App\Controllers\QrAuthController;

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
$router->post('/api/auth/verify',   [AuthController::class, 'verify']);
$router->post('/api/auth/photo',    [AuthController::class, 'uploadPhoto']);
$router->delete('/api/auth/photo',    [AuthController::class, 'deletePhoto']);

// ========================
// QR Auth Routes (scan-to-log-in between desktop & phone)
// ========================
$router->post('/api/qr-auth/request',           [QrAuthController::class, 'request']);
$router->get('/api/qr-auth/{code}',              [QrAuthController::class, 'status']);
$router->post('/api/qr-auth/{code}/scan',        [QrAuthController::class, 'scan']);
$router->post('/api/qr-auth/{code}/approve',     [QrAuthController::class, 'approve']);
$router->post('/api/qr-auth/{code}/reject',      [QrAuthController::class, 'reject']);
$router->post('/api/qr-auth/{code}/cancel',      [QrAuthController::class, 'cancel']);

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
$router->delete('/api/personnel/{id}/photo',    [PersonnelController::class, 'deletePhoto']);
$router->get('/api/personnel/{id}/photo',      [PersonnelController::class, 'servePhoto']);
$router->get('/api/personnel/{id}/thumbnail',  [PersonnelController::class, 'serveThumbnail']);

// ========================
// Personnel Signature Routes
// ========================
$router->post('/api/personnel/{id}/signature',     [PersonnelController::class, 'uploadSignature']);
$router->post('/api/personnel/{id}/signature/svg',  [PersonnelController::class, 'saveSignatureSvg']);
$router->get('/api/personnel/{id}/signature',      [PersonnelController::class, 'serveSignature']);

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
// Comportement Routes
// ========================
$router->get('/api/comportements',               [ComportementController::class, 'index']);
$router->get('/api/comportements/{id}',          [ComportementController::class, 'show']);
$router->post('/api/comportements',              [ComportementController::class, 'store']);
$router->put('/api/comportements/{id}',          [ComportementController::class, 'update']);
$router->put('/api/comportements/{id}/confirm',  [ComportementController::class, 'confirm']);
$router->put('/api/comportements/{id}/reject',   [ComportementController::class, 'reject']);
$router->delete('/api/comportements/{id}',       [ComportementController::class, 'destroy']);

// ========================
// Correspondance Routes
// ========================
$router->get('/api/correspondances',           [CorrespondanceController::class, 'index']);
$router->get('/api/correspondances/{id}',      [CorrespondanceController::class, 'show']);
$router->post('/api/correspondances',          [CorrespondanceController::class, 'store']);
$router->put('/api/correspondances/{id}',      [CorrespondanceController::class, 'update']);
$router->delete('/api/correspondances/{id}',   [CorrespondanceController::class, 'destroy']);

// ========================
// Correspondance Attachment Routes
// ========================
$router->get('/api/correspondances/{id}/attachments',                       [CorrespondanceAttachmentController::class, 'index']);
$router->post('/api/correspondances/{id}/attachments',                      [CorrespondanceAttachmentController::class, 'store']);
$router->put('/api/correspondances/{id}/attachments/{attachId}',            [CorrespondanceAttachmentController::class, 'update']);
$router->delete('/api/correspondances/{id}/attachments/{attachId}',         [CorrespondanceAttachmentController::class, 'destroy']);
$router->get('/api/correspondances/{id}/attachments/{attachId}/download',   [CorrespondanceAttachmentController::class, 'download']);

// ========================
// Déclaration de perte Routes
// ========================
$router->get('/api/declarations-perte',           [DeclarationPerteController::class, 'index']);
$router->get('/api/declarations-perte/{id}',      [DeclarationPerteController::class, 'show']);
$router->post('/api/declarations-perte',          [DeclarationPerteController::class, 'store']);
$router->put('/api/declarations-perte/{id}',      [DeclarationPerteController::class, 'update']);
$router->delete('/api/declarations-perte/{id}',   [DeclarationPerteController::class, 'destroy']);

// ========================
// Déclaration de perte Attachment Routes
// ========================
$router->get('/api/declarations-perte/{id}/attachments',                       [DeclarationPerteAttachmentController::class, 'index']);
$router->post('/api/declarations-perte/{id}/attachments',                      [DeclarationPerteAttachmentController::class, 'store']);
$router->put('/api/declarations-perte/{id}/attachments/{attachId}',            [DeclarationPerteAttachmentController::class, 'update']);
$router->delete('/api/declarations-perte/{id}/attachments/{attachId}',         [DeclarationPerteAttachmentController::class, 'destroy']);
$router->get('/api/declarations-perte/{id}/attachments/{attachId}/download',   [DeclarationPerteAttachmentController::class, 'download']);

// ========================
// Passation Routes (Sédentaire > Poste)
// ========================
$router->get('/api/passations',           [PassationController::class, 'index']);
$router->get('/api/passations/{id}',      [PassationController::class, 'show']);
$router->post('/api/passations',          [PassationController::class, 'store']);
$router->put('/api/passations/{id}',      [PassationController::class, 'update']);
$router->delete('/api/passations/{id}',   [PassationController::class, 'destroy']);

// ========================
// Passation Attachment Routes
// ========================
$router->get('/api/passations/{id}/attachments',                       [PassationAttachmentController::class, 'index']);
$router->post('/api/passations/{id}/attachments',                      [PassationAttachmentController::class, 'store']);
$router->put('/api/passations/{id}/attachments/{attachId}',            [PassationAttachmentController::class, 'update']);
$router->delete('/api/passations/{id}/attachments/{attachId}',         [PassationAttachmentController::class, 'destroy']);
$router->get('/api/passations/{id}/attachments/{attachId}/download',   [PassationAttachmentController::class, 'download']);

// ========================
// Armement Routes (Sédentaire > Poste)
// ========================
$router->get('/api/armements',                            [ArmementController::class, 'index']);
$router->get('/api/armements/{id}',                       [ArmementController::class, 'show']);
$router->post('/api/armements',                           [ArmementController::class, 'store']);
$router->put('/api/armements/{id}',                       [ArmementController::class, 'update']);
$router->post('/api/armements/{id}/reintegration',        [ArmementController::class, 'reintegrate']);
$router->delete('/api/armements/{id}',                    [ArmementController::class, 'destroy']);

// ========================
// Armement Attachment Routes
// ========================
$router->get('/api/armements/{id}/attachments',                       [ArmementAttachmentController::class, 'index']);
$router->post('/api/armements/{id}/attachments',                      [ArmementAttachmentController::class, 'store']);
$router->put('/api/armements/{id}/attachments/{attachId}',            [ArmementAttachmentController::class, 'update']);
$router->delete('/api/armements/{id}/attachments/{attachId}',         [ArmementAttachmentController::class, 'destroy']);
$router->get('/api/armements/{id}/attachments/{attachId}/download',   [ArmementAttachmentController::class, 'download']);

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
// Device Token Routes (FCM Push Notifications)
// ========================
$router->get('/api/devices',               [DeviceTokenController::class, 'index']);
$router->post('/api/devices/register',     [DeviceTokenController::class, 'register']);
$router->post('/api/devices/unregister',   [DeviceTokenController::class, 'unregister']);
$router->delete('/api/devices',            [DeviceTokenController::class, 'unregisterAll']);
$router->post('/api/devices/test-push',    [DeviceTokenController::class, 'testPush']);

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
