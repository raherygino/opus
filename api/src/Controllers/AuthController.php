<?php

namespace App\Controllers;

use App\Helpers\JWT;
use App\Helpers\Response;
use App\Models\User;
use App\Models\Personnel;
use App\Models\AuditLog;
use App\Validators\AuthValidator;

class AuthController
{
    /**
     * POST /api/auth/login
     * Body: { username, password }
     */
    public function login(array $params): void
    {
        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // Validate
        $errors = AuthValidator::validateLogin($data);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Find user
        $user = User::getByUsername($data['username']);
        if (!$user) {
            Response::unauthorized('Invalid username or password');
        }

        // Check active
        if (!$user['is_active']) {
            Response::forbidden('Account is deactivated');
        }

        // Verify password
        if (!password_verify($data['password'], $user['password_hash'])) {
            Response::unauthorized('Invalid username or password');
        }

        // Update last login
        User::updateLastLogin($user['id']);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $user['id'],
            'action' => 'login',
            'module' => 'auth',
            'description' => "Connexion réussie pour l'utilisateur '{$user['username']}'",
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // Generate tokens
        $config = require __DIR__ . '/../../config/app.php';
        $accessToken = JWT::encode([
            'sub'           => $user['id'],
            'username'      => $user['username'],
            'role_id'       => $user['role_id'],
            'role_code'     => $user['role_code'],
            'personnel_id'  => $user['personnel_id'],
        ]);
        $refreshToken = JWT::encode([
            'sub'  => $user['id'],
            'type' => 'refresh',
        ]);

        // Load role permissions
        $permissions = \App\Models\RolePermission::getByRoleId($user['role_id']);
        $user['permissions'] = $permissions;

        // Return user info (without password)
        unset($user['password_hash']);

        Response::success([
            'access_token'  => $accessToken,
            'refresh_token' => $refreshToken,
            'user'          => $user,
        ], 'Login successful');
    }

    /**
     * POST /api/auth/refresh
     * Body: { refresh_token }
     */
    public function refresh(array $params): void
    {
        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['refresh_token'])) {
            Response::error('Refresh token is required', 422);
        }

        $payload = JWT::decode($data['refresh_token']);
        if (!$payload || !isset($payload['sub'])) {
            Response::unauthorized('Invalid or expired refresh token');
        }

        $user = User::getById($payload['sub']);
        if (!$user || !$user['is_active']) {
            Response::unauthorized('User not found or deactivated');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $accessToken = JWT::encode([
            'sub'           => $user['id'],
            'username'      => $user['username'],
            'role_id'       => $user['role_id'],
            'role_code'     => $user['role_code'],
            'personnel_id'  => $user['personnel_id'],
        ]);

        Response::success([
            'access_token' => $accessToken,
        ], 'Token refreshed');
    }

    /**
     * GET /api/auth/me
     * Requires Authorization: Bearer <token>
     */
    public function me(array $params): void
    {
        $authUser = self::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $user = User::getById($authUser['sub']);
        if (!$user) {
            Response::notFound('User not found');
        }

        $user['permissions'] = \App\Models\RolePermission::getByRoleId($user['role_id']);
        unset($user['password_hash']);
        Response::success($user);
    }

    /**
     * PUT /api/auth/password
     * Body: { current_password, new_password }
     */
    public function password(array $params): void
    {
        $authUser = self::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = AuthValidator::validatePasswordChange($data);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $user = User::getById($authUser['sub']);
        if (!$user) {
            Response::notFound('User not found');
        }

        if (!password_verify($data['current_password'], $user['password_hash'])) {
            Response::error('Current password is incorrect', 422, ['current_password' => 'Mot de passe actuel incorrect']);
        }

        User::update($user['id'], ['password' => $data['new_password']]);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $user['id'],
            'action' => 'password_change',
            'module' => 'auth',
            'description' => "Changement de mot de passe pour l'utilisateur '{$user['username']}'",
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Password changed successfully');
    }

    /**
     * POST /api/auth/photo
     * Multipart: photo (file)
     * Uploads profile photo for the authenticated user's personnel record
     */
    public function uploadPhoto(array $params): void
    {
        $authUser = self::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $user = User::getById($authUser['sub']);
        if (!$user) {
            Response::notFound('User not found');
        }

        $personnelId = (int) $user['personnel_id'];
        $person = Personnel::getById($personnelId);
        if (!$person) {
            Response::notFound('Personnel record not found');
        }

        if (!isset($_FILES['photo']) || $_FILES['photo']['error'] !== UPLOAD_ERR_OK) {
            Response::error('Photo file is required', 422, ['photo' => 'Le fichier photo est requis']);
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/personnel/photos';
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
        }

        $uploadedFile = $_FILES['photo'];
        $extension = strtolower(pathinfo($uploadedFile['name'], PATHINFO_EXTENSION));
        $allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

        if (!in_array($extension, $allowedExtensions)) {
            Response::error('Invalid file type', 422, ['photo' => 'Seuls les formats JPG, PNG, GIF et WebP sont autorisés']);
        }

        // Delete old photo if exists
        if (!empty($person['photo'])) {
            $oldPath = $uploadDir . '/' . $person['photo'];
            if (file_exists($oldPath)) {
                unlink($oldPath);
            }
        }
        if (!empty($person['thumbnail'])) {
            $oldThumbPath = $uploadDir . '/' . $person['thumbnail'];
            if (file_exists($oldThumbPath)) {
                unlink($oldThumbPath);
            }
        }

        $storedName = 'photo_' . $personnelId . '_' . uniqid() . '.' . $extension;
        $destPath = $uploadDir . '/' . $storedName;

        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save photo', 500);
        }

        $updateData = ['photo' => $storedName];

        // Handle optional thumbnail upload
        if (isset($_FILES['thumbnail']) && $_FILES['thumbnail']['error'] === UPLOAD_ERR_OK) {
            $thumbExt = strtolower(pathinfo($_FILES['thumbnail']['name'], PATHINFO_EXTENSION));
            if (in_array($thumbExt, $allowedExtensions)) {
                $thumbName = 'thumb_' . $personnelId . '_' . uniqid() . '.' . $thumbExt;
                $thumbDest = $uploadDir . '/' . $thumbName;
                if (move_uploaded_file($_FILES['thumbnail']['tmp_name'], $thumbDest)) {
                    $updateData['thumbnail'] = $thumbName;
                }
            }
        }

        Personnel::update($personnelId, $updateData);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'],
            'action' => 'photo_upload',
            'module' => 'auth',
            'entity_id' => $personnelId,
            'description' => "Photo de profil mise à jour pour le personnel ID {$personnelId}",
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // Return updated user info
        $updatedUser = User::getById($user['id']);
        unset($updatedUser['password_hash']);
        Response::success($updatedUser, 'Photo uploaded successfully');
    }

    /**
     * Get authenticated user from JWT in Authorization header
     */
    public static function getAuthenticatedUser(): ?array
    {
        $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
        if (!preg_match('/^Bearer\s+(.+)$/', $header, $matches)) {
            return null;
        }

        $payload = JWT::decode($matches[1]);
        return $payload;
    }
}
